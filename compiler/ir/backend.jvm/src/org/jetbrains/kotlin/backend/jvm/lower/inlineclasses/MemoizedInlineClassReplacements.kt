/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.inlineclasses

import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParameters
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createDispatchReceiverParameter
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.InlineClassAbi.mangledNameFor
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildFunWithDescriptorForInlining
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionBase
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.InlineClassDescriptorResolver
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

/**
 * Keeps track of replacement functions and inline class box/unbox functions.
 */
class MemoizedInlineClassReplacements {
    private val storageManager = LockBasedStorageManager("inline-class-replacements")

    /**
     * Get a replacement for a function or a constructor.
     */
    val getReplacementFunction: (IrFunction) -> IrSimpleFunction? =
        storageManager.createMemoizedFunctionWithNullableValues {
            when {
                // Don't mangle anonymous or synthetic functions
                it.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA ||
                        it.origin == IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR ||
                        it.origin == JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_REPLACEMENT ||
                        it.origin.isSynthetic ||
                        it.isInlineClassFieldGetter -> null

                // Mangle all functions in the body of an inline class
                it.parent.safeAs<IrClass>()?.isInline == true ->
                    createStaticReplacement(it)

                // Otherwise, mangle functions with mangled parameters, while ignoring constructors
                it is IrSimpleFunction && it.hasMangledParameters ->
                    if (it.dispatchReceiverParameter != null) createMethodReplacement(it) else createStaticReplacement(it)

                else ->
                    null
            }
        }

    /**
     * Get the box function for an inline class. Concretely, this is a synthetic
     * static function named "box-impl" which takes an unboxed value and returns
     * a boxed value.
     */
    val getBoxFunction: (IrClass) -> IrSimpleFunction =
        storageManager.createMemoizedFunction { irClass ->
            require(irClass.isInline)
            buildFun {
                name = Name.identifier(KotlinTypeMapper.BOX_JVM_METHOD_NAME)
                origin = JvmLoweredDeclarationOrigin.SYNTHETIC_INLINE_CLASS_MEMBER
                returnType = irClass.defaultType
            }.apply {
                parent = irClass
                copyTypeParametersFrom(irClass)
                addValueParameter {
                    name = InlineClassDescriptorResolver.BOXING_VALUE_PARAMETER_NAME
                    type = InlineClassAbi.getUnderlyingType(irClass)
                }
            }
        }

    /**
     * Get the unbox function for an inline class. Concretely, this is a synthetic
     * member function named "unbox-impl" which returns an unboxed result.
     */
    val getUnboxFunction: (IrClass) -> IrSimpleFunction =
        storageManager.createMemoizedFunction { irClass ->
            require(irClass.isInline)
            buildFun {
                name = Name.identifier(KotlinTypeMapper.UNBOX_JVM_METHOD_NAME)
                origin = JvmLoweredDeclarationOrigin.SYNTHETIC_INLINE_CLASS_MEMBER
                returnType = InlineClassAbi.getUnderlyingType(irClass)
            }.apply {
                parent = irClass
                createDispatchReceiverParameter()
            }
        }

    private val specializedEqualsCache = storageManager.createCacheWithNotNullValues<IrClass, IrSimpleFunction>()
    fun getSpecializedEqualsMethod(irClass: IrClass, irBuiltIns: IrBuiltIns): IrSimpleFunction {
        require(irClass.isInline)
        return specializedEqualsCache.computeIfAbsent(irClass) {
            buildFun {
                name = InlineClassDescriptorResolver.SPECIALIZED_EQUALS_NAME
                // TODO: Revisit this once we allow user defined equals methods in inline classes.
                origin = JvmLoweredDeclarationOrigin.INLINE_CLASS_GENERATED_IMPL_METHOD
                returnType = irBuiltIns.booleanType
            }.apply {
                parent = irClass
                // We ignore type arguments here, since there is no good way to go from type arguments to types in the IR anyway.
                val typeArgument =
                    IrSimpleTypeImpl(null, irClass.symbol, false, List(irClass.typeParameters.size) { IrStarProjectionImpl }, listOf())
                addValueParameter {
                    name = InlineClassDescriptorResolver.SPECIALIZED_EQUALS_FIRST_PARAMETER_NAME
                    type = typeArgument
                }
                addValueParameter {
                    name = InlineClassDescriptorResolver.SPECIALIZED_EQUALS_SECOND_PARAMETER_NAME
                    type = typeArgument
                }
            }
        }
    }

    private fun createMethodReplacement(function: IrFunction): IrSimpleFunction =
        buildReplacement(function, function.origin) {
            require(function.dispatchReceiverParameter != null && function is IrSimpleFunction)
            val newValueParameters = ArrayList<IrValueParameter>()
            for ((index, parameter) in function.explicitParameters.withIndex()) {
                val name = if (parameter == function.extensionReceiverParameter) Name.identifier("\$receiver") else parameter.name
                val newParameter: IrValueParameter
                if (parameter == function.dispatchReceiverParameter) {
                    newParameter = parameter.copyTo(this, index = -1, name = name, defaultValue = null)
                    dispatchReceiverParameter = newParameter
                } else {
                    newParameter = parameter.copyTo(this, index = index - 1, name = name, defaultValue = null)
                    newValueParameters += newParameter
                }
                // Assuming that constructors and non-override functions are always replaced with the unboxed
                // equivalent, deep-copying the value here is unnecessary. See `JvmInlineClassLowering`.
                newParameter.defaultValue = parameter.defaultValue?.patchDeclarationParents(this)
            }
            valueParameters = newValueParameters
        }

    private fun createStaticReplacement(function: IrFunction): IrSimpleFunction =
        buildReplacement(function, JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_REPLACEMENT) {
            val newValueParameters = ArrayList<IrValueParameter>()
            for ((index, parameter) in function.explicitParameters.withIndex()) {
                newValueParameters += when (parameter) {
                    // FAKE_OVERRIDEs have broken dispatch receivers
                    function.dispatchReceiverParameter ->
                        function.parentAsClass.thisReceiver!!.copyTo(
                            this, index = index, name = Name.identifier("arg$index"),
                            type = function.parentAsClass.defaultType, origin = IrDeclarationOrigin.MOVED_DISPATCH_RECEIVER
                        )
                    function.extensionReceiverParameter ->
                        parameter.copyTo(
                            this, index = index, name = Name.identifier("\$this\$${function.name}"),
                            origin = IrDeclarationOrigin.MOVED_EXTENSION_RECEIVER
                        )
                    else ->
                        parameter.copyTo(this, index = index, defaultValue = null).also {
                            // See comment next to a similar line above.
                            it.defaultValue = parameter.defaultValue?.patchDeclarationParents(this)
                        }
                }
            }
            valueParameters = newValueParameters
        }

    private fun buildReplacement(function: IrFunction, replacementOrigin: IrDeclarationOrigin, body: IrFunctionImpl.() -> Unit) =
        buildFunWithDescriptorForInlining(function.descriptor) {
            updateFrom(function)
            origin = if (function.origin == IrDeclarationOrigin.GENERATED_INLINE_CLASS_MEMBER) {
                JvmLoweredDeclarationOrigin.INLINE_CLASS_GENERATED_IMPL_METHOD
            } else {
                replacementOrigin
            }
            name = mangledNameFor(function)
            returnType = function.returnType
        }.apply {
            parent = function.parent
            annotations += function.annotations
            copyTypeParameters(function.allTypeParameters)
            metadata = function.metadata
            function.safeAs<IrFunctionBase<*>>()?.metadata = null

            if (function is IrSimpleFunction) {
                correspondingPropertySymbol = function.correspondingPropertySymbol
                overriddenSymbols = function.overriddenSymbols.map {
                    getReplacementFunction(it.owner)?.symbol ?: it
                }
            }

            body()
        }
}
