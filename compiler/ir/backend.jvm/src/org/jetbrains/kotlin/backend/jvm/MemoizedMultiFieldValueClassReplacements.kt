/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.jvm.MemoizedMultiFieldValueClassReplacements.RemappedParameter
import org.jetbrains.kotlin.backend.jvm.MemoizedMultiFieldValueClassReplacements.RemappedParameter.MultiFieldValueClassMapping
import org.jetbrains.kotlin.backend.jvm.MemoizedMultiFieldValueClassReplacements.RemappedParameter.RegularMapping
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.irComposite
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrComposite
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.InlineClassDescriptorResolver
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.getOrSetIfNull
import java.util.concurrent.ConcurrentHashMap

var IrConstructor.originalConstructorOfThisMfvcConstructorReplacement: IrConstructor? by irAttribute(copyByDefault = false)

private var IrClass.mfvcFieldsToRemove: MutableSet<IrField>? by irAttribute(copyByDefault = false)

var IrValueParameter.oldMfvcDefaultArgument: IrExpression? by irAttribute(copyByDefault = false)

var IrFunction.parameterTemplateStructureOfThisOldMfvcBidingFunction: List<RemappedParameter>? by irAttribute(copyByDefault = false)

private var IrFunction._parameterTemplateStructureOfThisNewMfvcBidingFunction: List<RemappedParameter>? by irAttribute(copyByDefault = false)
var IrFunction.parameterTemplateStructureOfThisNewMfvcBidingFunction: List<RemappedParameter>?
    get() = _parameterTemplateStructureOfThisNewMfvcBidingFunction
    set(value) {
        if (value != null) {
            require(parameters.size == value.sumOf { it.parameters.size }) {
                "Illegal structure $value for function ${this.dump()}"
            }
        }
        _parameterTemplateStructureOfThisNewMfvcBidingFunction = value
    }

/**
 * Keeps track of replacement functions and multi-field value class box/unbox functions.
 */
class MemoizedMultiFieldValueClassReplacements(
    irFactory: IrFactory,
    context: JvmBackendContext
) : MemoizedValueClassAbstractReplacements(irFactory, context, LockBasedStorageManager("multi-field-value-class-replacements")) {

    private val IrValueParameter.inlineClassPropertyNames: List<Name>
        get() = type.inlineClassPropertyNames

    private fun IrValueParameter.grouped(
        name: String?,
        substitutionMap: Map<IrTypeParameterSymbol, IrType>,
        targetFunction: IrFunction,
        originWhenFlattened: IrDeclarationOrigin,
        originWhenNotFlattened: IrDeclarationOrigin,
    ): RemappedParameter {
        val oldParam = this
        if (!type.needsMfvcFlattening()) return RegularMapping(
            targetFunction.addValueParameter {
                updateFrom(oldParam)
                this.name = name?.withInlineClassParameterNameIfNeeded(inlineClassPropertyNames)
                    ?: oldParam.name.withInlineClassParameterNameIfNeeded(inlineClassPropertyNames)
                this.origin = originWhenNotFlattened
            }.apply {
                defaultValue = oldParam.defaultValue
                copyAnnotationsFrom(oldParam)
            }
        )
        val rootMfvcNode = this@MemoizedMultiFieldValueClassReplacements.getRootMfvcNode(type.erasedUpperBound)
        defaultValue?.expression?.let { this::oldMfvcDefaultArgument.getOrSetIfNull { it } }
        val newType = type.substitute(substitutionMap) as IrSimpleType
        val localSubstitutionMap = makeTypeArgumentsFromType(newType)
        val valueParameters = rootMfvcNode.mapLeaves { leaf ->
            targetFunction.addValueParameter {
                updateFrom(oldParam)
                type = leaf.type.substitute(localSubstitutionMap)
                val inlineClassPropertyNames = type.inlineClassPropertyNames
                this.name = "${name ?: oldParam.name}-${leaf.fullFieldName}".withInlineClassParameterNameIfNeeded(inlineClassPropertyNames)
                origin = originWhenFlattened
                isAssignable = isAssignable || oldParam.defaultValue != null
            }.also { newParam ->
                newParam.defaultValue = oldParam.defaultValue?.let {
                    context.createJvmIrBuilder(targetFunction.symbol).run { irExprBody(irGet(newParam)) }
                }
                require(oldParam.annotations.isEmpty()) { "Annotations are not supported for MFVC parameters" }
            }
        }
        return MultiFieldValueClassMapping(rootMfvcNode, newType, valueParameters)
    }

    private fun List<IrValueParameter>.grouped(
        name: String?,
        substitutionMap: Map<IrTypeParameterSymbol, IrType>,
        targetFunction: IrFunction,
        originWhenFlattened: IrDeclarationOrigin,
        originWhenNotFlattened: IrDeclarationOrigin,
    ): List<RemappedParameter> = map { it.grouped(name, substitutionMap, targetFunction, originWhenFlattened, originWhenNotFlattened) }

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
        name =
            if (function.isLocal && (function !is IrSimpleFunction || function.overriddenSymbols.isEmpty())) function.name
            else InlineClassAbi.mangledNameFor(context, function, mangleReturnTypes = false, useOldMangleRules = false)
    }

    private fun makeAndAddGroupedValueParametersFrom(
        sourceFunction: IrFunction,
        includeDispatcherReceiver: Boolean,
        substitutionMap: Map<IrTypeParameterSymbol, IrType>,
        targetFunction: IrFunction,
    ): List<RemappedParameter> {
        return sourceFunction.parameters.mapNotNull { param ->
            val sourceParam = if (param.kind == IrParameterKind.DispatchReceiver) {
                if (includeDispatcherReceiver) sourceFunction.parentAsClass.thisReceiver!! else null
            } else param
            val name = when (param.kind) {
                IrParameterKind.DispatchReceiver -> AsmUtil.THIS
                IrParameterKind.ExtensionReceiver -> sourceFunction.extensionReceiverName(context.config)
                IrParameterKind.Context -> sourceFunction.anonymousContextParameterName(param)
                IrParameterKind.Regular -> null
            }
            val originWhenNotFlattened = when (param.kind) {
                IrParameterKind.DispatchReceiver -> IrDeclarationOrigin.MOVED_DISPATCH_RECEIVER
                IrParameterKind.Context -> IrDeclarationOrigin.MOVED_CONTEXT_RECEIVER
                IrParameterKind.ExtensionReceiver -> IrDeclarationOrigin.MOVED_EXTENSION_RECEIVER
                IrParameterKind.Regular -> param.origin
            }
            val originWhenFlattened = when (param.kind) {
                IrParameterKind.Regular -> JvmLoweredDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_PARAMETER
                else -> originWhenNotFlattened
            }

            sourceParam?.grouped(
                name,
                substitutionMap,
                targetFunction,
                originWhenFlattened,
                originWhenNotFlattened
            )
        }
    }

    sealed class RemappedParameter {

        abstract val parameters: List<IrValueParameter>

        data class RegularMapping(val valueParameter: IrValueParameter) : RemappedParameter() {
            override val parameters: List<IrValueParameter> = listOf(valueParameter)
            override fun toString(): String {
                return "RegularMapping(valueParameter=${valueParameter.render()})"
            }
        }

        data class MultiFieldValueClassMapping(
            val rootMfvcNode: RootMfvcNode,
            val typeArguments: TypeArguments,
            override val parameters: List<IrValueParameter>,
        ) : RemappedParameter() {
            init {
                require(parameters.size > 1) { "MFVC must have > 1 fields" }
            }

            constructor(rootMfvcNode: RootMfvcNode, type: IrSimpleType, valueParameters: List<IrValueParameter>) :
                    this(rootMfvcNode, makeTypeArgumentsFromType(type), valueParameters)

            val boxedType: IrSimpleType = rootMfvcNode.type.substitute(typeArguments) as IrSimpleType
            override fun toString(): String {
                return """MultiFieldValueClassMapping(
                    |    rootMfvcNode=$rootMfvcNode,
                    |    typeArguments=[${typeArguments.values.joinToString(",") { "\n        " + it.render() }}],
                    |    valueParameters=[${parameters.joinToString(",") { "\n        " + it.render() }}],
                    |    boxedType=${boxedType.render()}
                    |)""".trimMargin()
            }


        }
    }

    override fun createStaticReplacement(function: IrFunction): IrSimpleFunction =
        buildReplacement(function, JvmLoweredDeclarationOrigin.STATIC_MULTI_FIELD_VALUE_CLASS_REPLACEMENT, noFakeOverride = true) {
            typeParameters = listOf()
            copyTypeParametersFrom(function.parentAsClass)
            val substitutionMap = function.parentAsClass.typeParameters.map { it.symbol }.zip(typeParameters.map { it.defaultType }).toMap()
            copyTypeParametersFrom(function, parameterMap = (function.parentAsClass.typeParameters zip typeParameters).toMap())
            val newFlattenedParameters =
                makeAndAddGroupedValueParametersFrom(function, includeDispatcherReceiver = true, substitutionMap, this)
            function.parameterTemplateStructureOfThisOldMfvcBidingFunction = newFlattenedParameters
            this.parameterTemplateStructureOfThisNewMfvcBidingFunction = newFlattenedParameters
        }

    override fun createMethodReplacement(function: IrFunction): IrSimpleFunction = buildReplacement(function, function.origin) {
        val remappedParameters = makeMethodLikeRemappedParameters(function)
        function.parameterTemplateStructureOfThisOldMfvcBidingFunction = remappedParameters
        this.parameterTemplateStructureOfThisNewMfvcBidingFunction = remappedParameters
    }

    private fun createConstructorReplacement(constructor: IrConstructor): IrConstructor {
        val newConstructor = irFactory.buildConstructor {
            updateFrom(constructor)
            returnType = constructor.returnType
        }.apply {
            parent = constructor.parent
            val remappedParameters = makeMethodLikeRemappedParameters(constructor)
            constructor.parameterTemplateStructureOfThisOldMfvcBidingFunction = remappedParameters
            copyTypeParametersFrom(constructor)
            annotations = constructor.annotations
            this.originalConstructorOfThisMfvcConstructorReplacement = constructor
            this.parameterTemplateStructureOfThisNewMfvcBidingFunction = remappedParameters
            if (constructor.metadata != null) {
                metadata = constructor.metadata
                constructor.metadata = null
            }
        }
        return newConstructor
    }

    private fun IrFunction.makeMethodLikeRemappedParameters(function: IrFunction): List<RemappedParameter> {
        parameters = listOfNotNull(function.dispatchReceiverParameter?.copyTo(this)) + nonDispatchParameters
        val newFlattenedParameters = makeAndAddGroupedValueParametersFrom(function, includeDispatcherReceiver = false, mapOf(), this)
        val receiver = dispatchReceiverParameter
        return if (receiver != null) listOf(RegularMapping(receiver)) + newFlattenedParameters else newFlattenedParameters
    }

    /**
     * Get a function replacement for a function or a constructor.
     */
    override val getReplacementFunctionImpl: (IrFunction) -> IrSimpleFunction? =
        storageManager.createMemoizedFunctionWithNullableValues { function ->
            when {
                (function.origin == IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR && function.visibility == DescriptorVisibilities.LOCAL) ||
                        function.isStaticValueClassReplacement ||
                        function.origin == IrDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER && function.isAccessor ||
                        function.origin == JvmLoweredDeclarationOrigin.MULTI_FIELD_VALUE_CLASS_GENERATED_IMPL_METHOD ||
                        (function.origin.isSynthetic && function.origin != IrDeclarationOrigin.SYNTHETIC_GENERATED_SAM_IMPLEMENTATION &&
                                !(function is IrConstructor && function.constructedClass.isMultiFieldValueClass && !function.isPrimary)) ||
                        function.isMultiFieldValueClassFieldGetter -> null

                (function.parent as? IrClass)?.isMultiFieldValueClass == true -> when {
                    function.isValueClassTypedEquals -> createStaticReplacement(function).also {
                        it.name = InlineClassDescriptorResolver.SPECIALIZED_EQUALS_NAME
                    }

                    function.isRemoveAtSpecialBuiltinStub() ->
                        null

                    function.isValueClassMemberFakeOverriddenFromJvmDefaultInterfaceMethod() ||
                            function.origin == IrDeclarationOrigin.IR_BUILTINS_STUB ->
                        createMethodReplacement(function)

                    else ->
                        createStaticReplacement(function)
                }

                function is IrSimpleFunction
                        && !(function.isFromJava() && function.overridesOnlyMethodsFromJava())
                        && function.nonDispatchParameters.any { it.type.needsMfvcFlattening() }
                        && run {
                    if (!function.isFakeOverride) return@run true
                    val superDeclaration = findSuperDeclaration(function)
                    getReplacementFunction(superDeclaration) != null
                } -> createMethodReplacement(function)

                else -> null
            }
        }

    private val getReplacementForRegularClassConstructorImpl: (IrConstructor) -> IrConstructor? =
        storageManager.createMemoizedFunctionWithNullableValues { constructor ->
            when {
                constructor.isFromJava() -> null //is recursive so run once
                else -> createConstructorReplacement(constructor)
            }
        }

    override fun getReplacementForRegularClassConstructor(constructor: IrConstructor): IrConstructor? = when {
        constructor.constructedClass.isMultiFieldValueClass -> null
        constructor.parameters.none { it.type.needsMfvcFlattening() } -> null
        else -> getReplacementForRegularClassConstructorImpl(constructor)
    }

    val getRootMfvcNode: (IrClass) -> RootMfvcNode = storageManager.createMemoizedFunction {
        require(it.defaultType.needsMfvcFlattening()) { it.defaultType.render() }
        getRootNode(context, it)
    }

    fun getRootMfvcNodeOrNull(irClass: IrClass): RootMfvcNode? =
        if (irClass.defaultType.needsMfvcFlattening()) getRootMfvcNode(irClass) else null

    private val getRegularClassMfvcPropertyNodeImpl: (IrProperty) -> IntermediateMfvcNode =
        storageManager.createMemoizedFunction { property: IrProperty ->
            val parent = property.parentAsClass
            createIntermediateNodeForMfvcPropertyOfRegularClass(parent, context, property)
        }

    private fun IrField.withAddedStaticReplacementIfNeeded(): IrField {
        val property = this.correspondingPropertySymbol?.owner ?: return this
        val replacementField = context.cachedDeclarations.getStaticBackingField(property) ?: return this
        property.backingField = replacementField
        return replacementField
    }

    private fun IrProperty.withAddedStaticReplacementIfNeeded(): IrProperty {
        val replacementField = context.cachedDeclarations.getStaticBackingField(this) ?: return this
        backingField = replacementField
        return this
    }

    fun getFieldsToRemove(clazz: IrClass): Set<IrField> = clazz.mfvcFieldsToRemove ?: emptySet()
    fun addFieldToRemove(clazz: IrClass, field: IrField) {
        clazz::mfvcFieldsToRemove.getOrSetIfNull {
            ConcurrentHashMap<IrField, Unit>().keySet(Unit)
        }.add(field.withAddedStaticReplacementIfNeeded())
    }

    fun getMfvcFieldNode(field: IrField): NameableMfvcNode? {
        val realField = field.withAddedStaticReplacementIfNeeded()
        val parent = realField.parent
        val property = realField.correspondingPropertySymbol?.owner
        return when {
            property?.isDelegated == false -> getMfvcPropertyNode(property)
            parent !is IrClass -> null
            !realField.type.needsMfvcFlattening() -> null
            else -> getMfvcStandaloneFieldNodeImpl(realField)
        }
    }

    private val getMfvcStandaloneFieldNodeImpl: (IrField) -> NameableMfvcNode = storageManager.createMemoizedFunction { field ->
        val parent = field.parentAsClass
        createIntermediateNodeForStandaloneMfvcField(parent, context, field)
    }

    private fun getRegularClassMfvcPropertyNode(property: IrProperty): IntermediateMfvcNode? {
        val parent = property.parent
        val types = listOfNotNull(
            property.backingFieldIfNotToRemove?.takeUnless { property.isDelegated }?.type, property.getter?.returnType
        )
        return when {
            types.isEmpty() || types.any { !it.needsMfvcFlattening() } -> null
            parent !is IrClass -> null
            property.isFakeOverride -> null
            property.getter.let { it != null && it.nonDispatchParameters.isNotEmpty() } -> null
            useRootNode(parent, property) -> null
            else -> getRegularClassMfvcPropertyNodeImpl(property)
        }
    }

    fun getMfvcPropertyNode(property: IrProperty): NameableMfvcNode? {
        val parent = property.parent
        val realProperty = property.withAddedStaticReplacementIfNeeded()
        return when {
            parent !is IrClass -> null
            useRootNode(parent, realProperty) -> getRootMfvcNode(parent)[realProperty.name]
            else -> getRegularClassMfvcPropertyNode(realProperty)
        }
    }

    private fun useRootNode(parent: IrClass, property: IrProperty): Boolean {
        val getter = property.getter
        if (getter != null && getter.nonDispatchParameters.isNotEmpty()) return false
        return parent.isMultiFieldValueClass && (getter?.isStatic ?: property.backingFieldIfNotToRemove?.isStatic) == false
    }

    private val IrProperty.backingFieldIfNotToRemove get() = backingField?.takeUnless { it in getFieldsToRemove(this.parentAsClass) }

    private val FLATTENED_NOTHING_DEFAULT_VALUE by IrStatementOriginImpl

    fun mapFunctionMfvcStructures(
        irBuilder: IrBlockBuilder,
        targetFunction: IrFunction,
        sourceFunction: IrFunction,
        getArgument: (sourceParameter: IrValueParameter, targetParameterType: IrType) -> IrExpression?
    ): Map<IrValueParameter, IrExpression?> {
        val targetStructure = targetFunction.parameterTemplateStructureOfThisNewMfvcBidingFunction
            ?: targetFunction.parameters.map { RegularMapping(it) }
        val sourceStructure = sourceFunction.parameterTemplateStructureOfThisNewMfvcBidingFunction
            ?: sourceFunction.parameters.map { RegularMapping(it) }
        verifyStructureCompatibility(targetStructure, sourceStructure)
        return buildMap {
            for ((targetParameterStructure, sourceParameterStructure) in targetStructure zip sourceStructure) {
                when (targetParameterStructure) {
                    is RegularMapping -> when (sourceParameterStructure) {
                        is RegularMapping -> put(
                            targetParameterStructure.valueParameter,
                            getArgument(sourceParameterStructure.valueParameter, targetParameterStructure.valueParameter.type)
                        )
                        is MultiFieldValueClassMapping -> {
                            val valueArguments = sourceParameterStructure.parameters.map {
                                getArgument(it, it.type) ?: error("Expected an argument for $sourceParameterStructure")
                            }
                            val newArgument =
                                if (valueArguments.all { it is IrComposite && it.origin == FLATTENED_NOTHING_DEFAULT_VALUE }) {
                                    (valueArguments[0] as IrComposite).statements[0] as IrExpression
                                } else {
                                    sourceParameterStructure.rootMfvcNode.makeBoxedExpression(
                                        irBuilder,
                                        sourceParameterStructure.typeArguments,
                                        valueArguments,
                                        registerPossibleExtraBoxCreation = {}
                                    )
                                }
                            put(targetParameterStructure.valueParameter, newArgument)
                        }
                    }
                    is MultiFieldValueClassMapping -> when (sourceParameterStructure) {
                        is RegularMapping -> with(irBuilder) {
                            val argument = getArgument(sourceParameterStructure.valueParameter, targetParameterStructure.boxedType)
                                ?: error("Expected an argument for $sourceParameterStructure")
                            if (sourceParameterStructure.valueParameter.type.isNothing()) {
                                for ((index, parameter) in targetParameterStructure.parameters.withIndex()) {
                                    put(parameter, irComposite(origin = FLATTENED_NOTHING_DEFAULT_VALUE) {
                                        if (index == 0) +argument
                                        +parameter.type.defaultValue(
                                            UNDEFINED_OFFSET, UNDEFINED_OFFSET, this@MemoizedMultiFieldValueClassReplacements.context
                                        )
                                    })
                                }
                            } else {
                                val instance = targetParameterStructure.rootMfvcNode.createInstanceFromBox(
                                    scope = this,
                                    receiver = argument,
                                    accessType = AccessType.ChooseEffective,
                                    saveVariable = { +it }
                                )
                                val arguments = instance.makeFlattenedGetterExpressions(
                                    this, sourceFunction.parents.firstIsInstance<IrClass>(), registerPossibleExtraBoxCreation = {}
                                )
                                for ((targetParameter, expression) in targetParameterStructure.parameters zip arguments) {
                                    put(targetParameter, expression)
                                }
                            }
                        }
                        is MultiFieldValueClassMapping -> {
                            for ((sourceParameter, targetParameter) in sourceParameterStructure.parameters zip targetParameterStructure.parameters) {
                                put(targetParameter, getArgument(sourceParameter, targetParameter.type))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun verifyStructureCompatibility(
        targetStructure: List<RemappedParameter>,
        sourceStructure: List<RemappedParameter>
    ) {
        for ((targetParameterStructure, sourceParameterStructure) in targetStructure zip sourceStructure) {
            if (targetParameterStructure is MultiFieldValueClassMapping && sourceParameterStructure is MultiFieldValueClassMapping) {
                require(targetParameterStructure.rootMfvcNode.mfvc.classId == sourceParameterStructure.rootMfvcNode.mfvc.classId) {
                    "Incompatible parameter structures:\n$targetParameterStructure inside $targetStructure\n$sourceParameterStructure inside $sourceStructure"
                }
            }
        }
        for (extraParameterStructure in targetStructure.slice(sourceStructure.size..<targetStructure.size)) {
            require(extraParameterStructure is RegularMapping && extraParameterStructure.valueParameter.origin != IrDeclarationOrigin.DEFINED) {
                "Unexpected target MFVC parameter structure:\n$extraParameterStructure inside $targetStructure"
            }
        }
        for (extraParameterStructure in sourceStructure.slice(targetStructure.size..<sourceStructure.size)) {
            require(extraParameterStructure is RegularMapping && extraParameterStructure.valueParameter.origin != IrDeclarationOrigin.DEFINED) {
                "Unexpected source MFVC parameter structure:\n$extraParameterStructure inside $sourceStructure"
            }
        }
    }
}
