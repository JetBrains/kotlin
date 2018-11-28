/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.ir.isSuspend
import org.jetbrains.kotlin.backend.common.lower.FinallyBlocksLowering
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.*


object COROUTINE_ROOT_LOOP : IrStatementOriginImpl("COROUTINE_ROOT_LOOP")
object COROUTINE_SWITCH : IrStatementOriginImpl("COROUTINE_SWITCH")

open class SuspendableNodesCollector(protected val suspendableNodes: MutableSet<IrElement>) : IrElementVisitorVoid {

    protected var hasSuspendableChildren = false

    override fun visitElement(element: IrElement) {
        val current = hasSuspendableChildren
        hasSuspendableChildren = false
        element.acceptChildrenVoid(this)
        if (hasSuspendableChildren) {
            suspendableNodes += element
        }
        hasSuspendableChildren = hasSuspendableChildren || current
    }

    override fun visitCall(expression: IrCall) {
        super.visitCall(expression)
        if (expression.isSuspend) {
            suspendableNodes += expression
            hasSuspendableChildren = true
        }
    }
}

fun collectSuspendableNodes(
    body: IrBlock,
    suspendableNodes: MutableSet<IrElement>,
    context: JsIrBackendContext,
    function: IrFunction
): IrBlock {

    // 1st: mark suspendable loops and tries
    body.acceptVoid(SuspendableNodesCollector(suspendableNodes))
    // 2nd: mark inner terminators
    val terminatorsCollector = SuspendedTerminatorsCollector(suspendableNodes)
    body.acceptVoid(terminatorsCollector)

    if (terminatorsCollector.shouldFinalliesBeLowered) {
        val finallyLower = FinallyBlocksLowering(context, context.dynamicType)

        function.body = IrBlockBodyImpl(body.startOffset, body.endOffset, body.statements)
        function.transform(finallyLower, null)

        val newBody = function.body as IrBlockBody
        function.body = null
        suspendableNodes.clear()
        val newBlock = JsIrBuilder.buildBlock(body.type, newBody.statements)

        return collectSuspendableNodes(newBlock, suspendableNodes, context, function)
    }

    return body
}

class SuspendedTerminatorsCollector(suspendableNodes: MutableSet<IrElement>) : SuspendableNodesCollector(suspendableNodes) {

    var shouldFinalliesBeLowered = false

    override fun visitBreakContinue(jump: IrBreakContinue) {
        if (jump.loop in suspendableNodes) {
            suspendableNodes.add(jump)
            hasSuspendableChildren = true
        }

        shouldFinalliesBeLowered = shouldFinalliesBeLowered || tryStack.any { it.finallyExpression != null && it in suspendableNodes }
    }

    private val tryStack = mutableListOf<IrTry>()
    private val tryLoopStack = mutableListOf<IrStatement>()

    private fun pushTry(aTry: IrTry) {
        tryStack.push(aTry)
        tryLoopStack.push(aTry)
    }

    private fun popTry() {
        tryLoopStack.pop()
        tryStack.pop()
    }

    private fun pushLoop(loop: IrLoop) {
        tryLoopStack.push(loop)
    }

    private fun popLoop() {
        tryLoopStack.pop()
    }

    override fun visitLoop(loop: IrLoop) {
        pushLoop(loop)

        super.visitLoop(loop)

        popLoop()
    }

    override fun visitTry(aTry: IrTry) {
        pushTry(aTry)

        super.visitTry(aTry)

        popTry()
    }

    override fun visitReturn(expression: IrReturn) {
        shouldFinalliesBeLowered = shouldFinalliesBeLowered || tryStack.any { it.finallyExpression != null && it in suspendableNodes }

        super.visitReturn(expression)

        if (expression.returnTargetSymbol is IrReturnableBlockSymbol) {
            suspendableNodes.add(expression)
            hasSuspendableChildren = true
        }
    }
}


class LiveLocalsTransformer(
    private val localMap: Map<IrValueSymbol, IrFieldSymbol>,
    private val receiver: IrExpression,
    private val unitType: IrType
) :
    IrElementTransformerVoid() {
    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val field = localMap[expression.symbol] ?: return expression
        return expression.run { IrGetFieldImpl(startOffset, endOffset, field, type, receiver, origin) }
    }

    override fun visitSetVariable(expression: IrSetVariable): IrExpression {
        expression.transformChildrenVoid(this)
        val field = localMap[expression.symbol] ?: return expression
        return expression.run { IrSetFieldImpl(startOffset, endOffset, field, receiver, value, unitType, origin) }
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        declaration.transformChildrenVoid(this)
        val field = localMap[declaration.symbol] ?: return declaration
        val initializer = declaration.initializer
        return if (initializer != null) {
            declaration.run { IrSetFieldImpl(startOffset, endOffset, field, receiver, initializer, unitType) }
        } else {
            JsIrBuilder.buildComposite(declaration.type)
        }
    }
}