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
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.InlineClassDescriptorResolver

sealed class MultiFieldValueClassTree<out TI, out TL> {
    abstract val type: IrType
    val irClass: IrClass?
        get() = type.getClass()

    companion object {
        @JvmStatic
        fun <TI, TL> create(
            type: IrType,
            defaultInternalNodeValue: TI,
            defaultLeafValue: TL,
            replacements: MemoizedMultiFieldValueClassReplacements
        ) =
            if (!type.isNullable() && type.isMultiFieldValueClassType())
                InternalNode(type, defaultInternalNodeValue, DescriptorVisibilities.PUBLIC, defaultLeafValue, replacements)
            else
                Leaf(type, defaultLeafValue)

        @JvmStatic
        fun create(type: IrType, replacements: MemoizedMultiFieldValueClassReplacements) = create(type, Unit, Unit, replacements)
    }

    data class Leaf<out TL>(override val type: IrType, val leafValue: TL) : MultiFieldValueClassTree<Nothing, TL>() {
        init {
            require(type.isNullable() || !type.isMultiFieldValueClassType())
        }

        fun <RL> map(transformLeaf: Leaf<TL>.(TL) -> RL) = Leaf(type, transformLeaf(leafValue))
    }

    data class InternalNode<out TI, out TL> internal constructor(
        override val type: IrType, val internalNodeValue: TI, val fields: List<TreeField<TI, TL>>
    ) : MultiFieldValueClassTree<TI, TL>() {
        constructor(
            type: IrType, defaultInternalNodeValue: TI, visibility: DescriptorVisibility, defaultLeafValue: TL,
            replacements: MemoizedMultiFieldValueClassReplacements
        ) : this(type, defaultInternalNodeValue, 0.let {
            val valueClassRepresentation = type.erasedUpperBound.valueClassRepresentation as MultiFieldValueClassRepresentation
            val primaryConstructor = type.erasedUpperBound.primaryConstructor!!
            val fieldsByName = replacements.getOldFields(primaryConstructor.constructedClass).associateBy { it.name }
            valueClassRepresentation.underlyingPropertyNamesToTypes.map { (name, type) ->
                val field = fieldsByName[name]!!
                val innerVisibility = field.correspondingPropertySymbol!!.owner.visibility
                val comparison = visibility.compareTo(innerVisibility)
                    ?: error("Expected comparable visibilities but got $visibility and $innerVisibility")
                val newVisibility = if (comparison < 0) visibility else innerVisibility
                TreeField(name, type, defaultInternalNodeValue, newVisibility, field.annotations, defaultLeafValue, replacements)
            }
        })

        data class TreeField<out TI, out TL>(
            val name: Name, val type: IrType, val visibility: DescriptorVisibility, val annotations: List<IrConstructorCall>,
            val node: MultiFieldValueClassTree<TI, TL>
        ) {
            constructor(
                name: Name,
                type: IrType,
                internalNodeValue: TI,
                visibility: DescriptorVisibility,
                annotations: List<IrConstructorCall>,
                defaultLeafValue: TL,
                replacements: MemoizedMultiFieldValueClassReplacements
            ) : this(name, type, visibility, annotations, create(type, internalNodeValue, defaultLeafValue, replacements))

            fun <RI, RL> map(
                transformInternalNode: InternalNode<TI, TL>.(TI) -> RI,
                transformLeaf: Leaf<TL>.(TL) -> RL
            ): TreeField<RI, RL> = TreeField(name, type, visibility, annotations, node.map(transformInternalNode, transformLeaf))
        }

        // todo store real fields, cannot restore them
        // todo or store fields not pairs

        init {
            require(!type.isNullable() && type.isMultiFieldValueClassType())
        }

        private val fieldByName = fields.associateBy { it.name }

        operator fun get(name: Name) = fieldByName[name]

        override fun <RI, RL> map(
            transformInternalNode: InternalNode<TI, TL>.(TI) -> RI,
            transformLeaf: Leaf<TL>.(TL) -> RL
        ): InternalNode<RI, RL> = InternalNode(
            type = type,
            internalNodeValue = transformInternalNode(internalNodeValue),
            fields = fields.map { it.map(transformInternalNode, transformLeaf) },
        )

    }

    interface Visitor<out T, in TL, in TI> {
        fun visitLeaf(treeNode: Leaf<TL>): T
        fun visitInternalNode(treeNode: InternalNode<TI, TL>): T
    }

    fun <T> visit(visitor: Visitor<T, TL, TI>) = when (this) {
        is InternalNode -> visitor.visitInternalNode(this)
        is Leaf -> visitor.visitLeaf(this)
    }

    open fun <RI, RL> map(
        transformInternalNode: InternalNode<TI, TL>.(TI) -> RI,
        transformLeaf: Leaf<TL>.(TL) -> RL
    ): MultiFieldValueClassTree<RI, RL> = when (this) {
        is InternalNode -> map(transformInternalNode, transformLeaf)
        is Leaf -> map(transformLeaf)
    }
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

    val leaves: List<Leaf<Unit>> = ArrayList<Leaf<Unit>>().apply {
        loweringRepresentation.visit(object : MultiFieldValueClassTree.Visitor<Unit, Unit, Unit> {
            override fun visitLeaf(treeNode: Leaf<Unit>) {
                add(treeNode)
            }

            override fun visitInternalNode(treeNode: InternalNode<Unit, Unit>) {
                treeNode.fields.forEach { it.node.visit(this) }
            }
        })
    }.also { if (it.size <= 1) error("${this::class.qualifiedName} for $valueClass must have multiple leaves") }

    val nodeFullNames: Map<MultiFieldValueClassTree<Unit, Unit>, Name> =
        mutableMapOf<MultiFieldValueClassTree<Unit, Unit>, Name>().apply {
            loweringRepresentation.visit(object : MultiFieldValueClassTree.Visitor<Unit, Unit, Unit> {
                val parts = mutableListOf<String>()
                override fun visitLeaf(treeNode: Leaf<Unit>) {
                    put(treeNode, makeName())
                }

                override fun visitInternalNode(treeNode: InternalNode<Unit, Unit>) {
                    if (parts.isNotEmpty()) {
                        put(treeNode, makeName())
                    }
                    for (field in treeNode.fields) {
                        parts.add(field.name.asString())
                        field.node.visit(this)
                        parts.removeLast()
                    }
                }

                private fun makeName() = Name.guessByFirstCharacter(parts.joinToString("$"))
            })
        }
    
    init {
        require(nodeFullNames.size == nodeFullNames.values.distinct().size) { "Ambiguous names found: ${nodeFullNames.values}" }
    }

    private val indexByLeaf = leaves.withIndex().associate { it.value to it.index }
    private val indexesByInternalNode = mutableMapOf<InternalNode<Unit, Unit>, IntRange>().apply {
        var index = 0
        loweringRepresentation.visit(object : MultiFieldValueClassTree.Visitor<Unit, Unit, Unit> {
            override fun visitLeaf(treeNode: Leaf<Unit>) {
                index++
            }

            override fun visitInternalNode(treeNode: InternalNode<Unit, Unit>) {
                val start = index
                treeNode.fields.forEach { it.node.visit(this) }
                val finish = index
                val range = start until finish
                if (finish - start <= 1) {
                    error("Invalid multi-field value class indexes range for class $valueClass: ${range}")
                }
                put(treeNode, range)
            }
        })
    }

    val oldPrimaryConstructor = valueClass.primaryConstructor ?: error("Value classes have primary constructors")

    val fields = leaves.map { leaf ->
        irFactory.buildField {
            this.name = nodeFullNames[leaf]!!
            this.type = leaf.type
            visibility = DescriptorVisibilities.PRIVATE
        }
    }

    private val gettersVisibilities = mutableMapOf<MultiFieldValueClassTree<Unit, Unit>, DescriptorVisibility>().apply {
        loweringRepresentation.visit(object : MultiFieldValueClassTree.Visitor<Unit, Unit, Unit> {
            val stack = mutableListOf<DescriptorVisibility>()
            override fun visitLeaf(treeNode: Leaf<Unit>) {
                put(treeNode, stack.last())
            }

            override fun visitInternalNode(treeNode: InternalNode<Unit, Unit>) {
                if (stack.isNotEmpty()) {
                    put(treeNode, stack.last())
                }
                for (field in treeNode.fields) {
                    stack.add(field.visibility)
                    field.node.visit(this)
                    stack.pop()
                }
            }
        })
    }

    private val gettersAnnotations = mutableMapOf<MultiFieldValueClassTree<Unit, Unit>, List<IrConstructorCall>>().apply {
        loweringRepresentation.visit(object : MultiFieldValueClassTree.Visitor<Unit, Unit, Unit> {
            val stack = mutableListOf<List<IrConstructorCall>>()
            override fun visitLeaf(treeNode: Leaf<Unit>) {
                put(treeNode, stack.last())
            }

            override fun visitInternalNode(treeNode: InternalNode<Unit, Unit>) {
                if (stack.isNotEmpty()) {
                    put(treeNode, stack.last())
                }
                for (field in treeNode.fields) {
                    stack.add(field.annotations)
                    field.node.visit(this)
                    stack.pop()
                }
            }
        })
    }

    fun IrBuilderWithScope.fieldGetter(receiver: IrValueParameter, field: IrField): IrGetField =
        irGetField(irGet(receiver), field)

    fun IrFunction.fieldGetter(field: IrField): IrBuilderWithScope.() -> IrGetField = { fieldGetter(dispatchReceiverParameter!!, field) }

    val selfImplementationAgnosticDeclarations = ImplementationAgnostic(
        fields.map {
            VirtualProperty(
                type = it.type,
                makeGetter = { receiver: IrValueParameter -> irGetField(irGet(receiver), it) },
                assigner = { receiver: IrValueParameter, value: IrExpression -> irSetField(irGet(receiver), it, value) },
            )
        }
    )

    val primaryConstructor: IrConstructor = irFactory.buildConstructor {
        updateFrom(oldPrimaryConstructor)
        visibility = DescriptorVisibilities.PRIVATE
        origin = JvmLoweredDeclarationOrigin.SYNTHETIC_MULTI_FIELD_VALUE_CLASS_MEMBER
        returnType = oldPrimaryConstructor.returnType
    }.apply {
        copyTypeParametersFrom(oldPrimaryConstructor)
        addFlattenedClassRepresentationToParameters()
        val irConstructor = this@apply
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
        copyTypeParametersFrom(oldPrimaryConstructor)
        addFlattenedClassRepresentationToParameters()
        // body is added in Lowering file
    }

    val boxMethod = irFactory.buildFun {
        name = Name.identifier(KotlinTypeMapper.BOX_JVM_METHOD_NAME)
        origin = JvmLoweredDeclarationOrigin.SYNTHETIC_MULTI_FIELD_VALUE_CLASS_MEMBER
        returnType = valueClass.defaultType
    }.apply {
        copyTypeParametersFrom(valueClass)
        addFlattenedClassRepresentationToParameters()
        // body is added in Lowering file
    }

    private fun IrFunction.addFlattenedClassRepresentationToParameters() {
        for (leaf in leaves) {
            addValueParameter {
                this.name = nodeFullNames[leaf]!!
                this.type = leaf.type
            }
        }
    }

    val specializedEqualsMethod = irFactory.buildFun {
        name = InlineClassDescriptorResolver.SPECIALIZED_EQUALS_NAME
        // TODO: Revisit this once we allow user defined equals methods in inline classes.
        origin = JvmLoweredDeclarationOrigin.MULTI_FIELD_VALUE_CLASS_GENERATED_IMPL_METHOD
        returnType = context.irBuiltIns.booleanType
    }.apply {
        copyTypeParametersFrom(oldPrimaryConstructor)
        for (leaf in leaves) {
            addValueParameter {
                this.name = Name.guessByFirstCharacter(
                    "${InlineClassDescriptorResolver.SPECIALIZED_EQUALS_FIRST_PARAMETER_NAME}$${nodeFullNames[leaf]!!.asString()}"
                )
                this.type = leaf.type
            }
        }
        for (leaf in leaves) {
            addValueParameter {
                this.name = Name.guessByFirstCharacter(
                    "${InlineClassDescriptorResolver.SPECIALIZED_EQUALS_SECOND_PARAMETER_NAME}$${nodeFullNames[leaf]!!.asString()}"
                )
                this.type = leaf.type
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

    val properties: Map<MultiFieldValueClassTree<Unit, Unit>, IrProperty> =
        selfImplementationAgnosticDeclarations.nodeToExpressionGetters.filterKeys { it in nodeFullNames }.mapValues { (node, getter) ->
            irFactory.buildProperty {
                name = nodeFullNames[node]!!
                origin = IrDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER
                visibility = gettersVisibilities[node]!!
            }.apply {
                annotations = gettersAnnotations[node]!!
                parent = valueClass
                addGetter {
                    returnType = node.type
                    origin = IrDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER
                }.apply {
                    createDispatchReceiverParameter()
                    body = with(context.createIrBuilder(this.symbol)) {
                        irExprBody(getter(this, dispatchReceiverParameter!!))
                    }
                }
            }
        }

    data class VirtualProperty<in T>(val type: IrType, val makeGetter: ExpressionGenerator<T>, val assigner: ExpressionSupplier<T>?) {
        constructor(declaration: IrValueDeclaration) : this(
            declaration.type,
            { irGet(declaration) },
            if (declaration.isAssignable) { _, value -> irSet(declaration, value) } else null,
        )

        val isAssignable: Boolean
            get() = assigner != null

        fun <R> map(f: (R) -> T) = VirtualProperty<R>(
            type = type,
            makeGetter = { makeGetter(f(it)) },
            assigner = assigner?.let { assigner -> { additionalParam, value -> assigner(f(additionalParam), value) } },
        )
    }

    fun implementationAgnosticFromDeclarations(declarations: List<IrValueDeclaration>) =
        ImplementationAgnostic<Any?>(declarations.map { VirtualProperty(it) })

    /**
     * Multi-field value class instance can be not only a standalone instance but also:
     *   1. A slice of fields in another multi-field class instance;
     *   2. A slice of fields in another regular class instance;
     *   3. Virtual instance constructed with local variables;
     *   4. Virtual instance constructed with local parameters;
     *   5. Virtual instance constructed with local fields;
     *   6. Some mix of the above ones.
     */
    inner class ImplementationAgnostic<T>(val virtualFields: List<VirtualProperty<T>>) {
        val regularDeclarations = this@MultiFieldValueClassSpecificDeclarations
        
        fun <R> map(f: (R) -> T) = ImplementationAgnostic(virtualFields.map { it.map(f) })

        init {
            require(virtualFields.size == leaves.size) { "Wrong symbols number given for $leaves, got $virtualFields" }
            for ((actual, expected) in virtualFields.map { it.type } zip leaves.map { it.type }) {
                require(actual.isSubtypeOf(expected, typeSystemContext)) { "$actual is not a subtype of $expected for MFVC $valueClass" }
            }
        }

        val nodeToSymbols = indexesByInternalNode.mapValues { (_, indexes) -> virtualFields.slice(indexes) } +
                indexByLeaf.mapValues { (_, index) -> listOf(virtualFields[index]) }

        private val internalNodeToExpressionGetters: Map<InternalNode<Unit, Unit>, ExpressionGenerator<T>> =
            indexesByInternalNode.mapValues { (node, indexes) ->
                { additionalParameter ->
                    val arguments = virtualFields.slice(indexes).map { it.makeGetter(this, additionalParameter) }
                    val innerDeclarations = replacements.getDeclarations(node.irClass!!)!!
                    irCall(innerDeclarations.boxMethod).apply {
                        for (i in arguments.indices) {
                            putValueArgument(i, arguments[i])
                        }
                    }
                }
            }

        private val leavesToExpressionGetters: Map<Leaf<Unit>, ExpressionGenerator<T>> =
            indexByLeaf.mapValues { (_, index) -> { virtualFields[index].makeGetter(this, it) } }

        val nodeToExpressionGetters: Map<MultiFieldValueClassTree<Unit, Unit>, ExpressionGenerator<T>> =
            internalNodeToExpressionGetters + leavesToExpressionGetters

        operator fun get(name: Name): Pair<ExpressionGenerator<T>, ImplementationAgnostic<T>?>? =
            when (val field = loweringRepresentation[name]) {
                null -> null
                else -> nodeToExpressionGetters[field.node]!! to when (field.node) {
                    is Leaf -> null
                    is InternalNode -> {
                        val irClass = field.node.irClass!!
                        val declarations = replacements.getDeclarations(irClass)!!
                        declarations.ImplementationAgnostic(nodeToSymbols[field.node]!!)
                    }
                }
            }

        val boxedExpression = nodeToExpressionGetters[loweringRepresentation]!!

        // todo type parameters
        // todo recursive
        // todo cheeeck!!!
        // todo equals
        // todo toString
        // todo hashCode
        // todo default parameters
        // todo annotations etc.
        // todo inline in not value class
    }
}

typealias ExpressionGenerator<T> = IrBuilderWithScope.(T) -> IrExpression
typealias ExpressionSupplier<T> = IrBuilderWithScope.(T, IrExpression) -> IrStatement