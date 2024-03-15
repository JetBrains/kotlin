/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

data class TailSuspendCalls(val callSites: Set<IrCall>, val hasNotTailSuspendCalls: Boolean)

/**
 * Collects calls to be treated as tail calls: "last" expressions which are either direct return statement with a call
 * to other suspend function, or, for a Unit-returning function, a call to other suspend function, also returning Unit.
 */
fun collectTailSuspendCalls(context: CommonBackendContext, irFunction: IrSimpleFunction): TailSuspendCalls {
    require(irFunction.isSuspend) { "A suspend function expected: ${irFunction.render()}" }
    val body = irFunction.body ?: return TailSuspendCalls(emptySet(), false)

    class VisitorState(val insideTryBlock: Boolean, val isTailExpression: Boolean)

    val isUnitReturn = irFunction.returnType.isUnit()
    var hasNotTailSuspendCall = false
    val tailSuspendCalls = mutableSetOf<IrCall>()
    val tailReturnableBlocks = mutableSetOf<IrReturnableBlockSymbol>()

    val visitor = object : IrElementVisitor<Unit, VisitorState> {
        override fun visitElement(element: IrElement, data: VisitorState) {
            element.acceptChildren(this, VisitorState(data.insideTryBlock, isTailExpression = false))
        }

        override fun visitTypeOperator(expression: IrTypeOperatorCall, data: VisitorState) {
            if (expression.operator == IrTypeOperator.IMPLICIT_CAST) {
                expression.acceptChildren(this, data)
            } else {
                super.visitTypeOperator(expression, data)
            }
        }

        override fun visitTry(aTry: IrTry, data: VisitorState) {
            aTry.tryResult.accept(this, VisitorState(insideTryBlock = true, isTailExpression = false))
            aTry.catches.forEach { it.result.accept(this, data) }
            require(aTry.finallyExpression == null) { "All finally clauses should've been lowered out" }
        }

        private fun isTailReturn(expression: IrReturn) =
            expression.returnTargetSymbol == irFunction.symbol || expression.returnTargetSymbol in tailReturnableBlocks

        override fun visitReturn(expression: IrReturn, data: VisitorState) {
            expression.value.accept(this, VisitorState(data.insideTryBlock, isTailReturn(expression)))
        }

        override fun visitExpressionBody(body: IrExpressionBody, data: VisitorState) =
            body.acceptChildren(this, data)

        override fun visitBlockBody(body: IrBlockBody, data: VisitorState) =
            visitStatementContainer(body, data)

        override fun visitContainerExpression(expression: IrContainerExpression, data: VisitorState) {
            if (expression is IrReturnableBlock && data.isTailExpression)
                tailReturnableBlocks.add(expression.symbol)
            visitStatementContainer(expression, data)
        }

        private fun visitStatementContainer(expression: IrStatementContainer, data: VisitorState) {
            expression.statements.forEachIndexed { index, irStatement ->
                val isTailStatement = if (index == expression.statements.lastIndex) {
                    // The last statement defines the result of the container expression, so it has the same kind.
                    // Note: this is even true for returnable blocks: if it is a Unit-returning block, this is exactly
                    // like a usual block; if it is a non-Unit block, then it must end with an explicit return statement.
                    data.isTailExpression
                } else {
                    // In a Unit-returning function, any statement directly followed by a `return` is a tail statement.
                    isUnitReturn && expression.statements[index + 1].let {
                        it is IrReturn && isTailReturn(it) && it.value.isUnitRead()
                    }
                }
                irStatement.accept(this, VisitorState(data.insideTryBlock, isTailStatement))
            }
        }

        override fun visitWhen(expression: IrWhen, data: VisitorState) {
            expression.branches.forEach {
                it.condition.accept(this, VisitorState(data.insideTryBlock, isTailExpression = false))
                it.result.accept(this, data)
            }
        }

        override fun visitCall(expression: IrCall, data: VisitorState) {
            if (expression.isSuspend) {
                if (!data.insideTryBlock && data.isTailExpression)
                    tailSuspendCalls.add(expression)
                else
                    hasNotTailSuspendCall = true
            }

            // For a tail call, [returnIfSuspended] can be optimized away.
            val isTailExpression = data.isTailExpression && expression.isReturnIfSuspendedCall()
            expression.acceptChildren(this, VisitorState(data.insideTryBlock, isTailExpression))
        }

        private fun IrExpression.isUnitRead(): Boolean {
            if (this is IrTypeOperatorCall) {
                return this.argument.isUnitRead()
            }
            return this is IrGetObjectValue && symbol == context.irBuiltIns.unitClass
        }

        private fun IrCall.isReturnIfSuspendedCall() =
            symbol == context.ir.symbols.returnIfSuspended
    }

    body.accept(visitor, VisitorState(insideTryBlock = false, isTailExpression = true))
    return TailSuspendCalls(tailSuspendCalls, hasNotTailSuspendCall)
}