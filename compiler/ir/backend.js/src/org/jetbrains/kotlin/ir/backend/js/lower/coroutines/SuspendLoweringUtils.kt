/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.ir.isSuspend
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.getInlinedClass
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.visitors.*

object COROUTINE_ROOT_LOOP : IrStatementOriginImpl("COROUTINE_ROOT_LOOP")
object COROUTINE_SWITCH : IrStatementOriginImpl("COROUTINE_SWITCH")

open class SuspendableNodesCollector(private val suspendableNodes: MutableSet<IrElement>) : IrElementVisitorVoid {

    private var hasSuspendableChildren = false

    private fun markNode(node: IrElement) {
        suspendableNodes += node
        hasSuspendableChildren = true
    }

    private fun isSuspendableNode(node: IrElement) = node in suspendableNodes

    override fun visitElement(element: IrElement) {
        val current = hasSuspendableChildren
        hasSuspendableChildren = false
        element.acceptChildrenVoid(this)
        if (hasSuspendableChildren) {
            markNode(element)
        }
        hasSuspendableChildren = hasSuspendableChildren || current
    }

    override fun visitCall(expression: IrCall) {
        super.visitCall(expression)
        if (expression.isSuspend) {
            markNode(expression)
        }
    }

    override fun visitBreakContinue(jump: IrBreakContinue) {
        if (isSuspendableNode(jump.loop)) {
            markNode(jump)
        }
    }

    override fun visitReturn(expression: IrReturn) {
        super.visitReturn(expression)

        if (expression.returnTargetSymbol is IrReturnableBlockSymbol && isSuspendableNode(expression.returnTargetSymbol.owner)) {
            markNode(expression)
        }
    }
}

fun collectSuspendableNodes(function: IrBlock): MutableSet<IrElement> {
    val suspendableNodes = mutableSetOf<IrElement>()
    var size: Int

    do {
        size = suspendableNodes.size
        function.acceptVoid(SuspendableNodesCollector(suspendableNodes))
    } while (size != suspendableNodes.size)

    return suspendableNodes
}

class LiveLocalsTransformer(
    private val localMap: Map<IrValueSymbol, IrFieldSymbol>,
    private val receiver: () -> IrExpression,
    private val unitType: IrType
) :
    IrElementTransformerVoid() {
    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val field = localMap[expression.symbol] ?: return expression
        return expression.run { IrGetFieldImpl(startOffset, endOffset, field, type, receiver(), origin) }
    }

    override fun visitSetValue(expression: IrSetValue): IrExpression {
        expression.transformChildrenVoid(this)
        val field = localMap[expression.symbol] ?: return expression
        return expression.run { IrSetFieldImpl(startOffset, endOffset, field, receiver(), value, unitType, origin) }
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        declaration.transformChildrenVoid(this)
        val field = localMap[declaration.symbol] ?: return declaration
        val initializer = declaration.initializer
        return if (initializer != null) {
            declaration.run { IrSetFieldImpl(startOffset, endOffset, field, receiver(), initializer, unitType) }
        } else {
            JsIrBuilder.buildComposite(declaration.type)
        }
    }
}

internal fun needUnboxingOrUnit(fromType: IrType, toType: IrType): Boolean {
    return (fromType.getInlinedClass() == null && toType.getInlinedClass() != null) ||
            (fromType.isUnit() && !toType.isUnit())
}