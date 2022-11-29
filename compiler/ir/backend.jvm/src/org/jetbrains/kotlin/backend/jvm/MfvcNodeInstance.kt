/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.substitute
import org.jetbrains.kotlin.name.Name

interface MfvcNodeInstance {
    val node: MfvcNode
    val typeArguments: TypeArguments
    val type: IrSimpleType

    fun makeFlattenedGetterExpressions(scope: IrBlockBuilder): List<IrExpression>
    fun makeGetterExpression(scope: IrBuilderWithScope): IrExpression
    operator fun get(name: Name): MfvcNodeInstance?
    fun makeStatements(scope: IrBuilderWithScope, values: List<IrExpression>): List<IrStatement>
}

private fun makeTypeFromMfvcNodeAndTypeArguments(node: MfvcNode, typeArguments: TypeArguments) =
    node.type.substitute(typeArguments) as IrSimpleType

fun MfvcNodeInstance.addSetterStatements(scope: IrBlockBuilder, values: List<IrExpression>) = with(scope) {
    for (statement in makeStatements(this, values)) {
        +statement
    }
}

fun MfvcNodeInstance.makeSetterExpressions(scope: IrBuilderWithScope, values: List<IrExpression>): IrExpression = scope.irBlock {
    addSetterStatements(this, values)
}

private fun MfvcNodeInstance.checkValuesCount(values: List<IrExpression>) {
    require(values.size == node.leavesCount) { "Node $node requires ${node.leavesCount} values but got ${values.map { it.render() }}" }
}

class ValueDeclarationMfvcNodeInstance(
    override val node: MfvcNode,
    override val typeArguments: TypeArguments,
    val valueDeclarations: List<IrValueDeclaration>,
) : MfvcNodeInstance {
    init {
        require(valueDeclarations.size == size) { "Expected value declarations list of size $size but got of size ${valueDeclarations.size}" }
    }

    override val type: IrSimpleType = makeTypeFromMfvcNodeAndTypeArguments(node, typeArguments)

    override fun makeFlattenedGetterExpressions(scope: IrBlockBuilder): List<IrExpression> =
        makeFlattenedGetterExpressions(scope as IrBuilderWithScope)
    private fun makeFlattenedGetterExpressions(scope: IrBuilderWithScope): List<IrExpression> = valueDeclarations.map { scope.irGet(it) }

    override fun makeGetterExpression(scope: IrBuilderWithScope): IrExpression = when (node) {
        is LeafMfvcNode -> makeFlattenedGetterExpressions(scope).single()
        is MfvcNodeWithSubnodes -> node.makeBoxedExpression(scope, typeArguments, makeFlattenedGetterExpressions(scope))
    }

    override fun get(name: Name): ValueDeclarationMfvcNodeInstance? {
        val (newNode, indices) = node.getSubnodeAndIndices(name) ?: return null
        return ValueDeclarationMfvcNodeInstance(newNode, typeArguments, valueDeclarations.slice(indices))
    }

    override fun makeStatements(scope: IrBuilderWithScope, values: List<IrExpression>): List<IrStatement> {
        checkValuesCount(values)
        return valueDeclarations.zip(values) { declaration, value -> scope.irSet(declaration, value) }
    }
}

internal class ExpressionCopierImpl(
    expression: IrExpression?,
    private val scope: IrBlockBuilder,
    private val saveVariable: (IrVariable) -> Unit,
) {
    private sealed interface CopyableExpression {
        fun makeExpression(scope: IrBuilderWithScope): IrExpression
    }

    private class SavedToVariable(val variable: IrVariable) : CopyableExpression {
        override fun makeExpression(scope: IrBuilderWithScope): IrExpression = scope.irGet(variable)
    }

    private class PureExpression(val expression: IrExpression) : CopyableExpression {
        override fun makeExpression(scope: IrBuilderWithScope): IrExpression = expression.deepCopyWithSymbols()
    }

    private fun IrExpression.orSavedToVariable(): CopyableExpression =
        if (isRepeatableGetter()) {
            PureExpression(this)
        } else SavedToVariable(
            scope.savableStandaloneVariableWithSetter(
                this@orSavedToVariable,
                origin = IrDeclarationOrigin.TEMPORARY_MULTI_FIELD_VALUE_CLASS_VARIABLE,
                saveVariable = saveVariable,
                isTemporary = true,
            )
        )

    private val copyableExpression = expression?.orSavedToVariable()

    fun makeCopy() = copyableExpression?.makeExpression(scope)
}

fun IrExpression?.isRepeatableGetter(): Boolean = when (this) {
    null -> true
    is IrConst<*> -> true
    is IrGetValue -> true
    is IrGetField -> receiver.isRepeatableGetter()
    is IrTypeOperatorCallImpl -> this.argument.isRepeatableGetter()
    is IrContainerExpression -> statements.all { it is IrExpression && it.isRepeatableGetter() || it is IrVariable }
    else -> false
}

fun IrExpression?.isRepeatableSetter(): Boolean = when (this) {
    null -> true
    is IrConst<*> -> true
    is IrSetValue -> value.isRepeatableGetter()
    is IrSetField -> receiver.isRepeatableGetter() && value.isRepeatableGetter()
    is IrTypeOperatorCallImpl -> this.argument.isRepeatableSetter()
    is IrContainerExpression -> statements.dropLast(1).all { it is IrExpression && it.isRepeatableGetter() || it is IrVariable } &&
            statements.lastOrNull().let { it is IrExpression? && it.isRepeatableSetter() }

    else -> false
}

fun IrExpression?.isRepeatableAccessor(): Boolean = isRepeatableGetter() || isRepeatableSetter()

enum class AccessType { AlwaysPublic, PrivateWhenNoBox, AlwaysPrivate }

class ReceiverBasedMfvcNodeInstance(
    private val scope: IrBlockBuilder,
    override val node: MfvcNode,
    override val typeArguments: TypeArguments,
    receiver: IrExpression?,
    val fields: List<IrField>?,
    val unboxMethod: IrSimpleFunction?,
    val accessType: AccessType,
    private val saveVariable: (IrVariable) -> Unit,
) : MfvcNodeInstance {
    override val type: IrSimpleType = makeTypeFromMfvcNodeAndTypeArguments(node, typeArguments)

    private val receiverCopier = ExpressionCopierImpl(receiver, scope, saveVariable)

    private fun makeReceiverCopy() = receiverCopier.makeCopy()

    init {
        require(fields == null || fields.isNotEmpty()) { "Empty list of fields" }
        require(node is RootMfvcNode == (unboxMethod == null)) { "Only root node has node getter" }
    }

    override fun makeFlattenedGetterExpressions(scope: IrBlockBuilder): List<IrExpression> = when (node) {
        is LeafMfvcNode -> listOf(makeGetterExpression(scope))
        is MfvcNodeWithSubnodes -> when {
            node is IntermediateMfvcNode && canUsePrivateAccessFor(node) && fields != null ->
                fields.map { scope.irGetField(makeReceiverCopy(), it) }

            node is IntermediateMfvcNode && !node.hasPureUnboxMethod -> {
                val value = makeGetterExpression(scope)
                val asVariable = scope.savableStandaloneVariableWithSetter(
                    value,
                    origin = IrDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_PARAMETER,
                    saveVariable = saveVariable,
                    isTemporary = true,
                )
                val root = node.rootNode
                val variableInstance =
                    root.createInstanceFromBox(scope, typeArguments, scope.irGet(asVariable), accessType, saveVariable)
                variableInstance.makeFlattenedGetterExpressions(scope)
            }

            else -> node.subnodes.flatMap { get(it.name)!!.makeFlattenedGetterExpressions(scope) }
        }
    }

    override fun makeGetterExpression(scope: IrBuilderWithScope): IrExpression = with(scope) {
        when {
            node is LeafMfvcNode && canUsePrivateAccessFor(node) && fields != null -> irGetField(makeReceiverCopy(), fields.single())
            node is IntermediateMfvcNode && accessType == AccessType.AlwaysPrivate && fields != null ->
                node.makeBoxedExpression(this, typeArguments, fields.map { irGetField(makeReceiverCopy(), it) })

            unboxMethod != null -> irCall(unboxMethod).apply {
                val dispatchReceiverParameter = unboxMethod.dispatchReceiverParameter
                if (dispatchReceiverParameter != null) {
                    dispatchReceiver = makeReceiverCopy() ?: run {
                        val erasedUpperBound = dispatchReceiverParameter.type.erasedUpperBound
                        require(erasedUpperBound.isCompanion) { "Expected a dispatch receiver for:\n${unboxMethod.dump()}" }
                        irGetObject(erasedUpperBound.symbol)
                    }
                }
            }

            node is RootMfvcNode -> makeReceiverCopy()!!
            else -> error("Unbox method must exist for $node")
        }
    }

    private fun canUsePrivateAccessFor(node: NameableMfvcNode) =
        node.hasPureUnboxMethod && accessType == AccessType.PrivateWhenNoBox || accessType == AccessType.AlwaysPrivate

    override fun get(name: Name): ReceiverBasedMfvcNodeInstance? {
        val (newNode, _) = node.getSubnodeAndIndices(name) ?: return null
        return newNode.createInstanceFromBox(scope, typeArguments, makeReceiverCopy(), accessType, saveVariable)
    }

    override fun makeStatements(scope: IrBuilderWithScope, values: List<IrExpression>): List<IrStatement> {
        checkValuesCount(values)
        require(fields != null) { "$node is immutable as it has custom getter and so no backing fields" }
        return fields.zip(values) { field, expr -> scope.irSetField(makeReceiverCopy(), field, expr) }
    }
}

val MfvcNodeInstance.size: Int
    get() = node.leavesCount

fun IrContainerExpression.unwrapBlock(): IrExpression = statements.singleOrNull() as? IrExpression ?: this

/**
 * Creates a variable and doesn't add it to a container. It saves the variable with given saveVariable.
 *
 * It may be used when the variable will be used outside the current container so the declaration is added later when all usages are known.
 */
fun IrBuilderWithScope.savableStandaloneVariable(
    type: IrType,
    name: String? = null,
    isMutable: Boolean = false,
    origin: IrDeclarationOrigin,
    isTemporary: Boolean = origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
    saveVariable: (IrVariable) -> Unit,
): IrVariable {
    val variable = if (isTemporary || name == null) scope.createTemporaryVariableDeclaration(
        type, name, isMutable,
        startOffset = startOffset,
        endOffset = endOffset,
        origin = origin,
    ) else IrVariableImpl(
        startOffset = startOffset,
        endOffset = endOffset,
        origin = origin,
        symbol = IrVariableSymbolImpl(),
        name = Name.identifier(name),
        type = type,
        isVar = isMutable,
        isConst = false,
        isLateinit = false
    ).apply {
        parent = this@savableStandaloneVariable.scope.getLocalDeclarationParent()
    }
    saveVariable(variable)
    return variable
}

/**
 * Creates a variable and doesn't add it to a container. It saves the variable with given saveVariable. It adds irSet-based initialization.
 *
 * It may be used when the variable will be used outside the current container so the declaration is added later when all usages are known.
 */
fun <T : IrElement> IrStatementsBuilder<T>.savableStandaloneVariableWithSetter(
    expression: IrExpression,
    name: String? = null,
    isMutable: Boolean = false,
    origin: IrDeclarationOrigin,
    isTemporary: Boolean = origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
    saveVariable: (IrVariable) -> Unit,
) = savableStandaloneVariable(expression.type, name, isMutable, origin, isTemporary, saveVariable).also {
    +irSet(it, expression)
}
