/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createDispatchReceiverParameter
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.jvm.MultiFieldValueClassTree.InternalNode
import org.jetbrains.kotlin.backend.jvm.MultiFieldValueClassTree.Leaf
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.backend.jvm.ir.isMultiFieldValueClassType
import org.jetbrains.kotlin.backend.jvm.ir.upperBound
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.MultiFieldValueClassRepresentation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.InlineClassDescriptorResolver

sealed class MultiFieldValueClassTree {
    abstract val type: IrType
    val irClass: IrClass
        get() = type.erasedUpperBound

    companion object {
        @JvmStatic
        fun create(type: IrSimpleType, replacements: MemoizedMultiFieldValueClassReplacements) =
            if (!type.isNullable() && type.isMultiFieldValueClassType()) InternalNode(type, DescriptorVisibilities.PUBLIC, replacements)
            else Leaf(type)
    }

    class Leaf(override val type: IrType) : MultiFieldValueClassTree() {
        init {
            require(type.isNullable() || !type.isMultiFieldValueClassType())
        }
    }

    class InternalNode internal constructor(
        override val type: IrSimpleType, val fields: List<TreeField>
    ) : MultiFieldValueClassTree() {
        constructor(
            type: IrSimpleType, visibility: DescriptorVisibility,
            replacements: MemoizedMultiFieldValueClassReplacements
        ) : this(type, 0.let {
            val valueClassRepresentation = type.erasedUpperBound.valueClassRepresentation as MultiFieldValueClassRepresentation
            val primaryConstructor = type.erasedUpperBound.primaryConstructor!!
            val propertiesByName = replacements.getOldMFVCProperties(primaryConstructor.constructedClass).associateBy { it.name }
            val typeArguments: List<IrTypeArgument> = type.arguments
            val typeParameters = type.erasedUpperBound.typeParameters
            val substitutionMap = typeParameters.map { it.symbol }.zip(typeArguments)
                .mapNotNull { (param, arg) -> if (arg is IrSimpleType) param to arg else null }.toMap()
            valueClassRepresentation.underlyingPropertyNamesToTypes.map { (name, representationType) ->
                val property = propertiesByName[name]!!
                val innerVisibility = property.visibility
                val comparison = visibility.compareTo(innerVisibility)
                    ?: error("Expected comparable visibilities but got $visibility and $innerVisibility")
                val newVisibility = if (comparison < 0) visibility else innerVisibility
                TreeField(
                    name,
                    representationType.substitute(substitutionMap) as IrSimpleType,
                    newVisibility,
                    property.annotations,
                    replacements
                )
            }
        })

        class TreeField(
            val name: Name, val type: IrType, val visibility: DescriptorVisibility, val annotations: List<IrConstructorCall>,
            val node: MultiFieldValueClassTree
        ) {
            constructor(
                name: Name,
                type: IrSimpleType,
                visibility: DescriptorVisibility,
                annotations: List<IrConstructorCall>,
                replacements: MemoizedMultiFieldValueClassReplacements
            ) : this(name, type, visibility, annotations, create(type, replacements))
        }

        // todo store real fields, cannot restore them
        // todo or store fields not pairs

        init {
            require(!type.isNullable() && type.isMultiFieldValueClassType())
        }

        private val fieldByName = fields.associateBy { it.name }

        operator fun get(name: Name) = fieldByName[name]
    }
}

fun MultiFieldValueClassTree.render(): String = type.render() + when (this) {
    is InternalNode -> "\n" + fields.joinToString("\n") { "${it.name.render()}: ${it.node.render().prependIndent("  ")}" }
    is Leaf -> ""
}

class MultiFieldValueClassSpecificDeclarations(
    val valueClass: IrClass,
    private val typeSystemContext: IrTypeSystemContext,
    private val irFactory: IrFactory,
    private val context: JvmBackendContext,
    private val replacements: MemoizedMultiFieldValueClassReplacements,
) {
    init {
        require(valueClass.isMultiFieldValueClass) { "Cannot build ${this::class.simpleName} for not multi-field value class: $valueClass" }
    }

    val loweringRepresentation = MultiFieldValueClassTree.create(valueClass.defaultType, replacements) as InternalNode

    val leaves: List<Leaf> = ArrayList<Leaf>().apply {
        fun proceed(node: MultiFieldValueClassTree) {
            when (node) {
                is InternalNode -> node.fields.forEach { proceed(it.node) }
                is Leaf -> add(node)
            }
        }
        proceed(loweringRepresentation)
        if (size <= 1) {
            error("${this@MultiFieldValueClassSpecificDeclarations::class.qualifiedName} for $valueClass must have multiple leaves")
        }
    }

    val nodeFullNames: Map<MultiFieldValueClassTree, Name> =
        mutableMapOf<MultiFieldValueClassTree, Name>().apply {
            val parts = mutableListOf<String>()
            fun makeName() = Name.guessByFirstCharacter(parts.joinToString("$"))
            fun proceed(node: MultiFieldValueClassTree) {
                when (node) {
                    is InternalNode -> {
                        if (parts.isNotEmpty()) {
                            put(node, makeName())
                        }
                        for (field in node.fields) {
                            parts.add(field.name.asString())
                            proceed(field.node)
                            parts.removeLast()
                        }
                    }
                    is Leaf -> put(node, makeName())
                }
            }
            proceed(loweringRepresentation)
        }

    init {
        require(nodeFullNames.size == nodeFullNames.values.distinct().size) { "Ambiguous names found: ${nodeFullNames.values}" }
    }

    private val indexByLeaf = leaves.withIndex().associate { it.value to it.index }
    private val indexesByInternalNode = mutableMapOf<InternalNode, IntRange>().apply {
        var index = 0
        fun proceed(node: MultiFieldValueClassTree) {
            when (node) {
                is InternalNode -> {
                    val start = index
                    node.fields.forEach { proceed(it.node) }
                    val finish = index
                    val range = start until finish
                    if (finish - start <= 1) {
                        error("Invalid multi-field value class indexes range for class $valueClass: ${range}")
                    }
                    put(node, range)
                }
                is Leaf -> index++
            }
        }
        proceed(loweringRepresentation)
    }

    val oldPrimaryConstructor = valueClass.primaryConstructor ?: error("Value classes have primary constructors")

    val oldProperties = replacements.getOldMFVCProperties(valueClass).associateBy { it.name }

    val fields = leaves.map { leaf ->
        irFactory.buildField {
            this.name = nodeFullNames[leaf]!!
            this.type = leaf.type
            visibility = DescriptorVisibilities.PRIVATE
        }.apply {
            parent = valueClass
        }
    }

    private val gettersVisibilities = mutableMapOf<MultiFieldValueClassTree, DescriptorVisibility>().apply {
        val stack = mutableListOf<DescriptorVisibility>()
        fun proceed(node: MultiFieldValueClassTree) {
            when (node) {
                is InternalNode -> {
                    if (stack.isNotEmpty()) {
                        put(node, stack.last())
                    }
                    for (field in node.fields) {
                        stack.add(field.visibility)
                        proceed(field.node)
                        stack.pop()
                    }
                }
                is Leaf -> put(node, stack.last())
            }
        }
        proceed(loweringRepresentation)
    }

    private val gettersAnnotations = mutableMapOf<MultiFieldValueClassTree, List<IrConstructorCall>>().apply {
        val stack = mutableListOf<List<IrConstructorCall>>()

        fun proceed(node: MultiFieldValueClassTree) {
            when (node) {
                is InternalNode -> {
                    if (stack.isNotEmpty()) {
                        put(node, stack.last())
                    }
                    for (field in node.fields) {
                        stack.add(field.annotations)
                        proceed(field.node)
                        stack.pop()
                    }
                }
                is Leaf -> put(node, stack.last())
            }
        }
        proceed(loweringRepresentation)
    }

    private fun IrBuilderWithScope.fieldGetter(receiver: IrValueParameter, field: IrField): IrGetField =
        irGetField(irGet(receiver), field)

    private fun IrFunction.fieldGetter(field: IrField): IrBuilderWithScope.() -> IrGetField =
        { fieldGetter(dispatchReceiverParameter!!, field) }

    val primaryConstructor: IrConstructor = irFactory.buildConstructor {
        updateFrom(oldPrimaryConstructor)
        visibility = DescriptorVisibilities.PRIVATE
        origin = JvmLoweredDeclarationOrigin.SYNTHETIC_MULTI_FIELD_VALUE_CLASS_MEMBER
        returnType = oldPrimaryConstructor.returnType
    }.apply {
        require(oldPrimaryConstructor.typeParameters.isEmpty()) {
            "Constructors do not support type parameters yet"
        }
        addFlattenedClassRepresentationToParameters(mapOf())
        val irConstructor = this@apply
        parent = valueClass
        body = context.createIrBuilder(irConstructor.symbol).irBlockBody(irConstructor) {
            +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
            for (i in leaves.indices) {
                +irSetField(
                    receiver = irGet(valueClass.thisReceiver!!),
                    field = fields[i],
                    value = irGet(irConstructor.valueParameters[i])
                )
            }
        }
    }

    val primaryConstructorImpl: IrSimpleFunction = irFactory.buildFun {
        name = InlineClassAbi.mangledNameFor(oldPrimaryConstructor, false, false)
        visibility = oldPrimaryConstructor.visibility
        origin = JvmLoweredDeclarationOrigin.STATIC_MULTI_FIELD_VALUE_CLASS_CONSTRUCTOR
        returnType = context.irBuiltIns.unitType
        modality = Modality.FINAL
    }.apply {
        parent = valueClass
        copyTypeParametersFrom(valueClass)
        addFlattenedClassRepresentationToParameters(classToFunctionTypeParametersMapping())
        // body is added in Lowering file
    }

    private fun IrSimpleFunction.classToFunctionTypeParametersMapping() =
        valueClass.typeParameters.map { it.symbol }.zip(typeParameters.map { it.defaultType }).toMap()

    val boxMethod = irFactory.buildFun {
        name = Name.identifier(KotlinTypeMapper.BOX_JVM_METHOD_NAME)
        origin = JvmLoweredDeclarationOrigin.SYNTHETIC_MULTI_FIELD_VALUE_CLASS_MEMBER
        returnType = valueClass.defaultType
    }.apply {
        parent = valueClass
        copyTypeParametersFrom(valueClass)
        addFlattenedClassRepresentationToParameters(classToFunctionTypeParametersMapping())
        // body is added in Lowering file
    }

    private fun IrFunction.addFlattenedClassRepresentationToParameters(substitutionMap: Map<IrTypeParameterSymbol, IrType>) {
        for (leaf in leaves) {
            addValueParameter {
                this.name = nodeFullNames[leaf]!!
                this.type = leaf.type.substitute(substitutionMap)
            }
        }
    }

    val specializedEqualsMethod = irFactory.buildFun {
        name = InlineClassDescriptorResolver.SPECIALIZED_EQUALS_NAME
        // TODO: Revisit this once we allow user defined equals methods in value classes.
        origin = JvmLoweredDeclarationOrigin.MULTI_FIELD_VALUE_CLASS_GENERATED_IMPL_METHOD
        returnType = context.irBuiltIns.booleanType
    }.apply {
        parent = valueClass
        copyTypeParametersFrom(valueClass)
        val typeParametersHalf1 = typeParameters.apply {
            for (it in this) {
                it.name = Name.guessByFirstCharacter("${it.name.asString()}1")
            }
        }
        val substitutionMapHalf1 = (valueClass.typeParameters.map { it.symbol } zip typeParametersHalf1.map { it.defaultType }).toMap()
        copyTypeParametersFrom(valueClass)
        val typeParametersHalf2 = typeParameters.drop(typeParametersHalf1.size).apply {
            for (it in this) {
                it.name = Name.guessByFirstCharacter("${it.name.asString()}2")
            }
        }
        val substitutionMapHalf2 = (valueClass.typeParameters.map { it.symbol } zip typeParametersHalf2.map { it.defaultType }).toMap()
        for (leaf in leaves) {
            addValueParameter {
                this.name = Name.guessByFirstCharacter(
                    "${InlineClassDescriptorResolver.SPECIALIZED_EQUALS_FIRST_PARAMETER_NAME}$${nodeFullNames[leaf]!!.asString()}"
                )
                this.type = leaf.type.substitute(substitutionMapHalf1)
            }
        }
        for (leaf in leaves) {
            addValueParameter {
                this.name = Name.guessByFirstCharacter(
                    "${InlineClassDescriptorResolver.SPECIALIZED_EQUALS_SECOND_PARAMETER_NAME}$${nodeFullNames[leaf]!!.asString()}"
                )
                this.type = leaf.type.substitute(substitutionMapHalf2)
            }
        }
        // body is added in Lowering file
    }

    val unboxMethods = fields.mapIndexed { index: Int, field: IrField ->
        irFactory.buildFun {
            name = Name.identifier(KotlinTypeMapper.UNBOX_JVM_METHOD_NAME + index)
            origin = JvmLoweredDeclarationOrigin.SYNTHETIC_MULTI_FIELD_VALUE_CLASS_MEMBER
            returnType = field.type
        }.apply {
            parent = valueClass
            createDispatchReceiverParameter()
            body = with(context.createIrBuilder(this.symbol)) {
                irExprBody(fieldGetter(field)())
            }
        }
    }

    val properties: Map<MultiFieldValueClassTree, IrProperty> = run {
        val nodes2expressions: Map<MultiFieldValueClassTree, IrBuilderWithScope.(function: IrFunction) -> IrExpression> =
            makeNodes2FieldExpressions(fields)
        nodeFullNames.mapValues { (node, propertyName) ->
            val overrideable = oldProperties[propertyName]
            irFactory.buildProperty {
                name = propertyName
                origin = IrDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER
                visibility = gettersVisibilities[node]!!
            }.apply {
                annotations = gettersAnnotations[node]!!
                overrideable?.overriddenSymbols?.let { overriddenSymbols = it }
                parent = valueClass
                addGetter {
                    returnType = node.type
                    origin = IrDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER
                }.apply {
                    val function = this
                    overrideable?.getter?.overriddenSymbols?.let { overriddenSymbols = it }
                    createDispatchReceiverParameter()
                    body = with(context.createIrBuilder(this.symbol)) {
                        irExprBody(nodes2expressions[node]!!.invoke(this, function))
                    }
                }
            }
        }
    }

    companion object {
        private fun IrFunctionAccessExpression.copyTypeArgumentsFromType(
            type: IrSimpleType, valueClass: IrClass, substitutionMap: Map<IrTypeParameterSymbol, IrType>
        ) {
            require(type.erasedUpperBound == valueClass) {
                "Illegal bound ${type.erasedUpperBound.render()} instead of ${valueClass.render()}"
            }
            require(type.arguments.size == valueClass.typeParameters.size) {
                "Unexpected number of type arguments: ${type.arguments.size} for ${valueClass.typeParameters.size} parameters"
            }
            require(type.arguments.size == symbol.owner.typeParameters.size) {
                "Unexpected number of type arguments: ${type.arguments.size} for ${symbol.owner.typeParameters.size} parameters"
            }
            for (i in valueClass.typeParameters.indices) {
                putTypeArgument(i, type.arguments[i].typeOrNull?.substitute(substitutionMap))
            }
        }
    }

    fun makeNodes2FieldExpressions(fields: List<IrField>): Map<MultiFieldValueClassTree, IrBuilderWithScope.(IrFunction) -> IrExpression> {
        fun proceed(node: MultiFieldValueClassTree): Map<MultiFieldValueClassTree, IrBuilderWithScope.(function: IrFunction) -> IrExpression> =
            when (node) {
                is InternalNode -> buildMap {
                    val innerDeclarations =
                        if (node.irClass == valueClass) this@MultiFieldValueClassSpecificDeclarations
                        else replacements.getDeclarations(node.irClass)!!
                    put(node) { function ->
                        irCall(innerDeclarations.boxMethod).apply {
                            copyTypeArgumentsFromType(node.type, node.irClass, mapOf())
                            indexesByInternalNode[node]!!.forEachIndexed { index, nodeIndex ->
                                putValueArgument(index, fieldGetter(function.dispatchReceiverParameter!!, fields[nodeIndex]))
                            }
                        }
                    }
                    node.fields.forEach { putAll(proceed(it.node)) }
                }
                is Leaf -> mapOf(node to { fieldGetter(it.dispatchReceiverParameter!!, fields[indexByLeaf[node]!!]) })
            }
        return proceed(loweringRepresentation)
    }

    data class VirtualProperty(
        val type: IrType,
        val makeGetter: ExpressionGenerator,
        val assigner: ExpressionSupplier?,
        val symbol: IrValueSymbol?
    ) {
        constructor(declaration: IrValueDeclaration) : this(
            declaration.type,
            { irGet(declaration) },
            if (declaration.isAssignable) { value -> irSet(declaration, value) } else null,
            declaration.symbol,
        )

        val isAssignable: Boolean
            get() = assigner != null
    }

    /**
     * Multi-field value class instance can be not only a standalone instance but also:
     *   1. A slice of fields in another multi-field class instance;
     *   2. A slice of fields in another regular class instance;
     *   3. Virtual instance constructed with local variables;
     *   4. Virtual instance constructed with local parameters;
     *   5. Virtual instance constructed with local fields;
     *   6. Some mix of the above ones.
     */
    inner class ImplementationAgnostic constructor(val type: IrSimpleType, val virtualFields: List<VirtualProperty>) {
        val regularDeclarations = this@MultiFieldValueClassSpecificDeclarations
        val symbols = virtualFields.map { it.symbol }
        private val substitutionMap = valueClass.typeParameters.map { it.symbol }.zip(type.arguments.map { it.typeOrNull })
            .mapNotNull { (parameter, type) -> if (type != null) parameter to type else null }.toMap()

        init {
            require(virtualFields.size == leaves.size) { "Wrong symbols number given for $leaves, got $virtualFields" }
            for ((actual, expected) in virtualFields.map { it.type } zip leaves.map { it.type }) {
                require(actual.upperBound.isSubtypeOf(expected.upperBound, typeSystemContext)) {
                    "${actual.render()} is not a subtype of ${expected.render()} for MFVC ${valueClass.render()}"
                }
            }
        }

        private val nodeToSymbols = indexesByInternalNode.mapValues { (_, indexes) -> virtualFields.slice(indexes) } +
                indexByLeaf.mapValues { (_, index) -> listOf(virtualFields[index]) }

        private val internalNodeToExpressionGetters: Map<InternalNode, ExpressionGenerator> =
            indexesByInternalNode.mapValues { (node, indexes) ->
                {
                    val arguments = virtualFields.slice(indexes).map { it.makeGetter(this) }
                    val innerDeclarations = replacements.getDeclarations(node.irClass)!!
                    irCall(innerDeclarations.boxMethod).apply {
                        copyTypeArgumentsFromType(node.type, node.irClass, substitutionMap)
                        for (i in arguments.indices) {
                            putValueArgument(i, arguments[i])
                        }
                    }
                }
            }

        private val leavesToExpressionGetters: Map<Leaf, ExpressionGenerator> =
            indexByLeaf.mapValues { (_, index) -> { virtualFields[index].makeGetter(this) } }

        val nodeToExpressionGetters: Map<MultiFieldValueClassTree, ExpressionGenerator> =
            internalNodeToExpressionGetters + leavesToExpressionGetters

        operator fun get(name: Name): Pair<ExpressionGenerator, ImplementationAgnostic?>? =
            when (val field = loweringRepresentation[name]) {
                null -> null
                else -> nodeToExpressionGetters[field.node]!! to when (field.node) {
                    is Leaf -> null
                    is InternalNode -> {
                        val irClass = field.node.irClass
                        val declarations = replacements.getDeclarations(irClass)!!
                        declarations.ImplementationAgnostic(field.type as IrSimpleType, nodeToSymbols[field.node]!!)
                    }
                }
            }

        val boxedExpression = nodeToExpressionGetters[loweringRepresentation]!!

        // todo default parameters
        // todo annotations etc.
    }
}

typealias ExpressionGenerator = IrBuilderWithScope.() -> IrExpression
typealias ExpressionSupplier = IrBuilderWithScope.(IrExpression) -> IrStatement