/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.jvm.MemoizedMultiFieldValueClassReplacements.RemappedParameter.MultiFieldValueClassMapping
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.concurrent.ConcurrentHashMap

/**
 * Keeps track of replacement functions and multi-field value class box/unbox functions.
 */
class MemoizedMultiFieldValueClassReplacements(
    irFactory: IrFactory,
    context: JvmBackendContext,
    private val typeSystemContext: IrTypeSystemContext
) : MemoizedValueClassAbstractReplacements(irFactory, context) {
    private val storageManager = LockBasedStorageManager("multi-field-value-class-replacements")

    val originalFunctionForStaticReplacement: MutableMap<IrFunction, IrFunction> = ConcurrentHashMap()
    val originalFunctionForMethodReplacement: MutableMap<IrFunction, IrFunction> = ConcurrentHashMap()
    private val originalConstructorForConstructorReplacement: MutableMap<IrConstructor, IrConstructor> = ConcurrentHashMap()

    val getDeclarations: (IrClass) -> MultiFieldValueClassSpecificDeclarations? =
        storageManager.createMemoizedFunctionWithNullableValues {
            if (it.isMultiFieldValueClass)
                MultiFieldValueClassSpecificDeclarations(it, typeSystemContext, irFactory, context, this)
            else
                null
        }

    val getOldMFVCProperties: (IrClass) -> List<IrProperty> = storageManager.createMemoizedFunction { irClass ->
        require(irClass.isMultiFieldValueClass) { "No need to save data for non MFVC" }
        (irClass.properties.toList().filter { it.getter?.isMultiFieldValueClassOriginalFieldGetter == true }.takeIf { it.isNotEmpty() }
            ?: irClass.fields.mapNotNull { it.correspondingPropertySymbol?.owner }.toList().takeIf { it.isNotEmpty() }
            ?: error("No properties found: ${irClass.render()}"))
    }

    fun commitMFVCOldProperties(irClass: IrClass) {
        getOldMFVCProperties(irClass)
    }

    class ValueParameterTemplate(
        val name: String,
        val type: IrType,
        val origin: IrDeclarationOrigin?,
        val defaultValue: IrExpressionBody?,
        val original: IrValueParameter, // todo don't use it as it is not always correct
    ) {

        constructor(valueParameter: IrValueParameter, origin: IrDeclarationOrigin?) : this(
            name = valueParameter.name.asString(),
            type = valueParameter.type,
            origin = origin,
            defaultValue = valueParameter.defaultValue,
            original = valueParameter,
        )

        fun toParameter(irFunction: IrFunction, index: Int): IrValueParameter = original.copyTo(
            irFunction = irFunction,
            index = index,
            name = Name.guessByFirstCharacter(name),
            origin = origin ?: original.origin,
            defaultValue = null,
            type = type,
        ).also {
            // Assuming that constructors and non-override functions are always replaced with the unboxed
            // equivalent, deep-copying the value here is unnecessary.
            it.defaultValue = defaultValue?.patchDeclarationParents(irFunction)
        }
    }

    private fun IrFunction.makeValueParametersFromTemplate(newFlattenedParameters: List<RemappedParameter>) = newFlattenedParameters
        .flatMap { it.valueParameters }.mapIndexed { index: Int, template -> template.toParameter(this, index) }

    private fun List<ValueParameterTemplate>.grouped(
        substitutionMap: Map<IrTypeParameterSymbol, IrType>,
        originWhenFlattenedAndNotSpecified: IrDeclarationOrigin? = IrDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_PARAMETER
    ): List<RemappedParameter> = map { parameter ->
        val declarations = parameter.type.takeIf { !it.isNullable() }?.erasedUpperBound?.let { getDeclarations(it) }
            ?: return@map RemappedParameter.RegularMapping(parameter)
        require(!parameter.original.hasDefaultValue()) { "Default parameters values are not supported for multi-field value classes" }
        val localSubstitutionMap = declarations.valueClass.typeParameters.zip((parameter.type as IrSimpleType).arguments)
            .mapNotNull { (parameter, argument) -> if (argument is IrSimpleType) parameter.symbol to argument.type else null}.toMap()
        MultiFieldValueClassMapping(declarations, parameter.type.substitute(substitutionMap) as IrSimpleType, declarations.leaves.map { leaf ->
                ValueParameterTemplate(
                    name = "${parameter.name}$${declarations.nodeFullNames[leaf]!!}",
                    type = leaf.type.substitute(localSubstitutionMap),
                    origin = parameter.origin ?: originWhenFlattenedAndNotSpecified,
                    defaultValue = null,
                    original = parameter.original,
                )
            })
    }

    private fun buildReplacement(
        function: IrFunction,
        replacementOrigin: IrDeclarationOrigin,
        noFakeOverride: Boolean = false,
        body: IrFunction.() -> Unit,
    ): IrSimpleFunction = commonBuildReplacementInner(function, noFakeOverride, body) {
        origin = when {
            function.origin == IrDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER ->
                JvmLoweredDeclarationOrigin.MULTI_FIELD_VALUE_CLASS_GENERATED_IMPL_METHOD
            function is IrConstructor && function.constructedClass.isMultiFieldValueClass ->
                JvmLoweredDeclarationOrigin.STATIC_MULTI_FIELD_VALUE_CLASS_CONSTRUCTOR
            else -> replacementOrigin
        }
        name = InlineClassAbi.mangledNameFor(function, mangleReturnTypes = false, useOldMangleRules = false)
    }

    private fun makeGroupedValueParametersFrom(
        function: IrFunction, includeDispatcherReceiver: Boolean, substitutionMap: Map<IrTypeParameterSymbol, IrType>
    ): List<RemappedParameter> {
        val newFlattenedParameters = mutableListOf<RemappedParameter>()
        if (function.dispatchReceiverParameter != null && includeDispatcherReceiver) {
            val template = ValueParameterTemplate(
                name = "\$dispatchReceiver",
                type = function.parentAsClass.defaultType,
                origin = IrDeclarationOrigin.MOVED_DISPATCH_RECEIVER,
                defaultValue = null,
                original = function.parentAsClass.thisReceiver!!,
            )
            newFlattenedParameters.addAll(listOf(template).grouped(substitutionMap))
        }
        val contextReceivers = function.valueParameters.take(function.contextReceiverParametersCount)
            .mapIndexed { index: Int, valueParameter: IrValueParameter ->
                ValueParameterTemplate(
                    name = "contextReceiver$index",
                    origin = IrDeclarationOrigin.MOVED_CONTEXT_RECEIVER,
                    type = valueParameter.type,
                    defaultValue = valueParameter.defaultValue,
                    original = valueParameter,
                )
            }
            .grouped(substitutionMap)
        newFlattenedParameters.addAll(contextReceivers)
        function.extensionReceiverParameter?.let {
            val template = ValueParameterTemplate(
                name = Name.identifier(function.extensionReceiverName(context.state)).asString(),
                origin = IrDeclarationOrigin.MOVED_EXTENSION_RECEIVER,
                type = it.type,
                defaultValue = it.defaultValue,
                original = it,
            )
            newFlattenedParameters.addAll(listOf(template).grouped(substitutionMap))
        }
        newFlattenedParameters += function.valueParameters.drop(function.contextReceiverParametersCount).map {
            ValueParameterTemplate(it, origin = null)
        }.grouped(substitutionMap)
        return newFlattenedParameters
    }

    sealed class RemappedParameter {
        data class RegularMapping(val valueParameter: ValueParameterTemplate) : RemappedParameter() {
            override val valueParameters: List<ValueParameterTemplate> = listOf(valueParameter)
        }

        data class MultiFieldValueClassMapping(
            val declarations: MultiFieldValueClassSpecificDeclarations,
            val boxedType: IrSimpleType,
            override val valueParameters: List<ValueParameterTemplate>,
        ) : RemappedParameter() {
            init {
                require(valueParameters.size > 1) { "MFVC must have > 1 fields" }
            }
        }

        abstract val valueParameters: List<ValueParameterTemplate>
    }

    val bindingOldFunctionToParameterTemplateStructure: MutableMap<IrFunction, List<RemappedParameter>> = ConcurrentHashMap()
    val bindingNewFunctionToParameterTemplateStructure: MutableMap<IrFunction, List<RemappedParameter>> =
        object : ConcurrentHashMap<IrFunction, List<RemappedParameter>>() {
            override fun put(key: IrFunction, value: List<RemappedParameter>): List<RemappedParameter>? {
                require(key.explicitParametersCount == value.sumOf { it.valueParameters.size }) {
                    "Illegal structure $value for function ${key.dump()}"
                }
                return super.put(key, value)
            }
        }
// todo inline classes pass type arguments correctly
    override fun createStaticReplacement(function: IrFunction): IrSimpleFunction =
        buildReplacement(function, JvmLoweredDeclarationOrigin.STATIC_MULTI_FIELD_VALUE_CLASS_REPLACEMENT, noFakeOverride = true) {
            originalFunctionForStaticReplacement[this] = function
            typeParameters = listOf()
            copyTypeParametersFrom(function.parentAsClass)
            val substitutionMap = function.parentAsClass.typeParameters.map { it.symbol }.zip(typeParameters.map { it.defaultType }).toMap()
            copyTypeParametersFrom(function, parameterMap = (function.parentAsClass.typeParameters zip typeParameters).toMap())
            val newFlattenedParameters = makeGroupedValueParametersFrom(function, includeDispatcherReceiver = true, substitutionMap)
            valueParameters = makeValueParametersFromTemplate(newFlattenedParameters)
            bindingOldFunctionToParameterTemplateStructure[function] = newFlattenedParameters
            bindingNewFunctionToParameterTemplateStructure[this] = newFlattenedParameters
        }

    override fun createMethodReplacement(function: IrFunction): IrSimpleFunction = buildReplacement(function, function.origin) {
        originalFunctionForMethodReplacement[this] = function
        dispatchReceiverParameter = function.dispatchReceiverParameter?.copyTo(this, index = -1)
        val newFlattenedParameters = makeGroupedValueParametersFrom(function, includeDispatcherReceiver = false, mapOf())
        val receiver = dispatchReceiverParameter
        val receiverTemplate = if (receiver != null) ValueParameterTemplate(receiver, origin = receiver.origin) else null
        val remappedParameters =
            if (receiverTemplate != null) listOf(RemappedParameter.RegularMapping(receiverTemplate)) + newFlattenedParameters else newFlattenedParameters
        valueParameters = makeValueParametersFromTemplate(newFlattenedParameters)
        bindingOldFunctionToParameterTemplateStructure[function] = remappedParameters
        bindingNewFunctionToParameterTemplateStructure[this] = remappedParameters
    }

    private fun createConstructorReplacement(@Suppress("UNUSED_PARAMETER") constructor: IrConstructor): IrConstructor {
        val newFlattenedParameters = makeGroupedValueParametersFrom(constructor, includeDispatcherReceiver = false, mapOf())
        bindingOldFunctionToParameterTemplateStructure[constructor] = newFlattenedParameters
        val newConstructor = irFactory.buildConstructor {
            updateFrom(constructor)
            returnType = constructor.returnType
        }.apply {
            parent = constructor.parent
            copyTypeParametersFrom(constructor)
            valueParameters = makeValueParametersFromTemplate(newFlattenedParameters)
            annotations = constructor.annotations
            originalConstructorForConstructorReplacement[this] = constructor
        }
        bindingNewFunctionToParameterTemplateStructure[newConstructor] = newFlattenedParameters
        return newConstructor
    }

    private fun createGetterReplacement(function: IrFunction): IrSimpleFunction {
        val declarations = getDeclarations(function.parentAsClass)!!
        val name = (function as IrSimpleFunction).correspondingPropertySymbol!!.owner.name
        val node = declarations.loweringRepresentation[name]!!.node
        val newGetter = declarations.properties[node]!!.getter!!
        originalFunctionForMethodReplacement[newGetter] = function
        val receiver = function.dispatchReceiverParameter!!
        val receiverTemplate = ValueParameterTemplate(receiver, origin = receiver.origin)
        val parameterRemapping = listOf(RemappedParameter.RegularMapping(receiverTemplate))
        bindingOldFunctionToParameterTemplateStructure[function] = parameterRemapping
        bindingNewFunctionToParameterTemplateStructure[newGetter] = parameterRemapping
        return newGetter
    }

    /**
     * Get a replacement for a function or a constructor.
     */
    override val getReplacementFunction: (IrFunction) -> IrSimpleFunction? =
        storageManager.createMemoizedFunctionWithNullableValues { function ->
            when {
                (function.isLocal && function is IrSimpleFunction && function.overriddenSymbols.isEmpty()) ||
                        (function.origin == IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR && function.visibility == DescriptorVisibilities.LOCAL) ||
                        function.isStaticValueClassReplacement ||
                        function.origin == IrDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER && function.isAccessor ||
                        function.origin == JvmLoweredDeclarationOrigin.MULTI_FIELD_VALUE_CLASS_GENERATED_IMPL_METHOD ||
                        function.origin.isSynthetic && function.origin != IrDeclarationOrigin.SYNTHETIC_GENERATED_SAM_IMPLEMENTATION -> null
                function.isMultiFieldValueClassOriginalFieldGetter -> createGetterReplacement(function)
                function.parent.safeAs<IrClass>()?.isMultiFieldValueClass == true -> when {
                    function.isRemoveAtSpecialBuiltinStub() ->
                        null
                    function.isValueClassMemberFakeOverriddenFromJvmDefaultInterfaceMethod() ||
                            function.origin == IrDeclarationOrigin.IR_BUILTINS_STUB ->
                        createMethodReplacement(function)
                    else ->
                        createStaticReplacement(function)
                }
                function is IrSimpleFunction && !function.isFromJava() &&
                        function.fullValueParameterList.any { it.type.isMultiFieldValueClassType() && !it.type.isNullable() } &&
                        (!function.isFakeOverride ||
                                findSuperDeclaration(function, false, context.state.jvmDefaultMode)
                                in bindingOldFunctionToParameterTemplateStructure) ->
                    createMethodReplacement(function)
                else -> null
            }
        }

    override val getReplacementRegularClassConstructor: (IrConstructor) -> IrConstructor? =
        storageManager.createMemoizedFunctionWithNullableValues { constructor ->
            when {
                constructor.constructedClass.isMultiFieldValueClass -> null
                constructor.isFromJava() -> null
                constructor.fullValueParameterList.any { !it.type.isNullable() && it.type.isMultiFieldValueClassType() } ->
                    createConstructorReplacement(constructor)
                else -> null
            }
        }


    override val replaceOverriddenSymbols: (IrSimpleFunction) -> List<IrSimpleFunctionSymbol> =
        storageManager.createMemoizedFunction { irSimpleFunction ->
            irSimpleFunction.overriddenSymbols.map {
                computeOverrideReplacement(it.owner).symbol
            }
        }
}

