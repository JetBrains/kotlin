/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.inlineclasses

import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParameters
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createDispatchReceiverParameter
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.classFileContainsMethod
import org.jetbrains.kotlin.backend.jvm.ir.isStaticInlineClassReplacement
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.InlineClassAbi.mangledNameFor
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.InlineClassDescriptorResolver
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

/**
 * Keeps track of replacement functions and inline class box/unbox functions.
 */
class MemoizedInlineClassReplacements(
    private val mangleReturnTypes: Boolean,
    private val irFactory: IrFactory,
    private val context: JvmBackendContext
) {
    private val storageManager = LockBasedStorageManager("inline-class-replacements")
    private val propertyMap = mutableMapOf<IrPropertySymbol, IrProperty>()

    internal val originalFunctionForStaticReplacement: MutableMap<IrFunction, IrFunction> = HashMap()

    /**
     * Get a replacement for a function or a constructor.
     */
    val getReplacementFunction: (IrFunction) -> IrSimpleFunction? =
        storageManager.createMemoizedFunctionWithNullableValues {
            when {
                // Don't mangle anonymous or synthetic functions
                it.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA ||
                        (it.origin == IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR && it.visibility == DescriptorVisibilities.LOCAL) ||
                        it.isStaticInlineClassReplacement ||
                        it.origin.isSynthetic ->
                    null

                it.isInlineClassFieldGetter ->
                    if (it.hasMangledReturnType)
                        createMethodReplacement(it)
                    else
                        null

                // Mangle all functions in the body of an inline class
                it.parent.safeAs<IrClass>()?.isInline == true ->
                    when {
                        it.isRemoveAtSpecialBuiltinStub() ->
                            null
                        it.origin == IrDeclarationOrigin.IR_BUILTINS_STUB ->
                            createMethodReplacement(it)
                        else ->
                            createStaticReplacement(it)
                    }

                // Otherwise, mangle functions with mangled parameters, ignoring constructors
                it is IrSimpleFunction && (it.hasMangledParameters || mangleReturnTypes && it.hasMangledReturnType) ->
                    if (it.dispatchReceiverParameter != null)
                        createMethodReplacement(it)
                    else
                        createStaticReplacement(it)

                else ->
                    null
            }
        }

    private fun IrFunction.isRemoveAtSpecialBuiltinStub() =
        origin == IrDeclarationOrigin.IR_BUILTINS_STUB &&
                name.asString() == "remove" &&
                valueParameters.size == 1 &&
                valueParameters[0].type.isInt()

    /**
     * Get the box function for an inline class. Concretely, this is a synthetic
     * static function named "box-impl" which takes an unboxed value and returns
     * a boxed value.
     */
    val getBoxFunction: (IrClass) -> IrSimpleFunction =
        storageManager.createMemoizedFunction { irClass ->
            require(irClass.isInline)
            irFactory.buildFun {
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
            irFactory.buildFun {
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
            irFactory.buildFun {
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
            dispatchReceiverParameter = function.dispatchReceiverParameter?.copyTo(this, index = -1)
            extensionReceiverParameter = function.extensionReceiverParameter?.copyTo(this, index = -1, name = Name.identifier("\$receiver"))
            valueParameters = function.valueParameters.mapIndexed { index, parameter ->
                parameter.copyTo(this, index = index, defaultValue = null).also {
                    // Assuming that constructors and non-override functions are always replaced with the unboxed
                    // equivalent, deep-copying the value here is unnecessary. See `JvmInlineClassLowering`.
                    it.defaultValue = parameter.defaultValue?.patchDeclarationParents(this)
                }
            }
        }

    private fun createStaticReplacement(function: IrFunction): IrSimpleFunction =
        buildReplacement(function, JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_REPLACEMENT, noFakeOverride = true) {
            originalFunctionForStaticReplacement[this] = function

            val newValueParameters = mutableListOf<IrValueParameter>()
            if (function.dispatchReceiverParameter != null) {
                // FAKE_OVERRIDEs have broken dispatch receivers
                newValueParameters += function.parentAsClass.thisReceiver!!.copyTo(
                    this, index = newValueParameters.size, name = Name.identifier("arg${newValueParameters.size}"),
                    type = function.parentAsClass.defaultType, origin = IrDeclarationOrigin.MOVED_DISPATCH_RECEIVER
                )
            }
            function.extensionReceiverParameter?.let {
                newValueParameters += it.copyTo(
                    this, index = newValueParameters.size, name = Name.identifier("\$this\$${function.name}"),
                    origin = IrDeclarationOrigin.MOVED_EXTENSION_RECEIVER
                )
            }
            for (parameter in function.valueParameters) {
                newValueParameters += parameter.copyTo(this, index = newValueParameters.size, defaultValue = null).also {
                    // See comment next to a similar line above.
                    it.defaultValue = parameter.defaultValue?.patchDeclarationParents(this)
                }
            }
            valueParameters = newValueParameters
        }

    private fun buildReplacement(
        function: IrFunction,
        replacementOrigin: IrDeclarationOrigin,
        noFakeOverride: Boolean = false,
        body: IrFunction.() -> Unit
    ): IrSimpleFunction = irFactory.buildFun {
        updateFrom(function)
        if (function is IrConstructor) {
            // The [updateFrom] call will set the modality to FINAL for constructors, while the JVM backend would use OPEN here.
            modality = Modality.OPEN
        }
        origin = when {
            function.origin == IrDeclarationOrigin.GENERATED_INLINE_CLASS_MEMBER ->
                JvmLoweredDeclarationOrigin.INLINE_CLASS_GENERATED_IMPL_METHOD
            function is IrConstructor && function.constructedClass.isInline ->
                JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_CONSTRUCTOR
            else ->
                replacementOrigin
        }
        if (noFakeOverride) {
            isFakeOverride = false
        }
        val useOldManglingScheme = context.state.useOldManglingSchemeForFunctionsWithInlineClassesInSignatures
        name = mangledNameFor(function, mangleReturnTypes, useOldManglingScheme)
        if (
            !useOldManglingScheme &&
            name.asString().contains("-") &&
            classFileContainsMethod(function, context, name.asString()) == false
        ) {
            name = mangledNameFor(function, mangleReturnTypes, true)
        }
        returnType = function.returnType
    }.apply {
        parent = function.parent
        annotations = function.annotations
        copyTypeParameters(function.allTypeParameters)
        if (function.metadata != null) {
            metadata = function.metadata
            function.metadata = null
        }
        copyAttributes(function as? IrAttributeContainer)

        if (function is IrSimpleFunction) {
            val propertySymbol = function.correspondingPropertySymbol
            if (propertySymbol != null) {
                val property = propertyMap.getOrPut(propertySymbol) {
                    irFactory.buildProperty {
                        name = propertySymbol.owner.name
                        updateFrom(propertySymbol.owner)
                    }.apply {
                        parent = propertySymbol.owner.parent
                        copyAttributes(propertySymbol.owner)
                        annotations = propertySymbol.owner.annotations
                    }
                }
                correspondingPropertySymbol = property.symbol
                when (function) {
                    propertySymbol.owner.getter -> property.getter = this
                    propertySymbol.owner.setter -> property.setter = this
                    else -> error("Orphaned property getter/setter: ${function.render()}")
                }
            }

            overriddenSymbols = function.overriddenSymbols.map {
                getReplacementFunction(it.owner)?.symbol ?: it
            }
        }

        body()
    }
}
