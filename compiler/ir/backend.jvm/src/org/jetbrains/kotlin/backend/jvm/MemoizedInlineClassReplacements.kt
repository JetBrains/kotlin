/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.InlineClassDescriptorResolver
import org.jetbrains.kotlin.storage.LockBasedStorageManager

var IrFunction.originalFunctionOfStaticInlineClassReplacement: IrFunction? by irAttribute(copyByDefault = false)

/**
 * Keeps track of replacement functions and inline class box/unbox functions.
 */
class MemoizedInlineClassReplacements(
    private val mangleReturnTypes: Boolean,
    irFactory: IrFactory,
    context: JvmBackendContext
) : MemoizedValueClassAbstractReplacements(irFactory, context, LockBasedStorageManager("inline-class-replacements")) {
    private val mangleCallsToJavaMethodsWithValueClasses =
        context.config.languageVersionSettings.supportsFeature(LanguageFeature.MangleCallsToJavaMethodsWithValueClasses)

    /**
     * Get a replacement for a function or a constructor.
     */
    override val getReplacementFunctionImpl: (IrFunction) -> IrSimpleFunction? =
        storageManager.createMemoizedFunctionWithNullableValues {
            when {
                // Don't mangle anonymous or synthetic functions, except for generated SAM wrapper methods
                (it.isLocal && it is IrSimpleFunction && it.overriddenSymbols.isEmpty()) ||
                        (it.origin == IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR && it.visibility == DescriptorVisibilities.LOCAL) ||
                        it.isStaticValueClassReplacement ||
                        it.origin == JvmLoweredDeclarationOrigin.MULTI_FIELD_VALUE_CLASS_GENERATED_IMPL_METHOD ||
                        it.origin.isSynthetic && it.origin != IrDeclarationOrigin.SYNTHETIC_GENERATED_SAM_IMPLEMENTATION ->
                    null

                it.isInlineClassFieldGetter ->
                    if (it.hasMangledReturnType)
                        createMethodReplacement(it)
                    else
                        null

                // Mangle all functions in the body of an inline class
                (it.parent as? IrClass)?.isSingleFieldValueClass == true ->
                    when {
                        it.isValueClassTypedEquals -> createStaticReplacement(it).also {
                            it.name = InlineClassDescriptorResolver.SPECIALIZED_EQUALS_NAME
                            specializedEqualsCache.computeIfAbsent(it.parentAsClass) { it }
                        }

                        it.isRemoveAtSpecialBuiltinStub() ->
                            null

                        it.isValueClassMemberFakeOverriddenFromJvmDefaultInterfaceMethod() ||
                                it.origin == IrDeclarationOrigin.IR_BUILTINS_STUB ->
                            createMethodReplacement(it)

                        else ->
                            createStaticReplacement(it)
                    }

                // Otherwise, mangle functions with mangled parameters, ignoring constructors
                it is IrSimpleFunction && it.needsReplacement -> createMethodReplacement(it)

                else ->
                    null
            }
        }

    private val IrSimpleFunction.needsReplacement: Boolean
        get() {
            if (!(shouldBeExposedByAnnotationOrFlag(context.config.languageVersionSettings) || hasMangledParameters(includeMFVC = false) ||
                        mangleReturnTypes && hasMangledReturnType)
            ) return false
            if (isFromJava()) return mangleCallsToJavaMethodsWithValueClasses && !overridesOnlyMethodsFromJava()
            return true
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
                parameters += createDispatchReceiverParameterWithClassParent()
            }
        }

    private val specializedEqualsCache = storageManager.createCacheWithNotNullValues<IrClass, IrSimpleFunction>()
    fun getSpecializedEqualsMethod(irClass: IrClass, irBuiltIns: IrBuiltIns): IrSimpleFunction {
        require(irClass.isSingleFieldValueClass)
        return specializedEqualsCache.computeIfAbsent(irClass) {
            irFactory.buildFun {
                name = InlineClassDescriptorResolver.SPECIALIZED_EQUALS_NAME
                // TODO: Revisit this once we allow user defined equals methods in inline/multi-field value classes.
                origin = JvmLoweredDeclarationOrigin.INLINE_CLASS_GENERATED_IMPL_METHOD
                returnType = irBuiltIns.booleanType
            }.apply {
                parent = irClass
                // We ignore type arguments here, since there is no good way to go from type arguments to types in the IR anyway.
                val typeArgument =
                    IrSimpleTypeImpl(irClass.symbol, false, List(irClass.typeParameters.size) { IrStarProjectionImpl }, listOf())
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

    private val IrValueParameter.inlineClassPropertyNames: List<Name>
        get() = if ((parent as? IrFunction)?.parameterTemplateStructureOfThisNewMfvcBidingFunction != null) emptyList() else type.inlineClassPropertyNames

    override fun createMethodReplacement(function: IrFunction): IrSimpleFunction =
        buildReplacement(function, function.origin) {
            parameters += function.parameters.map { parameter ->
                val inlineClassPropertyNames = parameter.inlineClassPropertyNames
                parameter.copyTo(
                    this,
                    defaultValue = null,
                    name = if (parameter.kind == IrParameterKind.ExtensionReceiver) {
                        // The function's name will be mangled, so preserve the old receiver name.
                        function.extensionReceiverName(context.config).withInlineClassParameterNameIfNeeded(inlineClassPropertyNames)
                    } else if (parameter.kind == IrParameterKind.Context) {
                        function.anonymousContextParameterName(parameter)?.withInlineClassParameterNameIfNeeded(inlineClassPropertyNames)
                            ?: parameter.name.withInlineClassParameterNameIfNeeded(inlineClassPropertyNames)
                    } else {
                        parameter.name.withInlineClassParameterNameIfNeeded(inlineClassPropertyNames)
                    },
                    origin = when (parameter.kind) {
                        IrParameterKind.Context -> IrDeclarationOrigin.MOVED_CONTEXT_RECEIVER
                        IrParameterKind.ExtensionReceiver -> IrDeclarationOrigin.EXTENSION_RECEIVER_WITH_FIXED_NAME
                        else -> parameter.origin
                    }
                ).also {
                    // Assuming that constructors and non-override functions are always replaced with the unboxed
                    // equivalent, deep-copying the value here is unnecessary. See `JvmInlineClassLowering`.
                    it.defaultValue = parameter.defaultValue?.patchDeclarationParents(this)
                }
            }
            context.remapMultiFieldValueClassStructure(function, this, parametersMappingOrNull = null)
        }

    override fun createStaticReplacement(function: IrFunction): IrSimpleFunction =
        buildReplacement(function, JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_REPLACEMENT, noFakeOverride = true) {
            this.originalFunctionOfStaticInlineClassReplacement = function

            parameters += function.parameters.map { parameter ->
                val inlineClassPropertyNames = parameter.inlineClassPropertyNames

                when (parameter.kind) {
                    IrParameterKind.DispatchReceiver -> {
                        // FAKE_OVERRIDEs have broken dispatch receivers
                        val parent = function.parentAsClass
                        parent.thisReceiver!!.copyTo(
                            this,
                            name = AsmUtil.THIS.withInlineClassParameterNameIfNeeded(inlineClassPropertyNames),
                            type = parent.defaultType,
                            origin = IrDeclarationOrigin.MOVED_DISPATCH_RECEIVER,
                            kind = IrParameterKind.Regular,
                        )
                    }
                    IrParameterKind.Context -> {
                        parameter.copyTo(
                            this,
                            name = (function.anonymousContextParameterName(parameter)?.withInlineClassParameterNameIfNeeded(inlineClassPropertyNames)
                                ?: parameter.name.withInlineClassParameterNameIfNeeded(inlineClassPropertyNames)),
                            origin = IrDeclarationOrigin.MOVED_CONTEXT_RECEIVER,
                            kind = IrParameterKind.Regular,
                        )
                    }
                    IrParameterKind.ExtensionReceiver -> {
                        parameter.copyTo(
                            this,
                            name = function.extensionReceiverName(context.config)
                                .withInlineClassParameterNameIfNeeded(inlineClassPropertyNames),
                            origin = IrDeclarationOrigin.MOVED_EXTENSION_RECEIVER,
                            kind = IrParameterKind.Regular,
                        )
                    }
                    IrParameterKind.Regular -> {
                        parameter.copyTo(
                            this,
                            defaultValue = null,
                            name = parameter.name.withInlineClassParameterNameIfNeeded(inlineClassPropertyNames),
                        ).also {
                            // See comment next to a similar line above.
                            it.defaultValue = parameter.defaultValue?.patchDeclarationParents(this)
                        }
                    }
                }
            }

            context.remapMultiFieldValueClassStructure(function, this, parametersMappingOrNull = null)
        }

    private fun buildReplacement(
        function: IrFunction,
        replacementOrigin: IrDeclarationOrigin,
        noFakeOverride: Boolean = false,
        body: IrFunction.() -> Unit
    ): IrSimpleFunction {
        val useOldManglingScheme = context.config.useOldManglingSchemeForFunctionsWithInlineClassesInSignatures
        val replacement = buildReplacementInner(function, replacementOrigin, noFakeOverride, useOldManglingScheme, body)
        // When using the new mangling scheme we might run into dependencies using the old scheme
        // for which we will fall back to the old mangling scheme as well.
        if (!useOldManglingScheme && replacement.name.asString().contains('-') && function.parentClassId != null) {
            val resolved = (function as? IrSimpleFunction)?.resolveFakeOverrideMaybeAbstractOrFail()
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
    ): IrSimpleFunction = commonBuildReplacementInner(function, noFakeOverride, body) {
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
        name = InlineClassAbi.mangledNameFor(context, function, mangleReturnTypes, useOldManglingScheme)
    }

    override fun getReplacementForRegularClassConstructor(constructor: IrConstructor): IrConstructor? = null
}
