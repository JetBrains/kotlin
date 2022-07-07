/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.jvm.MultiFieldValueClassSpecificDeclarations
import org.jetbrains.kotlin.backend.jvm.MultiFieldValueClassTree
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.substitute
import org.jetbrains.kotlin.name.Name

typealias TypeArguments = Map<IrTypeParameterSymbol, IrType>

sealed interface MFVCRepresentation<Leaf : MFVCLeafRepresentation> {
    operator fun get(name: Name): MFVCRepresentation<Leaf>?
    fun IrBuilderWithScope.makeGetter(): IrExpression
    val type: IrSimpleType
    val children: List<Pair<Name, MFVCRepresentation<Leaf>>>
    val leaves: List<Leaf>
}

sealed interface MutableMFVCRepresentation<Leaf : MFVCLeafRepresentation> : MFVCRepresentation<Leaf>
sealed interface RecursivelyMutableMFVCRepresentation<Leaf : MFVCLeafRepresentation> : MutableMFVCRepresentation<Leaf>

interface MFVCLeafRepresentation : MFVCRepresentation<Nothing> {
    override fun get(name: Name): Nothing? = null
    override val children: List<Pair<Name, MFVCRepresentation<Nothing>>>
        get() = emptyList()
    override val leaves: List<Nothing>
        get() = emptyList()
}

interface MutableMFVCLeafRepresentation : MFVCLeafRepresentation, RecursivelyMutableMFVCRepresentation<Nothing> {
    fun IrBuilderWithScope.makeSetter(value: IrExpression): IrExpression
}

sealed class ValueDeclarationLeafRepresentation(open val valueDeclaration: IrValueDeclaration) : MFVCLeafRepresentation {
    override fun IrBuilderWithScope.makeGetter(): IrExpression = irGet(valueDeclaration)
    override val type: IrSimpleType
        get() = valueDeclaration.type as IrSimpleType
}

data class VariableLeafRepresentation(override val valueDeclaration: IrVariable) : ValueDeclarationLeafRepresentation(valueDeclaration),
    MutableMFVCLeafRepresentation {
    override fun IrBuilderWithScope.makeSetter(value: IrExpression): IrExpression = irSet(valueDeclaration, value)
}

data class ValueParameterLeafRepresentation(override val valueDeclaration: IrValueParameter) :
    ValueDeclarationLeafRepresentation(valueDeclaration)

sealed class ReceiverBasedLeafRepresentation(open val receiver: IrValueDeclaration) : MFVCLeafRepresentation {
    protected fun makeTypeParametersMapping(): Map<IrTypeParameterSymbol, IrType> {
        val type = receiver.type as IrSimpleType
        return (type.erasedUpperBound.typeParameters zip type.arguments)
            .mapNotNull { (parameter, argument) -> argument.typeOrNull?.let { parameter.symbol to it } }.toMap()
    }
}

data class FieldAccessLeafRepresentation(override val receiver: IrValueDeclaration, val field: IrField) :
    ReceiverBasedLeafRepresentation(receiver), MutableMFVCLeafRepresentation {
    override fun IrBuilderWithScope.makeGetter(): IrExpression = irGetField(irGet(receiver), field)
    override fun IrBuilderWithScope.makeSetter(value: IrExpression): IrExpression = irSetField(irGet(receiver), field, value)
    override val type: IrSimpleType = field.type.substitute(makeTypeParametersMapping()) as IrSimpleType
}

data class GetterLeafRepresentation(override val receiver: IrValueDeclaration, val getter: IrFunction) :
    ReceiverBasedLeafRepresentation(receiver) {
    init {
        require(getter.typeParameters.isEmpty()) { "Getter ${getter.render()} must have no type parameters" }
    }

    override fun IrBuilderWithScope.makeGetter(): IrExpression = irCall(getter).apply {
        dispatchReceiver = irGet(receiver)
    }

    override val type: IrSimpleType = getter.returnType.substitute(makeTypeParametersMapping()) as IrSimpleType
}

private fun getChildType(
    declarations: MultiFieldValueClassSpecificDeclarations,
    typeMapping: Map<IrTypeParameterSymbol, IrSimpleType>,
    name: Name,
): IrSimpleType? = declarations.loweringRepresentation[name]?.type?.substitute(typeMapping) as IrSimpleType?


private fun <T : MFVCRepresentation<Leaf>, Leaf : T, InternalNode : T> makeTree(
    node: MultiFieldValueClassTree,
    typeArguments: TypeArguments,
    createInternalNode: (type: IrSimpleType, typeArguments: TypeArguments, node: MultiFieldValueClassTree, children: List<Pair<Name, T>>) -> InternalNode,
    createLeaf: (type: IrType, node: MultiFieldValueClassTree) -> Leaf,
): T = when (node) {
    is MultiFieldValueClassTree.InternalNode -> {
        val type = node.type.substitute(typeArguments) as IrSimpleType
        val children = node.fields.map { it.name to makeTree(it.node, typeArguments, createInternalNode, createLeaf) }
        createInternalNode(type, typeArguments, node, children)
    }
    is MultiFieldValueClassTree.Leaf -> createLeaf(node.type.substitute(typeArguments), node)
}

sealed interface InternalNodeRepresentation<Leaf : MFVCLeafRepresentation> : MFVCRepresentation<Leaf>

abstract class InternalNodeRepresentationImpl<Leaf : MFVCLeafRepresentation>(
    val declarations: MultiFieldValueClassSpecificDeclarations,
    val typeArguments: TypeArguments,
    final override val children: List<Pair<Name, MFVCRepresentation<Leaf>>>,
) : InternalNodeRepresentation<Leaf> {
    final override val type: IrSimpleType = declarations.valueClass.defaultType.substitute(typeArguments) as IrSimpleType

    final override val leaves: List<Leaf> = children.flatMap { (_, node) -> node.leaves }

    private val childrenMap = children.toMap()

    final override fun get(name: Name): MFVCRepresentation<Leaf>? = childrenMap[name]

    final override fun IrBuilderWithScope.makeGetter(): IrExpression = irCall(declarations.boxMethod).apply {
        declarations.valueClass.typeParameters.forEachIndexed { index, typeParameter ->
            typeArguments[typeParameter.symbol]?.let { putTypeArgument(index, it) }
        }
        leaves.forEachIndexed { index, leaf -> putValueArgument(index, leaf.run { makeGetter() }) }
    }
}

abstract class MutableInternalNodeRepresentation<Leaf : MFVCLeafRepresentation>(
    declarations: MultiFieldValueClassSpecificDeclarations,
    typeArguments: TypeArguments,
    children: List<Pair<Name, MFVCRepresentation<Leaf>>>
) : InternalNodeRepresentationImpl<Leaf>(declarations, typeArguments, children), MutableMFVCRepresentation<Leaf> {
    abstract fun IrBuilderWithScope.makeSetter(values: List<IrExpression>): IrExpression
}

abstract class RecursivelyMutableInternalNodeRepresentation<Leaf : MutableMFVCLeafRepresentation>(
    declarations: MultiFieldValueClassSpecificDeclarations,
    typeArguments: TypeArguments,
    children: List<Pair<Name, MFVCRepresentation<Leaf>>>,
) : MutableInternalNodeRepresentation<Leaf>(declarations, typeArguments, children), RecursivelyMutableMFVCRepresentation<Leaf> {
    final override fun IrBuilderWithScope.makeSetter(values: List<IrExpression>): IrExpression {
        require(values.size == leaves.size) { "Expected ${leaves.size} expressions but found ${values.size} of them" }
        return irComposite {
            for ((leaf, value) in leaves zip values) {
                +leaf.run { makeSetter(value) }
            }
        }
    }
}

sealed interface ValueDeclarationInternalNodeRepresentation<Leaf : ValueDeclarationLeafRepresentation> : InternalNodeRepresentation<Leaf>

class MutableValueDeclarationInternalNodeRepresentation(
    declarations: MultiFieldValueClassSpecificDeclarations,
    typeArguments: TypeArguments,
    children: List<Pair<Name, RecursivelyMutableMFVCRepresentation<VariableLeafRepresentation>>>,
) : ValueDeclarationInternalNodeRepresentation<VariableLeafRepresentation>,
    RecursivelyMutableInternalNodeRepresentation<VariableLeafRepresentation>(declarations, typeArguments, children)

class FieldAccessInternalNodeRepresentation(
    declarations: MultiFieldValueClassSpecificDeclarations,
    typeArguments: TypeArguments,
    children: List<Pair<Name, RecursivelyMutableMFVCRepresentation<FieldAccessLeafRepresentation>>>,
) : RecursivelyMutableInternalNodeRepresentation<FieldAccessLeafRepresentation>(declarations, typeArguments, children)

class RegularClassMFVCInternalNodeRepresentation(
    declarations: MultiFieldValueClassSpecificDeclarations,
    typeArguments: TypeArguments,
    children: List<Pair<Name, MFVCRepresentation<ReceiverBasedLeafRepresentation>>>,
) : InternalNodeRepresentationImpl<ReceiverBasedLeafRepresentation>(declarations, typeArguments, children) {
    init {
        leaves.verifyEqualReceivers()
    }
}

private fun List<ReceiverBasedLeafRepresentation>.verifyEqualReceivers() {
    val receiver = this[0].receiver
    require(all { it.receiver == receiver }) { "Different receivers found in leaves" }
}

class MutableRegularClassMFVCInternalNodeRepresentation(
    declarations: MultiFieldValueClassSpecificDeclarations,
    val setter: IrFunction,
    typeArguments: TypeArguments,
    children: List<Pair<Name, MFVCRepresentation<ReceiverBasedLeafRepresentation>>>,
) : MutableInternalNodeRepresentation<ReceiverBasedLeafRepresentation>(declarations, typeArguments, children) {
    init {
        leaves.verifyEqualReceivers()
        require(setter.typeParameters.isEmpty()) { "No type parameters are expected for ${setter.render()}" }
    }

    override fun IrBuilderWithScope.makeSetter(values: List<IrExpression>): IrExpression = irCall(setter).apply {
        dispatchReceiver = irGet(leaves[0].receiver)
        values.forEachIndexed { index, value -> putValueArgument(index, value) }
    }
}
