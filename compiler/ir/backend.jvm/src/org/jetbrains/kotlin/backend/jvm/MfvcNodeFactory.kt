/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.jvm.IrPropertyOrIrField.Field
import org.jetbrains.kotlin.backend.jvm.IrPropertyOrIrField.Property
import org.jetbrains.kotlin.backend.jvm.NameableMfvcNodeImpl.Companion.MethodFullNameMode
import org.jetbrains.kotlin.backend.jvm.UnboxFunctionImplementation.*
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.backend.jvm.ir.upperBound
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.MultiFieldValueClassRepresentation
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.deepCopyWithVariables
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

fun createLeafMfvcNode(
    parent: IrClass,
    context: JvmBackendContext,
    type: IrType,
    methodFullNameMode: MethodFullNameMode,
    nameParts: List<Name>,
    fieldAnnotations: List<IrConstructorCall>,
    static: Boolean,
    overriddenNode: LeafMfvcNode?,
    defaultMethodsImplementationSourceNode: UnboxFunctionImplementation,
    oldGetter: IrSimpleFunction?,
    modality: Modality,
    oldPropertyBackingField: IrField?,
): LeafMfvcNode {
    require(!type.needsMfvcFlattening()) { "${type.render()} requires flattening" }

    val fullMethodName = NameableMfvcNodeImpl.makeFullMethodName(methodFullNameMode, nameParts)
    val fullFieldName = NameableMfvcNodeImpl.makeFullFieldName(nameParts)

    val field = oldPropertyBackingField?.let { oldBackingField ->
        context.irFactory.buildField {
            updateFrom(oldBackingField)
            this.name = fullFieldName
            this.type = type
            this.visibility = DescriptorVisibilities.PRIVATE
            oldBackingField.metadata = null
        }.apply {
            this.parent = oldBackingField.parent
            this.annotations = fieldAnnotations.map { it.deepCopyWithVariables() }
        }
    }

    val unboxMethod = makeUnboxMethod(
        context,
        fullMethodName,
        type,
        parent,
        overriddenNode,
        static,
        defaultMethodsImplementationSourceNode,
        oldGetter,
        modality,
    ) { receiver -> irGetField(if (field!!.isStatic) null else irGet(receiver!!), field) }

    return LeafMfvcNode(type, methodFullNameMode, nameParts, field, unboxMethod, defaultMethodsImplementationSourceNode)
}

fun IrClass.isKotlinExternalStub() = origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB

private fun makeUnboxMethod(
    context: JvmBackendContext,
    fullMethodName: Name,
    type: IrType,
    parent: IrClass,
    overriddenNode: NameableMfvcNode?,
    static: Boolean,
    defaultMethodsImplementationSourceNode: UnboxFunctionImplementation,
    oldGetter: IrSimpleFunction?,
    modality: Modality,
    makeOptimizedExpression: IrBuilderWithScope.(receiver: IrValueDeclaration?) -> IrExpression,
): IrSimpleFunction {
    val res = oldGetter ?: context.irFactory.buildFun {
        this.name = fullMethodName
        this.origin = JvmLoweredDeclarationOrigin.SYNTHETIC_MULTI_FIELD_VALUE_CLASS_MEMBER
        this.returnType = type
        this.modality = modality
    }.apply {
        this.parent = parent
        overriddenSymbols = overriddenNode?.let { it.unboxMethod.overriddenSymbols + it.unboxMethod.symbol } ?: listOf()
        if (!static) {
            createDispatchReceiverParameter()
        }
    }

    return res.apply {
        if (!parent.isKotlinExternalStub()) {
            body = with(context.createJvmIrBuilder(this.symbol)) {
                val receiver = dispatchReceiverParameter
                if (defaultMethodsImplementationSourceNode.hasPureUnboxMethod) {
                    irExprBody(makeOptimizedExpression(receiver))
                } else {
                    val receiverExpression: IrExpression? = receiver?.let { irGet(it) }
                    val unboxMethodsToCall = when (defaultMethodsImplementationSourceNode) {
                        DefaultUnboxFunctionImplementation -> error("$defaultMethodsImplementationSourceNode is expected to be pure")
                        ExternalUnboxFunctionImplementation -> error("${parent.render()} is expected to be external")
                        is DelegatingUnboxFunctionImplementation -> listOf(defaultMethodsImplementationSourceNode.node.unboxMethod)
                        is CustomUnboxFunctionImplementation -> listOfNotNull(
                            defaultMethodsImplementationSourceNode.getter,
                            (defaultMethodsImplementationSourceNode.node as? NameableMfvcNode)?.unboxMethod
                        )
                    }
                    val expression = unboxMethodsToCall.fold(receiverExpression) { arg, f -> irCall(f).apply { dispatchReceiver = arg } }
                    irExprBody(expression!!)
                }
            }
        }
    }
}


sealed class UnboxFunctionImplementation {
    abstract val hasPureUnboxMethod: Boolean

    object DefaultUnboxFunctionImplementation : UnboxFunctionImplementation() {
        override val hasPureUnboxMethod: Boolean get() = true
    }

    object ExternalUnboxFunctionImplementation : UnboxFunctionImplementation() {
        override val hasPureUnboxMethod: Boolean get() = false
    }

    data class DelegatingUnboxFunctionImplementation(val node: NameableMfvcNode) : UnboxFunctionImplementation() {
        override val hasPureUnboxMethod: Boolean = node.hasPureUnboxMethod
    }

    data class CustomUnboxFunctionImplementation(val getter: IrSimpleFunction, val node: MfvcNode) : UnboxFunctionImplementation() {
        override val hasPureUnboxMethod: Boolean get() = false
    }

    operator fun get(name: Name): UnboxFunctionImplementation {
        fun nextNode(node: MfvcNode) = (node as MfvcNodeWithSubnodes)[name]!!
        return when (this) {
            is CustomUnboxFunctionImplementation -> copy(node = nextNode(node))
            is DelegatingUnboxFunctionImplementation -> copy(node = nextNode(node))
            DefaultUnboxFunctionImplementation -> DefaultUnboxFunctionImplementation
            ExternalUnboxFunctionImplementation -> ExternalUnboxFunctionImplementation
        }
    }
}

fun createNameableMfvcNodes(
    parent: IrClass,
    context: JvmBackendContext,
    type: IrSimpleType,
    typeArguments: TypeArguments,
    methodFullNameMode: MethodFullNameMode,
    nameParts: List<Name>,
    fieldAnnotations: List<IrConstructorCall>,
    static: Boolean,
    overriddenNode: NameableMfvcNode?,
    defaultMethodsImplementationSourceNode: UnboxFunctionImplementation,
    oldGetter: IrSimpleFunction?,
    modality: Modality,
    oldPropertyBackingField: IrField?,
): NameableMfvcNode = if (type.needsMfvcFlattening()) createIntermediateMfvcNode(
    parent,
    context,
    type,
    typeArguments,
    methodFullNameMode,
    nameParts,
    fieldAnnotations,
    static,
    overriddenNode as IntermediateMfvcNode?,
    defaultMethodsImplementationSourceNode,
    oldGetter,
    modality,
    oldPropertyBackingField,
) else createLeafMfvcNode(
    parent,
    context,
    type,
    methodFullNameMode,
    nameParts,
    fieldAnnotations,
    static,
    overriddenNode as LeafMfvcNode?,
    defaultMethodsImplementationSourceNode,
    oldGetter,
    modality,
    oldPropertyBackingField
)

fun createIntermediateMfvcNode(
    parent: IrClass,
    context: JvmBackendContext,
    type: IrSimpleType,
    typeArguments: TypeArguments,
    methodFullNameMode: MethodFullNameMode,
    nameParts: List<Name>,
    fieldAnnotations: List<IrConstructorCall>,
    static: Boolean,
    overriddenNode: IntermediateMfvcNode?,
    defaultMethodsImplementationSourceNode: UnboxFunctionImplementation?,
    oldGetter: IrSimpleFunction?,
    modality: Modality,
    oldPropertyBackingField: IrField?,
): IntermediateMfvcNode {
    require(type.needsMfvcFlattening()) { "${type.render()} does not require flattening" }
    val valueClass = type.erasedUpperBound
    val representation = valueClass.multiFieldValueClassRepresentation!!

    val replacements = context.multiFieldValueClassReplacements
    val rootNode = replacements.getRootMfvcNode(valueClass)

    val oldField = oldGetter?.correspondingPropertySymbol?.owner?.backingFieldIfNotDelegate

    val shadowBackingFieldProperty = if (oldField == null) oldGetter?.getGetterField()?.correspondingPropertySymbol?.owner else null
    val useOldGetter = oldGetter != null && (oldField == null || !oldGetter.isDefaultGetter(oldField))

    val subnodes = representation.underlyingPropertyNamesToTypes.map { (name, type) ->
        val newType = type.substitute(typeArguments) as IrSimpleType
        val newTypeArguments = typeArguments.toMutableMap().apply { putAll(makeTypeArgumentsFromType(newType)) }
        val newDefaultMethodsImplementationSourceNode = when {
            defaultMethodsImplementationSourceNode != null -> defaultMethodsImplementationSourceNode[name]
            shadowBackingFieldProperty != null -> DelegatingUnboxFunctionImplementation(
                replacements.getMfvcPropertyNode(shadowBackingFieldProperty)!!
            )
            useOldGetter -> CustomUnboxFunctionImplementation(oldGetter!!, rootNode[name]!!)
            else -> DefaultUnboxFunctionImplementation
        }
        createNameableMfvcNodes(
            parent,
            context,
            newType,
            newTypeArguments,
            methodFullNameMode,
            nameParts + name,
            fieldAnnotations,
            static,
            overriddenNode?.let { it[name]!! },
            newDefaultMethodsImplementationSourceNode,
            null,
            modality,
            oldPropertyBackingField,
        )
    }

    val fullMethodName = NameableMfvcNodeImpl.makeFullMethodName(methodFullNameMode, nameParts)

    val unboxMethod: IrSimpleFunction
    val unboxFunctionImplementation: UnboxFunctionImplementation
    if (useOldGetter) {
        unboxMethod = oldGetter!!
        unboxFunctionImplementation = defaultMethodsImplementationSourceNode ?: CustomUnboxFunctionImplementation(unboxMethod, rootNode)
    } else {
        unboxFunctionImplementation = defaultMethodsImplementationSourceNode ?: DefaultUnboxFunctionImplementation
        unboxMethod = makeUnboxMethod(
            context, fullMethodName, type, parent, overriddenNode, static, unboxFunctionImplementation, oldGetter, modality
        ) { receiver ->
            val valueArguments = subnodes.flatMap { it.fields!! }
                .map { field -> irGetField(if (field.isStatic) null else irGet(receiver!!), field) }
            rootNode.makeBoxedExpression(this, typeArguments, valueArguments, registerPossibleExtraBoxCreation = {})
        }
    }
    return IntermediateMfvcNode(type, methodFullNameMode, nameParts, subnodes, unboxMethod, unboxFunctionImplementation, rootNode)
}

fun collectPropertiesAfterLowering(irClass: IrClass, context: JvmBackendContext): LinkedHashSet<IrProperty> =
    LinkedHashSet(collectPropertiesOrFieldsAfterLowering(irClass, context).map { (it as Property).property })

sealed class IrPropertyOrIrField {
    data class Property(val property: IrProperty) : IrPropertyOrIrField() {
        override fun toString(): String = property.dump()
    }

    data class Field(val field: IrField) : IrPropertyOrIrField() {
        override fun toString(): String = field.dump()
    }
}

fun collectPropertiesOrFieldsAfterLowering(irClass: IrClass, context: JvmBackendContext): LinkedHashSet<IrPropertyOrIrField> =
    LinkedHashSet<IrPropertyOrIrField>().apply {
        fun handleField(element: IrField) {
            val property = element.correspondingPropertySymbol?.owner
            if (
                property != null && !property.isDelegated &&
                !context.multiFieldValueClassReplacements.getFieldsToRemove(element.parentAsClass).contains(element)
            ) {
                add(Property(property))
            } else {
                add(Field(element))
            }
        }

        fun handleAccessor(element: IrSimpleFunction) {
            if (element.extensionReceiverParameter == null && element.contextReceiverParametersCount == 0) {
                element.correspondingPropertySymbol?.owner?.let { add(Property(it)) }
            }
        }

        for (element in irClass.declarations) {
            when (element) {
                is IrField -> handleField(element)
                is IrSimpleFunction -> handleAccessor(element)
                is IrProperty -> {
                    element.backingField?.let(::handleField)
                    element.getter?.let(::handleAccessor)
                }
            }
        }
    }

private fun IrProperty.isStatic(currentContainer: IrDeclarationContainer) =
    getterIfDeclared(currentContainer)?.isStatic
        ?: backingFieldIfNotDelegate?.isStatic
        ?: error("Property without both getter and backing field:\n${dump()}")

fun getRootNode(context: JvmBackendContext, mfvc: IrClass): RootMfvcNode {
    require(mfvc.isMultiFieldValueClass) { "${mfvc.defaultType.render()} does not require flattening" }
    val oldPrimaryConstructor = mfvc.primaryConstructor!!
    val oldFields = mfvc.declarations.mapNotNull { it as? IrField ?: (it as? IrProperty)?.backingField }.filter { !it.isStatic }
    val representation = mfvc.multiFieldValueClassRepresentation!!
    val properties = collectPropertiesAfterLowering(mfvc, context).associateBy { it.isStatic(mfvc) to it.name }

    val subnodes = makeRootMfvcNodeSubnodes(representation, properties, context, mfvc)

    val leaves = subnodes.leaves
    val fields = subnodes.fields

    val newPrimaryConstructor = makeMfvcPrimaryConstructor(context, oldPrimaryConstructor, mfvc, leaves, fields)
    val primaryConstructorImpl = makePrimaryConstructorImpl(context, oldPrimaryConstructor, mfvc, leaves, subnodes)
    val boxMethod = makeBoxMethod(context, mfvc, leaves, newPrimaryConstructor)

    val customEqualsAny = mfvc.functions.singleOrNull {
        it.isEquals() && it.origin != IrDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER
    }

    val customEqualsMfvc = mfvc.functions.singleOrNull {
        it.name == OperatorNameConventions.EQUALS &&
                it.contextReceiverParametersCount == 0 &&
                it.extensionReceiverParameter == null &&
                it.valueParameters.singleOrNull()?.type?.erasedUpperBound == mfvc
    }

    val specializedEqualsMethod = makeSpecializedEqualsMethod(context, mfvc, oldFields, customEqualsAny, customEqualsMfvc)

    return RootMfvcNode(
        mfvc, subnodes, oldPrimaryConstructor, newPrimaryConstructor, primaryConstructorImpl, boxMethod,
        specializedEqualsMethod, customEqualsMfvc == null
    )
}

private fun makeSpecializedEqualsMethod(
    context: JvmBackendContext, mfvc: IrClass, oldFields: List<IrField>,
    customEqualsAny: IrSimpleFunction?, customEqualsMfvc: IrSimpleFunction?
) = customEqualsMfvc ?: context.irFactory.buildFun {
    name = OperatorNameConventions.EQUALS
    origin = IrDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER
    returnType = context.irBuiltIns.booleanType
}.apply {
    parent = mfvc
    createDispatchReceiverParameter()

    val other = addValueParameter {
        name = Name.identifier("other")
        type = mfvc.defaultType.upperBound
    }
    if (!mfvc.isKotlinExternalStub()) {
        body = with(context.createJvmIrBuilder(this.symbol)) {
            if (customEqualsAny != null) {
                irExprBody(irCall(customEqualsAny).apply {
                    dispatchReceiver = irGet(dispatchReceiverParameter!!)
                    putValueArgument(0, irGet(other))
                })
            } else {
                val leftArgs = oldFields.map { irGetField(irGet(dispatchReceiverParameter!!), it) }
                val rightArgs = oldFields.map { irGetField(irGet(other), it) }
                val conjunctions = leftArgs.zip(rightArgs) { l, r -> irEquals(l, r) }
                irExprBody(conjunctions.reduce { acc, current ->
                    irCall(context.irBuiltIns.andandSymbol).apply {
                        putValueArgument(0, acc)
                        putValueArgument(1, current)
                    }
                })
            }
        }
    }
}

private fun makeBoxMethod(
    context: JvmBackendContext,
    mfvc: IrClass,
    leaves: List<LeafMfvcNode>,
    newPrimaryConstructor: IrConstructor
) = context.irFactory.buildFun {
    name = Name.identifier(KotlinTypeMapper.BOX_JVM_METHOD_NAME)
    origin = JvmLoweredDeclarationOrigin.SYNTHETIC_MULTI_FIELD_VALUE_CLASS_MEMBER
    returnType = mfvc.defaultType
}.apply {
    parent = mfvc
    copyTypeParametersFrom(mfvc)
    val mapping = mfvc.typeParameters.zip(typeParameters) { classTypeParameter, functionTypeParameter ->
        classTypeParameter.symbol to functionTypeParameter.defaultType
    }.toMap()
    returnType = returnType.substitute(mapping)
    val parameters = leaves.map { leaf -> addValueParameter(leaf.fullFieldName, leaf.type.substitute(mapping)) }
    if (!mfvc.isKotlinExternalStub()) {
        body = with(context.createJvmIrBuilder(this.symbol)) {
            irExprBody(irCall(newPrimaryConstructor).apply {
                for ((index, parameter) in parameters.withIndex()) {
                    putValueArgument(index, irGet(parameter))
                }
            })
        }
    }
}

private fun makePrimaryConstructorImpl(
    context: JvmBackendContext,
    oldPrimaryConstructor: IrConstructor,
    mfvc: IrClass,
    leaves: List<LeafMfvcNode>,
    subnodes: List<NameableMfvcNode>,
) = context.irFactory.buildFun {
    name = InlineClassAbi.mangledNameFor(oldPrimaryConstructor, false, false)
    visibility = oldPrimaryConstructor.visibility
    origin = JvmLoweredDeclarationOrigin.STATIC_MULTI_FIELD_VALUE_CLASS_CONSTRUCTOR
    returnType = context.irBuiltIns.unitType
    modality = Modality.FINAL
}.apply {
    parent = mfvc
    copyTypeParametersFrom(mfvc)
    for (leaf in leaves) {
        addValueParameter(leaf.fullFieldName, leaf.type.substitute(mfvc.typeParameters, typeParameters.map { it.defaultType }))
    }
    for ((index, oldParameter) in oldPrimaryConstructor.valueParameters.withIndex()) {
        val node = subnodes[index]
        val subnodesIndices = subnodes.subnodeIndices
        if (node is LeafMfvcNode) {
            val newIndex = subnodesIndices[node]!!.first
            valueParameters[newIndex].annotations = oldParameter.annotations
        }
    }
    annotations = oldPrimaryConstructor.annotations
    if (oldPrimaryConstructor.metadata != null) {
        metadata = oldPrimaryConstructor.metadata
        oldPrimaryConstructor.metadata = null
    }
    copyAttributes(oldPrimaryConstructor as? IrAttributeContainer)
    // body is added in the Lowering file as it needs to be lowered
}

private fun makeMfvcPrimaryConstructor(
    context: JvmBackendContext,
    oldPrimaryConstructor: IrConstructor,
    mfvc: IrClass,
    leaves: List<LeafMfvcNode>,
    fields: List<IrField>?
) = context.irFactory.buildConstructor {
    updateFrom(oldPrimaryConstructor)
    visibility = DescriptorVisibilities.PRIVATE
    origin = JvmLoweredDeclarationOrigin.SYNTHETIC_MULTI_FIELD_VALUE_CLASS_MEMBER
    returnType = oldPrimaryConstructor.returnType
}.apply {
    require(oldPrimaryConstructor.typeParameters.isEmpty()) { "Constructors do not support type parameters yet" }
    this.parent = mfvc
    val parameters = leaves.map { addValueParameter(it.fullFieldName, it.type) }
    val irConstructor = this@apply
    if (!mfvc.isKotlinExternalStub()) {
        body = context.createIrBuilder(irConstructor.symbol).irBlockBody(irConstructor) {
            +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
            for ((field, parameter) in fields!! zip parameters) {
                +irSetField(irGet(mfvc.thisReceiver!!), field, irGet(parameter))
            }
        }
    }
}

private fun makeRootMfvcNodeSubnodes(
    representation: MultiFieldValueClassRepresentation<IrSimpleType>,
    properties: Map<Pair<Boolean, Name>, IrProperty>,
    context: JvmBackendContext,
    mfvc: IrClass
) = representation.underlyingPropertyNamesToTypes.map { (name, type) ->
    val typeArguments = makeTypeArgumentsFromType(type)
    val oldProperty = properties[false to name]!!
    val oldBackingField = oldProperty.backingFieldIfNotDelegate
    val oldGetter = oldProperty.getterIfDeclared(mfvc)
    val overriddenNode = oldGetter?.let { getOverriddenNode(context.multiFieldValueClassReplacements, it) as IntermediateMfvcNode? }
    val static = oldProperty.isStatic(mfvc)
    createNameableMfvcNodes(
        mfvc,
        context,
        type,
        typeArguments,
        MethodFullNameMode.UnboxFunction,
        listOf(name),
        oldBackingField?.annotations ?: listOf(),
        static,
        overriddenNode,
        DefaultUnboxFunctionImplementation,
        oldGetter.takeIf { static },
        Modality.FINAL,
        oldBackingField,
    ).also {
        updateAnnotationsAndPropertyFromOldProperty(oldProperty, context, it)
        it.unboxMethod.overriddenSymbols = listOf() // the getter is saved so it overrides itself
    }
}

private fun updateAnnotationsAndPropertyFromOldProperty(
    oldProperty: IrProperty,
    context: JvmBackendContext,
    node: MfvcNode,
) {
    if (node is LeafMfvcNode) return
    oldProperty.backingField?.let {
        context.multiFieldValueClassReplacements.addFieldToRemove(it.parentAsClass, it)
    }
}

fun createIntermediateNodeForMfvcPropertyOfRegularClass(
    parent: IrClass,
    context: JvmBackendContext,
    oldProperty: IrProperty,
): IntermediateMfvcNode {
    val oldGetter = oldProperty.getterIfDeclared(parent)
    val oldField = oldProperty.backingFieldIfNotDelegate
    val type = oldProperty.getter?.returnType ?: oldField?.type ?: error("Either getter or field must exist")
    require(type is IrSimpleType && type.needsMfvcFlattening()) { "Expected MFVC but got ${type.render()}" }
    val fieldAnnotations = oldField?.annotations ?: listOf()
    val static = oldProperty.isStatic(parent)
    val overriddenNode = oldGetter?.let { getOverriddenNode(context.multiFieldValueClassReplacements, it) as IntermediateMfvcNode? }
    val modality = if (oldGetter == null || oldGetter.modality == Modality.FINAL) Modality.FINAL else oldGetter.modality
    val defaultMethodsImplementationSourceNode = if (parent.isKotlinExternalStub()) ExternalUnboxFunctionImplementation else null
    return createIntermediateMfvcNode(
        parent, context, type, makeTypeArgumentsFromType(type), MethodFullNameMode.Getter, listOf(oldProperty.name),
        fieldAnnotations, static, overriddenNode, defaultMethodsImplementationSourceNode, oldGetter, modality, oldField
    ).also {
        updateAnnotationsAndPropertyFromOldProperty(oldProperty, context, it)
    }
}

fun createIntermediateNodeForStandaloneMfvcField(
    parent: IrClass,
    context: JvmBackendContext,
    oldField: IrField,
): IntermediateMfvcNode {
    val type = oldField.type
    require(type is IrSimpleType && type.needsMfvcFlattening()) { "Expected MFVC but got ${type.render()}" }
    return createIntermediateMfvcNode(
        parent, context, type, makeTypeArgumentsFromType(type), MethodFullNameMode.Getter, listOf(oldField.name),
        oldField.annotations, oldField.isStatic, null, null, null, Modality.FINAL, oldField
    )
}

private fun getOverriddenNode(replacements: MemoizedMultiFieldValueClassReplacements, getter: IrSimpleFunction): NameableMfvcNode? =
    getter.overriddenSymbols
        .firstOrNull { !it.owner.isFakeOverride }
        ?.let { replacements.getMfvcPropertyNode(it.owner.correspondingPropertySymbol!!.owner) }

private fun IrProperty.getterIfDeclared(parent: IrDeclarationContainer): IrSimpleFunction? =
    getter?.takeIf { it in parent.declarations || this in parent.declarations }

private val IrProperty.backingFieldIfNotDelegate get() = backingField?.takeUnless { isDelegated }
