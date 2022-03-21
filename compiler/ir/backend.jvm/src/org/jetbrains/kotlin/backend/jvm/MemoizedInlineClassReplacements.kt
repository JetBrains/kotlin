/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParameters
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createDispatchReceiverParameter
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.InlineClassDescriptorResolver
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.concurrent.ConcurrentHashMap

/**
 * Keeps track of replacement functions and inline class box/unbox functions.
 */
class MemoizedInlineClassReplacements(
    private val mangleReturnTypes: Boolean,
    private val irFactory: IrFactory,
    private val context: JvmBackendContext
) {
    private val storageManager = LockBasedStorageManager("inline-class-replacements")
    private val propertyMap = ConcurrentHashMap<IrPropertySymbol, IrProperty>()

    val originalFunctionForStaticReplacement: MutableMap<IrFunction, IrFunction> = ConcurrentHashMap()
    internal val originalFunctionForMethodReplacement: MutableMap<IrFunction, IrFunction> = ConcurrentHashMap()

    /**
     * Get a replacement for a function or a constructor.
     */
    val getReplacementFunction: (IrFunction) -> IrSimpleFunction? =
        storageManager.createMemoizedFunctionWithNullableValues {
            when {
                // Don't mangle anonymous or synthetic functions, except for generated SAM wrapper methods
                (it.isLocal && it is IrSimpleFunction && it.overriddenSymbols.isEmpty()) ||
                        (it.origin == IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR && it.visibility == DescriptorVisibilities.LOCAL) ||
                        it.isStaticInlineClassReplacement ||
                        it.origin.isSynthetic && it.origin != IrDeclarationOrigin.SYNTHETIC_GENERATED_SAM_IMPLEMENTATION ->
                    null

                it.isInlineClassFieldGetter ->
                    if (it.hasMangledReturnType)
                        createMethodReplacement(it)
                    else
                        null

                // Mangle all functions in the body of an inline class
                it.parent.safeAs<IrClass>()?.isSingleFieldValueClass == true ->
                    when {
                        it.isRemoveAtSpecialBuiltinStub() ->
                            null
                        it.isInlineClassMemberFakeOverriddenFromJvmDefaultInterfaceMethod() ||
                                it.origin == IrDeclarationOrigin.IR_BUILTINS_STUB ->
                            createMethodReplacement(it)
                        else ->
                            createStaticReplacement(it)
                    }

                // Otherwise, mangle functions with mangled parameters, ignoring constructors
                it is IrSimpleFunction && !it.isFromJava() && (it.hasMangledParameters || mangleReturnTypes && it.hasMangledReturnType) ->
                    createMethodReplacement(it)

                else ->
                    null
            }
        }

    private fun IrFunction.isRemoveAtSpecialBuiltinStub() =
        origin == IrDeclarationOrigin.IR_BUILTINS_STUB &&
                name.asString() == "remove" &&
                valueParameters.size == 1 &&
                valueParameters[0].type.isInt()

    private fun IrFunction.isInlineClassMemberFakeOverriddenFromJvmDefaultInterfaceMethod(): Boolean {
        if (this !is IrSimpleFunction) return false
        if (!this.isFakeOverride) return false
        val parentClass = parentClassOrNull ?: return false
        if (!parentClass.isSingleFieldValueClass) return false

        val overridden = resolveFakeOverride() ?: return false
        if (!overridden.parentAsClass.isJvmInterface) return false
        if (overridden.modality == Modality.ABSTRACT) return false

        // We have a non-abstract interface member.
        // It is a JVM default interface method if one of the following conditions are true:
        // - it is a Java method,
        // - it is a Kotlin function compiled to JVM default interface method.
        return overridden.isFromJava() || overridden.isCompiledToJvmDefault(context.state.jvmDefaultMode)
    }

    /**
     * Get the box function for an inline class. Concretely, this is a synthetic
     * static function named "box-impl" which takes an unboxed value and returns
     * a boxed value.
     */
    val getBoxFunction: (IrClass) -> IrSimpleFunction =
        storageManager.createMemoizedFunction { irClass ->
            require(irClass.isSingleFieldValueClass)
            irFactory.buildFun {
                name = Name.identifier(KotlinTypeMapper.BOX_JVM_METHOD_NAME)
                origin = JvmLoweredDeclarationOrigin.SYNTHETIC_INLINE_CLASS_MEMBER
                returnType = irClass.defaultType
            }.apply {
                parent = irClass
                copyTypeParametersFrom(irClass)
                addValueParameter {
                    name = InlineClassDescriptorResolver.BOXING_VALUE_PARAMETER_NAME
                    type = irClass.inlineClassRepresentation!!.underlyingType
                }
            }
        }

    /**
     * Get the unbox function for an inline class. Concretely, this is a synthetic
     * member function named "unbox-impl" which returns an unboxed result.
     */
    val getUnboxFunction: (IrClass) -> IrSimpleFunction =
        storageManager.createMemoizedFunction { irClass ->
            require(irClass.isSingleFieldValueClass)
            irFactory.buildFun {
                name = Name.identifier(KotlinTypeMapper.UNBOX_JVM_METHOD_NAME)
                origin = JvmLoweredDeclarationOrigin.SYNTHETIC_INLINE_CLASS_MEMBER
                returnType = irClass.inlineClassRepresentation!!.underlyingType
            }.apply {
                parent = irClass
                createDispatchReceiverParameter()
            }
        }

    private val specializedEqualsCache = storageManager.createCacheWithNotNullValues<IrClass, IrSimpleFunction>()
    fun getSpecializedEqualsMethod(irClass: IrClass, irBuiltIns: IrBuiltIns): IrSimpleFunction {
        require(irClass.isSingleFieldValueClass)
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
            originalFunctionForMethodReplacement[this] = function
            dispatchReceiverParameter = function.dispatchReceiverParameter?.copyTo(this, index = -1)
            extensionReceiverParameter = function.extensionReceiverParameter?.copyTo(
                // The function's name will be mangled, so preserve the old receiver name.
                this, index = -1, name = Name.identifier(function.extensionReceiverName(context.state))
            )
            contextReceiverParametersCount = function.contextReceiverParametersCount
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
            if (function.contextReceiverParametersCount != 0) {
                function.valueParameters.take(function.contextReceiverParametersCount).forEachIndexed { i, contextReceiver ->
                    newValueParameters += contextReceiver.copyTo(
                        this, index = newValueParameters.size, name = Name.identifier("contextReceiver$i"),
                        origin = IrDeclarationOrigin.MOVED_CONTEXT_RECEIVER
                    )
                }
            }
            function.extensionReceiverParameter?.let {
                newValueParameters += it.copyTo(
                    this, index = newValueParameters.size, name = Name.identifier(function.extensionReceiverName(context.state)),
                    origin = IrDeclarationOrigin.MOVED_EXTENSION_RECEIVER
                )
            }
            for (parameter in function.valueParameters.drop(function.contextReceiverParametersCount)) {
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
    ): IrSimpleFunction {
        val useOldManglingScheme = context.state.useOldManglingSchemeForFunctionsWithInlineClassesInSignatures
        val replacement = buildReplacementInner(function, replacementOrigin, noFakeOverride, useOldManglingScheme, body)
        // When using the new mangling scheme we might run into dependencies using the old scheme
        // for which we will fall back to the old mangling scheme as well.
        if (!useOldManglingScheme && replacement.name.asString().contains('-') && function.parentClassId != null) {
            val resolved = (function as? IrSimpleFunction)?.resolveFakeOverride(true)
            if (resolved?.parentClassId?.let { classFileContainsMethod(it, replacement, context) } == false) {
                return buildReplacementInner(function, replacementOrigin, noFakeOverride, true, body)
            }
        }
        return replacement
    }

    private fun buildReplacementInner(
        function: IrFunction,
        replacementOrigin: IrDeclarationOrigin,
        noFakeOverride: Boolean,
        useOldManglingScheme: Boolean,
        body: IrFunction.() -> Unit,
    ): IrSimpleFunction = irFactory.buildFun {
        updateFrom(function)
        if (function is IrConstructor) {
            // The [updateFrom] call will set the modality to FINAL for constructors, while the JVM backend would use OPEN here.
            modality = Modality.OPEN
        }
        origin = when {
            function.origin == IrDeclarationOrigin.GENERATED_SINGLE_FIELD_VALUE_CLASS_MEMBER ->
                JvmLoweredDeclarationOrigin.INLINE_CLASS_GENERATED_IMPL_METHOD
            function is IrConstructor && function.constructedClass.isSingleFieldValueClass ->
                JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_CONSTRUCTOR
            else ->
                replacementOrigin
        }
        if (noFakeOverride) {
            isFakeOverride = false
        }
        name = InlineClassAbi.mangledNameFor(function, mangleReturnTypes, useOldManglingScheme)
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
                        // In case this property is declared in an object in another file which is not yet lowered, its backing field will
                        // be made static later. We have to handle it here though, because this new property will be saved to the cache
                        // and reused when lowering the same call in all subsequent files, which would be incorrect if it was unlowered.
                        backingField = context.cachedDeclarations.getStaticBackingField(propertySymbol.owner)
                            ?: propertySymbol.owner.backingField
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
