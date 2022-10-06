/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.jvm.MemoizedMultiFieldValueClassReplacements.RemappedParameter.MultiFieldValueClassMapping
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.backend.jvm.ir.extensionReceiverName
import org.jetbrains.kotlin.backend.jvm.ir.findSuperDeclaration
import org.jetbrains.kotlin.backend.jvm.ir.isStaticValueClassReplacement
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
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
    context: JvmBackendContext
) : MemoizedValueClassAbstractReplacements(irFactory, context) {
    private val storageManager = LockBasedStorageManager("multi-field-value-class-replacements")

    val originalFunctionForStaticReplacement: MutableMap<IrFunction, IrFunction> = ConcurrentHashMap()
    val originalFunctionForMethodReplacement: MutableMap<IrFunction, IrFunction> = ConcurrentHashMap()
    private val originalConstructorForConstructorReplacement: MutableMap<IrConstructor, IrConstructor> = ConcurrentHashMap()

    private fun IrValueParameter.grouped(
        name: String?,
        substitutionMap: Map<IrTypeParameterSymbol, IrType>,
        targetFunction: IrFunction,
        originWhenFlattened: IrDeclarationOrigin,
    ): RemappedParameter {
        if (!type.needsMfvcFlattening()) return RemappedParameter.RegularMapping(
            targetFunction.addValueParameter {
                updateFrom(this@grouped)
                this.name = this@grouped.name
                index = targetFunction.valueParameters.size
            }.apply {
                copyAnnotationsFrom(this@grouped)
            }
        )
        val rootMfvcNode = this@MemoizedMultiFieldValueClassReplacements.getRootMfvcNode(type.erasedUpperBound)!!
        require(!hasDefaultValue()) { "Default parameters values are not supported for multi-field value classes" }
        val newType = type.substitute(substitutionMap) as IrSimpleType
        val localSubstitutionMap = makeTypeArgumentsFromType(newType)
        val valueParameters = rootMfvcNode.mapLeaves { leaf ->
            targetFunction.addValueParameter {
                updateFrom(this@grouped)
                this.name = Name.identifier("${name ?: this@grouped.name}-${leaf.fullFieldName}")
                type = leaf.type.substitute(localSubstitutionMap)
                origin = originWhenFlattened
                index = targetFunction.valueParameters.size
            }.apply {
                defaultValue = null
                copyAnnotationsFrom(this@grouped)
            }
        }
        return MultiFieldValueClassMapping(rootMfvcNode, newType, valueParameters)
    }

    private fun List<IrValueParameter>.grouped(
        name: String?,
        substitutionMap: Map<IrTypeParameterSymbol, IrType>,
        targetFunction: IrFunction,
        originWhenFlattened: IrDeclarationOrigin,
    ): List<RemappedParameter> = map { it.grouped(name, substitutionMap, targetFunction, originWhenFlattened) }

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

    private fun makeAndAddGroupedValueParametersFrom(
        sourceFunction: IrFunction,
        includeDispatcherReceiver: Boolean,
        substitutionMap: Map<IrTypeParameterSymbol, IrType>,
        targetFunction: IrFunction,
    ): List<RemappedParameter> {
        val newFlattenedParameters = mutableListOf<RemappedParameter>()
        if (sourceFunction.dispatchReceiverParameter != null && includeDispatcherReceiver) {
            newFlattenedParameters.add(
                sourceFunction.parentAsClass.thisReceiver!!.grouped(
                    "\$dispatchReceiver",
                    substitutionMap,
                    targetFunction,
                    IrDeclarationOrigin.MOVED_DISPATCH_RECEIVER,
                )
            )
        }
        val contextReceivers = sourceFunction.valueParameters.take(sourceFunction.contextReceiverParametersCount)
            .mapIndexed { index: Int, valueParameter: IrValueParameter ->
                valueParameter.grouped(
                    "contextReceiver$index",
                    substitutionMap,
                    targetFunction,
                    IrDeclarationOrigin.MOVED_CONTEXT_RECEIVER,
                )
            }
        newFlattenedParameters.addAll(contextReceivers)
        sourceFunction.extensionReceiverParameter?.let {
            val newParameters = it.grouped(
                sourceFunction.extensionReceiverName(context.state),
                substitutionMap,
                targetFunction,
                IrDeclarationOrigin.MOVED_EXTENSION_RECEIVER,
            )
            newFlattenedParameters.add(newParameters)
        }
        newFlattenedParameters += sourceFunction.valueParameters.drop(sourceFunction.contextReceiverParametersCount)
            .grouped(name = null, substitutionMap, targetFunction, IrDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_PARAMETER)
        return newFlattenedParameters
    }

    sealed class RemappedParameter {

        abstract val valueParameters: List<IrValueParameter>

        data class RegularMapping(val valueParameter: IrValueParameter) : RemappedParameter() {
            override val valueParameters: List<IrValueParameter> = listOf(valueParameter)
        }

        data class MultiFieldValueClassMapping(
            val rootMfvcNode: RootMfvcNode,
            val typeArguments: TypeArguments,
            override val valueParameters: List<IrValueParameter>,
        ) : RemappedParameter() {
            init {
                require(valueParameters.size > 1) { "MFVC must have > 1 fields" }
            }

            constructor(rootMfvcNode: RootMfvcNode, type: IrSimpleType, valueParameters: List<IrValueParameter>) :
                    this(rootMfvcNode, makeTypeArgumentsFromType(type), valueParameters)

            val boxedType: IrSimpleType = rootMfvcNode.type.substitute(typeArguments) as IrSimpleType
        }
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

    override fun createStaticReplacement(function: IrFunction): IrSimpleFunction =
        buildReplacement(function, JvmLoweredDeclarationOrigin.STATIC_MULTI_FIELD_VALUE_CLASS_REPLACEMENT, noFakeOverride = true) {
            originalFunctionForStaticReplacement[this] = function
            typeParameters = listOf()
            copyTypeParametersFrom(function.parentAsClass)
            val substitutionMap = function.parentAsClass.typeParameters.map { it.symbol }.zip(typeParameters.map { it.defaultType }).toMap()
            copyTypeParametersFrom(function, parameterMap = (function.parentAsClass.typeParameters zip typeParameters).toMap())
            val newFlattenedParameters =
                makeAndAddGroupedValueParametersFrom(function, includeDispatcherReceiver = true, substitutionMap, this)
            bindingOldFunctionToParameterTemplateStructure[function] = newFlattenedParameters
            bindingNewFunctionToParameterTemplateStructure[this] = newFlattenedParameters
        }

    override fun createMethodReplacement(function: IrFunction): IrSimpleFunction = buildReplacement(function, function.origin) {
        originalFunctionForMethodReplacement[this] = function
        dispatchReceiverParameter = function.dispatchReceiverParameter?.copyTo(this, index = -1)
        val newFlattenedParameters = makeAndAddGroupedValueParametersFrom(function, includeDispatcherReceiver = false, mapOf(), this)
        val receiver = dispatchReceiverParameter
        val remappedParameters =
            if (receiver != null) listOf(RemappedParameter.RegularMapping(receiver)) + newFlattenedParameters else newFlattenedParameters
        bindingOldFunctionToParameterTemplateStructure[function] = remappedParameters
        bindingNewFunctionToParameterTemplateStructure[this] = remappedParameters
    }

    private fun createConstructorReplacement(constructor: IrConstructor): IrConstructor {
        val newConstructor = irFactory.buildConstructor {
            updateFrom(constructor)
            returnType = constructor.returnType
        }.apply {
            val newFlattenedParameters = makeAndAddGroupedValueParametersFrom(constructor, includeDispatcherReceiver = false, mapOf(), this)
            bindingOldFunctionToParameterTemplateStructure[constructor] = newFlattenedParameters
            parent = constructor.parent
            copyTypeParametersFrom(constructor)
            annotations = constructor.annotations
            originalConstructorForConstructorReplacement[this] = constructor
            bindingNewFunctionToParameterTemplateStructure[this] = newFlattenedParameters
        }
        return newConstructor
    }

    /**
     * Get a function replacement for a function or a constructor.
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

                // Do not check for overridden symbols because it makes previously overriding function not overriding would break a code.
                function.isMultiFieldValueClassFieldGetter -> makeMultiFieldValueClassFieldGetterReplacement(function)
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
                        function.fullValueParameterList.any { it.type.needsMfvcFlattening() } &&
                        (!function.isFakeOverride ||
                                findSuperDeclaration(function, false, context.state.jvmDefaultMode)
                                in bindingOldFunctionToParameterTemplateStructure) ->
                    createMethodReplacement(function)

                else -> null
            }
        }

    private fun makeMultiFieldValueClassFieldGetterReplacement(function: IrFunction): IrSimpleFunction {
        require(function is IrSimpleFunction && function.isMultiFieldValueClassFieldGetter) { "Illegal function:\n${function.dump()}" }
        val replacement = getMfvcPropertyNode(function.correspondingPropertySymbol!!.owner)!!.unboxMethod
        originalFunctionForMethodReplacement[replacement] = function
        val templateParameters = listOf(RemappedParameter.RegularMapping(replacement.dispatchReceiverParameter!!))
        bindingNewFunctionToParameterTemplateStructure[replacement] = templateParameters
        bindingOldFunctionToParameterTemplateStructure[function] = templateParameters
        return replacement
    }

    override val getReplacementForRegularClassConstructor: (IrConstructor) -> IrConstructor? =
        storageManager.createMemoizedFunctionWithNullableValues { constructor ->
            when {
                constructor.constructedClass.isMultiFieldValueClass -> null
                constructor.isFromJava() -> null
                constructor.fullValueParameterList.any { it.type.needsMfvcFlattening() } ->
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

    val getRootMfvcNode: (IrClass) -> RootMfvcNode? = storageManager.createMemoizedFunctionWithNullableValues {
        if (it.defaultType.needsMfvcFlattening()) getRootNode(context, it) else null
    }

    val getRegularClassMfvcPropertyNode: (IrProperty) -> IntermediateMfvcNode? =
        storageManager.createMemoizedFunctionWithNullableValues { property: IrProperty ->
            val parent = property.parent
            when {
                parent !is IrClass -> null
                property.isFakeOverride -> null
                property.getter.let { it != null && (it.contextReceiverParametersCount > 0 || it.extensionReceiverParameter != null) } -> null
                useRootNode(parent, property) -> null
                property.run { backingField?.type ?: getter?.returnType }?.needsMfvcFlattening() != true -> null
                else -> createIntermediateNodeForMfvcPropertyOfRegularClass(parent, context, property)
            }
        }

    fun getMfvcPropertyNode(property: IrProperty): NameableMfvcNode? {
        val parent = property.parent
        return when {
            parent !is IrClass -> null
            useRootNode(parent, property) -> getRootMfvcNode(parent)!![property.name]
            else -> getRegularClassMfvcPropertyNode(property)
        }
    }

    private fun useRootNode(
        parent: IrClass,
        property: IrProperty
    ) = parent.isMultiFieldValueClass && (property.getter?.isStatic ?: property.backingField?.isStatic) == false
}
