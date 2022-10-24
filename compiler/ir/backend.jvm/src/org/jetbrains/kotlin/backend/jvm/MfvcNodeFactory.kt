/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.parents
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.MultiFieldValueClassRepresentation
import org.jetbrains.kotlin.ir.IrElement
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
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.InlineClassDescriptorResolver

fun createLeafMfvcNode(
    parent: IrDeclarationContainer,
    context: JvmBackendContext,
    type: IrType,
    rootPropertyName: String?,
    nameParts: List<IndexedNamePart>,
    fieldAnnotations: List<IrConstructorCall>,
    static: Boolean,
    overriddenNode: LeafMfvcNode?,
    defaultMethodsImplementationSourceNode: Pair<IrSimpleFunction?, LeafMfvcNode>?, // used if the getter was custom and need to call it.
    oldGetter: IrSimpleFunction?,
    modality: Modality,
    oldPropertyBackingField: IrField?,
): LeafMfvcNode {
    require(!type.needsMfvcFlattening()) { "${type.render()} requires flattening" }

    val fullMethodName = NameableMfvcNodeImpl.makeFullMethodName(rootPropertyName, nameParts)
    val fullFieldName = NameableMfvcNodeImpl.makeFullFieldName(rootPropertyName, nameParts)

    val field = oldPropertyBackingField?.let { oldBackingField ->
        context.irFactory.buildField {
            updateFrom(oldBackingField)
            this.name = fullFieldName
            this.type = type
            this.visibility = DescriptorVisibilities.PRIVATE
            this.metadata = null
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

    return LeafMfvcNode(type, rootPropertyName, nameParts, field, unboxMethod, defaultMethodsImplementationSourceNode.isPure())
}

private fun Pair<IrSimpleFunction?, NameableMfvcNode>?.isPure(): Boolean {
    val (outer, inner) = this ?: return true
    return outer == null && inner.hasPureUnboxMethod
}

private fun makeUnboxMethod(
    context: JvmBackendContext,
    fullMethodName: Name,
    type: IrType,
    parent: IrDeclarationParent,
    overriddenNode: NameableMfvcNode?,
    static: Boolean,
    defaultMethodsImplementationSourceNode: Pair<IrSimpleFunction?, NameableMfvcNode>?,
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
        body = with(context.createJvmIrBuilder(this.symbol)) {
            val receiver = dispatchReceiverParameter
            if (defaultMethodsImplementationSourceNode == null) {
                irExprBody(makeOptimizedExpression(receiver))
            } else {
                val (outer, inner) = defaultMethodsImplementationSourceNode
                val receiverExpression = receiver?.let { irGet(it) }
                val outerCall = if (outer == null) receiverExpression else irCall(outer).apply { dispatchReceiver = receiverExpression }
                val innerCall = irCall(inner.unboxMethod).apply { dispatchReceiver = outerCall }
                irExprBody(innerCall)
            }
        }
    }
}

fun createNameableMfvcNodes(
    parent: IrDeclarationContainer,
    context: JvmBackendContext,
    type: IrSimpleType,
    typeArguments: TypeArguments,
    rootPropertyName: String?,
    nameParts: List<IndexedNamePart>,
    fieldAnnotations: List<IrConstructorCall>,
    static: Boolean,
    overriddenNode: NameableMfvcNode?,
    defaultMethodsImplementationSourceNode: Pair<IrSimpleFunction?, NameableMfvcNode>?, // used if the getter was custom and need to call it.
    oldGetter: IrSimpleFunction?,
    modality: Modality,
    oldPropertyBackingField: IrField?,
): NameableMfvcNode = if (type.needsMfvcFlattening()) createIntermediateMfvcNode(
    parent,
    context,
    type,
    typeArguments,
    rootPropertyName,
    nameParts,
    fieldAnnotations,
    static,
    overriddenNode as IntermediateMfvcNode?,
    defaultMethodsImplementationSourceNode?.let { (outer, inner) -> outer to (inner as IntermediateMfvcNode) },
    oldGetter,
    modality,
    oldPropertyBackingField,
) else createLeafMfvcNode(
    parent,
    context,
    type,
    rootPropertyName,
    nameParts,
    fieldAnnotations,
    static,
    overriddenNode as LeafMfvcNode?,
    defaultMethodsImplementationSourceNode?.let { (outer, inner) -> outer to (inner as LeafMfvcNode) },
    oldGetter,
    modality,
    oldPropertyBackingField
)

fun createIntermediateMfvcNode(
    parent: IrDeclarationContainer,
    context: JvmBackendContext,
    type: IrSimpleType,
    typeArguments: TypeArguments,
    rootPropertyName: String?,
    nameParts: List<IndexedNamePart>,
    fieldAnnotations: List<IrConstructorCall>,
    static: Boolean,
    overriddenNode: IntermediateMfvcNode?,
    defaultMethodsImplementationSourceNode: Pair<IrSimpleFunction?, IntermediateMfvcNode>?, // used if the getter was custom and need to call it.
    oldGetter: IrSimpleFunction?,
    modality: Modality,
    oldPropertyBackingField: IrField?,
): IntermediateMfvcNode {
    require(type.needsMfvcFlattening()) { "${type.render()} does not require flattening" }
    val valueClass = type.erasedUpperBound
    val representation = valueClass.multiFieldValueClassRepresentation!!

    val replacements = context.multiFieldValueClassReplacements
    val rootNode = replacements.getRootMfvcNode(valueClass)!!

    val oldField = oldGetter?.correspondingPropertySymbol?.owner?.backingField

    val shadowBackingFieldProperty = if (oldField == null) oldGetter?.getGetterField()?.correspondingPropertySymbol?.owner else null
    val useOldGetter = oldGetter != null && (oldField == null || !oldGetter.isDefaultGetter(oldField))

    val subnodes = representation.underlyingPropertyNamesToTypes.mapIndexed { index, (name, type) ->
        val newType = type.substitute(typeArguments) as IrSimpleType
        val newTypeArguments = typeArguments.toMutableMap().apply { putAll(makeTypeArgumentsFromType(newType)) }
        val newDefaultMethodsImplementationSourceNode = when {
            defaultMethodsImplementationSourceNode != null -> {
                val (outer, inner) = defaultMethodsImplementationSourceNode
                require(!useOldGetter) { "Multiple non-default getters:\n\n${outer?.dump()}\n\n${oldGetter?.dump()}" }
                outer to inner[name]!!
            }

            shadowBackingFieldProperty != null -> null to replacements.getMfvcPropertyNode(shadowBackingFieldProperty)!!
            useOldGetter -> oldGetter!! to rootNode[name]!!
            else -> null
        }
        createNameableMfvcNodes(
            parent,
            context,
            newType,
            newTypeArguments,
            rootPropertyName,
            nameParts + IndexedNamePart(index, name),
            fieldAnnotations,
            static,
            overriddenNode?.let { it[name]!! },
            newDefaultMethodsImplementationSourceNode,
            null,
            modality,
            oldPropertyBackingField,
        )
    }

    val fullMethodName = NameableMfvcNodeImpl.makeFullMethodName(rootPropertyName, nameParts)

    val unboxMethod = if (useOldGetter) oldGetter!! else makeUnboxMethod(
        context, fullMethodName, type, parent, overriddenNode, static, defaultMethodsImplementationSourceNode, oldGetter, modality
    ) { receiver ->
        val valueArguments = subnodes.flatMap { it.fields!! }
            .map { field -> irGetField(if (field.isStatic) null else irGet(receiver!!), field) }
        rootNode.makeBoxedExpression(this, typeArguments, valueArguments)
    }

    val hasPureUnboxMethod = defaultMethodsImplementationSourceNode.isPure() && subnodes.all { it.hasPureUnboxMethod }
    return IntermediateMfvcNode(
        type, rootPropertyName, nameParts, subnodes, unboxMethod, hasPureUnboxMethod, rootNode
    )
}

fun collectPropertiesAfterLowering(irClass: IrClass) = LinkedHashSet<IrProperty>().apply {
    for (element in irClass.declarations) {
        if (element is IrField) {
            element.correspondingPropertySymbol?.owner?.let { add(it) }
        } else if (element is IrSimpleFunction && element.extensionReceiverParameter == null && element.contextReceiverParametersCount == 0) {
            element.correspondingPropertySymbol?.owner?.let { add(it) }
        }
    }
}

private fun IrProperty.isStatic(currentContainer: IrDeclarationContainer) =
    getterIfDeclared(currentContainer)?.isStatic ?: backingField?.isStatic ?: error("Property without both getter and backing field")

fun getRootNode(context: JvmBackendContext, mfvc: IrClass): RootMfvcNode {
    require(mfvc.isMultiFieldValueClass) { "${mfvc.defaultType.render()} does not require flattening" }
    val oldPrimaryConstructor = mfvc.primaryConstructor!!
    val representation = mfvc.multiFieldValueClassRepresentation!!
    val properties = collectPropertiesAfterLowering(mfvc).associateBy { it.isStatic(mfvc) to it.name }

    val subnodes = makeRootMfvcNodeSubnodes(representation, properties, context, mfvc)

    val mfvcNodeWithSubnodesImpl = MfvcNodeWithSubnodesImpl(subnodes, null)
    val leaves = mfvcNodeWithSubnodesImpl.leaves
    val fields = mfvcNodeWithSubnodesImpl.fields!!

    val newPrimaryConstructor = makeMfvcPrimaryConstructor(context, oldPrimaryConstructor, mfvc, leaves, fields)
    val primaryConstructorImpl = makePrimaryConstructorImpl(context, oldPrimaryConstructor, mfvc, leaves)
    val boxMethod = makeBoxMethod(context, mfvc, leaves, newPrimaryConstructor)
    val specializedEqualsMethod = makeSpecializedEqualsMethod(context, mfvc, leaves)

    return RootMfvcNode(
        mfvc, subnodes, oldPrimaryConstructor, newPrimaryConstructor, primaryConstructorImpl, boxMethod, specializedEqualsMethod
    )
}

private fun makeSpecializedEqualsMethod(
    context: JvmBackendContext,
    mfvc: IrClass,
    leaves: List<LeafMfvcNode>
) = context.irFactory.buildFun {
    name = InlineClassDescriptorResolver.SPECIALIZED_EQUALS_NAME
    // TODO: Revisit this once we allow user defined equals methods in value classes.
    origin = JvmLoweredDeclarationOrigin.MULTI_FIELD_VALUE_CLASS_GENERATED_IMPL_METHOD
    returnType = context.irBuiltIns.booleanType
}.apply {
    parent = mfvc
    copyTypeParametersFrom(mfvc)
    val typeParametersHalf1 = typeParameters.apply {
        for (it in this) {
            it.name = Name.guessByFirstCharacter("${it.name.asString()}1")
        }
    }

    copyTypeParametersFrom(mfvc)
    val typeParametersHalf2 = typeParameters.drop(typeParametersHalf1.size).apply {
        for (it in this) {
            it.name = Name.guessByFirstCharacter("${it.name.asString()}2")
        }
    }

    val valueParametersHalf1 = leaves.map { leaf ->
        addValueParameter {
            this.name = Name.guessByFirstCharacter(
                "${InlineClassDescriptorResolver.SPECIALIZED_EQUALS_FIRST_PARAMETER_NAME}$${leaf.fullFieldName}"
            )
            this.type = leaf.type.substitute(mfvc.typeParameters, typeParametersHalf1.map { it.defaultType })
        }
    }

    val valueParametersHalf2 = leaves.map { leaf ->
        addValueParameter {
            this.name = Name.guessByFirstCharacter(
                "${InlineClassDescriptorResolver.SPECIALIZED_EQUALS_SECOND_PARAMETER_NAME}$${leaf.fullFieldName}"
            )
            this.type = leaf.type.substitute(mfvc.typeParameters, typeParametersHalf2.map { it.defaultType })
        }
    }

    body = with(context.createJvmIrBuilder(this.symbol)) {
        val conjunctions = valueParametersHalf1.zip(valueParametersHalf2) { f1, f2 -> irEquals(irGet(f1), irGet(f2)) }
        irExprBody(conjunctions.reduce { acc, current ->
            irCall(context.irBuiltIns.andandSymbol).apply {
                putValueArgument(0, acc)
                putValueArgument(1, current)
            }
        })
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
    body = with(context.createJvmIrBuilder(this.symbol)) {
        irExprBody(irCall(newPrimaryConstructor).apply {
            for ((index, parameter) in parameters.withIndex()) {
                putValueArgument(index, irGet(parameter))
            }
        })
    }
}

private fun makePrimaryConstructorImpl(
    context: JvmBackendContext,
    oldPrimaryConstructor: IrConstructor,
    mfvc: IrClass,
    leaves: List<LeafMfvcNode>
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
    // body is added in the Lowering file as it needs to be lowered
}

private fun makeMfvcPrimaryConstructor(
    context: JvmBackendContext,
    oldPrimaryConstructor: IrConstructor,
    mfvc: IrClass,
    leaves: List<LeafMfvcNode>,
    fields: List<IrField>
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
    body = context.createIrBuilder(irConstructor.symbol).irBlockBody(irConstructor) {
        +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
        for ((field, parameter) in fields zip parameters) {
            +irSetField(irGet(mfvc.thisReceiver!!), field, irGet(parameter))
        }
    }
}

private fun makeRootMfvcNodeSubnodes(
    representation: MultiFieldValueClassRepresentation<IrSimpleType>,
    properties: Map<Pair<Boolean, Name>, IrProperty>,
    context: JvmBackendContext,
    mfvc: IrClass
) = representation.underlyingPropertyNamesToTypes.mapIndexed { index, (name, type) ->
    val typeArguments = makeTypeArgumentsFromType(type)
    val oldProperty = properties[false to name]!!
    val oldBackingField = oldProperty.backingField
    val oldGetter = oldProperty.getterIfDeclared(mfvc)
    val overriddenNode = oldGetter?.let { getOverriddenNode(context.multiFieldValueClassReplacements, it) as IntermediateMfvcNode? }
    val static = oldProperty.isStatic(mfvc)
    createNameableMfvcNodes(
        mfvc,
        context,
        type,
        typeArguments,
        null,
        listOf(IndexedNamePart(index, name)),
        oldBackingField?.annotations ?: listOf(),
        static,
        overriddenNode,
        null,
        oldGetter.takeIf { static },
        Modality.FINAL,
        oldBackingField,
    ).also {
        updateAnnotationsAndPropertyFromOldProperty(oldProperty)
        it.unboxMethod.overriddenSymbols = listOf() // the getter is saved so it overrides itself
    }
}

private fun updateAnnotationsAndPropertyFromOldProperty(
    oldProperty: IrProperty
) {
    oldProperty.setter?.apply {
        name = Name.identifier(JvmAbi.setterName(oldProperty.name.asString()))
        correspondingPropertySymbol = null
        origin = IrDeclarationOrigin.DEFINED
    }
    oldProperty.setter = null
    oldProperty.backingField = null
}

fun createIntermediateNodeForMfvcPropertyOfRegularClass(
    parent: IrDeclarationContainer,
    context: JvmBackendContext,
    oldProperty: IrProperty,
): IntermediateMfvcNode {
    val oldGetter = oldProperty.getterIfDeclared(parent)
    val oldField = oldProperty.backingField
    val type = oldProperty.getter?.returnType ?: oldField?.type ?: error("Either getter or field must exist")
    require(type is IrSimpleType && type.needsMfvcFlattening()) { "Expected MFVC but got ${type.render()}" }
    val fieldAnnotations = oldField?.annotations ?: listOf()
    val static = oldProperty.isStatic(parent)
    val overriddenNode = oldGetter?.let { getOverriddenNode(context.multiFieldValueClassReplacements, it) as IntermediateMfvcNode? }
    val modality = if (oldGetter == null || oldGetter.modality == Modality.FINAL) Modality.FINAL else oldGetter.modality
    return createIntermediateMfvcNode(
        parent, context, type, makeTypeArgumentsFromType(type), oldProperty.name.asString(), listOf(),
        fieldAnnotations, static, overriddenNode, null, oldGetter, modality, oldField
    ).also {
        updateAnnotationsAndPropertyFromOldProperty(oldProperty)
    }
}

private fun getOverriddenNode(replacements: MemoizedMultiFieldValueClassReplacements, getter: IrSimpleFunction): NameableMfvcNode? =
    getter.overriddenSymbols
        .firstOrNull { !it.owner.isFakeOverride }
        ?.let { replacements.getMfvcPropertyNode(it.owner.correspondingPropertySymbol!!.owner) }

fun getOptimizedPublicAccess(currentElement: IrElement?, parent: IrClass): AccessType {
    val declaration = currentElement as? IrDeclaration ?: return AccessType.AlwaysPublic
    for (cur in declaration.parents.filterIsInstance<IrClass>()) {
        return when {
            cur == parent -> AccessType.PrivateWhenNoBox
            cur.isInner -> continue
            cur.isCompanion -> continue
            else -> AccessType.AlwaysPublic
        }
    }
    return AccessType.AlwaysPublic
}

private fun IrProperty.getterIfDeclared(parent: IrDeclarationContainer): IrSimpleFunction? = getter?.takeIf { it in parent.declarations }
