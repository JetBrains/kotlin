/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCallOp
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irSetVar
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.util.OperatorNameConventions

val forLoopsPhase = makeIrFilePhase(
    ::ForLoopsLowering,
    name = "ForLoopsLowering",
    description = "For loops lowering"
)

/**
 * This lowering pass optimizes for-loops.
 *
 * Replace iteration over ranges (X.indices, a..b, etc.) and arrays with
 * simple while loop over primitive induction variable.
 */
internal class ForLoopsLowering(val context: CommonBackendContext) : FileLoweringPass {

    private val headerInfoBuilder = HeaderInfoBuilder(context)

    override fun lower(irFile: IrFile) {
        val oldLoopToNewLoop = mutableMapOf<IrLoop, IrLoop>()
        val transformer = RangeLoopTransformer(context, oldLoopToNewLoop, headerInfoBuilder)
        irFile.transformChildrenVoid(transformer)
        // Update references in break/continue.
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitBreakContinue(jump: IrBreakContinue): IrExpression {
                oldLoopToNewLoop[jump.loop]?.let { jump.loop = it }
                return jump
            }
        })
    }
}

private class RangeLoopTransformer(
    val context: CommonBackendContext,
    val oldLoopToNewLoop: MutableMap<IrLoop, IrLoop>,
    headerInfoBuilder: HeaderInfoBuilder
) : IrElementTransformerVoidWithContext() {

    private val symbols = context.ir.symbols
    private val iteratorToLoopHeader = mutableMapOf<IrVariableSymbol, ForLoopHeader>()
    private val headerProcessor = HeaderProcessor(context, headerInfoBuilder, this::getScopeOwnerSymbol)

    fun getScopeOwnerSymbol() = currentScope!!.scope.scopeOwnerSymbol

    override fun visitVariable(declaration: IrVariable): IrStatement {
        val initializer = declaration.initializer
        if (initializer == null || initializer !is IrCall) {
            return super.visitVariable(declaration)
        }
        return when (initializer.origin) {
            IrStatementOrigin.FOR_LOOP_ITERATOR ->
                processHeader(declaration)
            IrStatementOrigin.FOR_LOOP_NEXT ->
                processNext(declaration)
            else -> null
        } ?: super.visitVariable(declaration)
    }

    private fun processHeader(variable: IrVariable): IrStatement? {
        assert(variable.symbol !in iteratorToLoopHeader)
        val forLoopInfo = headerProcessor.processHeader(variable)
            ?: return null
        iteratorToLoopHeader[variable.symbol] = forLoopInfo
        return IrCompositeImpl(
            variable.startOffset,
            variable.endOffset,
            context.irBuiltIns.unitType,
            null,
            forLoopInfo.declarations
        )
    }

    /**
     * This loop
     *
     * for (i in first..last step foo) { ... }
     *
     * is represented in IR in such a manner:
     *
     * val it = (first..last step foo).iterator()
     * while (it.hasNext()) {
     *     val i = it.next()
     *     ...
     * }
     *
     * We transform it into the following loop:
     *
     * var it = first
     * if (it <= last) {  // (it >= last if the progression is decreasing)
     *     do {
     *         val i = it++
     *         ...
     *     } while (i != last)
     * }
     *
     * In case of iteration over array we transform it into following:
     * while (i <= array.size - 1) {
     *     val element = array[i]
     *     i++
     *     ...
     * }
     */
    // TODO:  Lower `for (i in a until b)` to loop with precondition: for (i = a; i < b; a++);
    override fun visitWhileLoop(loop: IrWhileLoop): IrExpression {
        if (loop.origin != IrStatementOrigin.FOR_LOOP_INNER_WHILE) {
            return super.visitWhileLoop(loop)
        }

        with(context.createIrBuilder(getScopeOwnerSymbol(), loop.startOffset, loop.endOffset)) {
            val newBody = loop.body?.transform(this@RangeLoopTransformer, null)?.let {
                if (it is IrContainerExpression && !it.isTransparentScope) {
                    IrCompositeImpl(startOffset, endOffset, it.type, it.origin, it.statements)
                } else {
                    it
                }
            }
            val loopHeader = getLoopHeader(loop.condition)
                ?: return super.visitWhileLoop(loop)
            val newLoop = loopHeader.buildInnerLoop(this, loop, newBody)
            oldLoopToNewLoop[loop] = newLoop

            // Surround the new loop with a check for an empty loop, if necessary.
            if (loopHeader.needsEmptinessCheck) {
                val notEmptyCondition = loopHeader.buildNotEmptyCondition(this@with)
                if (notEmptyCondition != null)
                    return irIfThen(notEmptyCondition, newLoop)
            }
            return newLoop
        }
    }

    private fun getLoopHeader(oldCondition: IrExpression): ForLoopHeader? {
        if (oldCondition !is IrCall || oldCondition.origin != IrStatementOrigin.FOR_LOOP_HAS_NEXT) {
            return null
        }
        val irIteratorAccess = oldCondition.dispatchReceiver as? IrGetValue
            ?: throw AssertionError()
        // Return null if we didn't lower the corresponding header.
        return iteratorToLoopHeader[irIteratorAccess.symbol]
    }

    // Lower getting a next induction variable value.
    fun processNext(variable: IrVariable): IrExpression? {
        val initializer = variable.initializer as IrCall

        val iterator = initializer.dispatchReceiver as? IrGetValue
            ?: throw AssertionError()
        val forLoopInfo = iteratorToLoopHeader[iterator.symbol]
            ?: return null  // If we didn't lower a corresponding header.
        // TODO: Use PLUS_ASSIGN to increment
        val plusOperator = symbols.getBinaryOperator(
            OperatorNameConventions.PLUS,
            forLoopInfo.inductionVariable.type.toKotlinType(),
            forLoopInfo.step.type.toKotlinType()
        )
        forLoopInfo.loopVariable = variable
        return with(context.createIrBuilder(getScopeOwnerSymbol(), initializer.startOffset, initializer.endOffset)) {
            variable.initializer = forLoopInfo.initializeLoopVariable(symbols, this)
            val increment = irSetVar(
                forLoopInfo.inductionVariable.symbol, irCallOp(
                    plusOperator, plusOperator.owner.returnType,
                    irGet(forLoopInfo.inductionVariable),
                    irGet(forLoopInfo.step)
                )
            )
            IrCompositeImpl(
                variable.startOffset,
                variable.endOffset,
                context.irBuiltIns.unitType,
                IrStatementOrigin.FOR_LOOP_NEXT,
                listOf(variable, increment)
            )
        }
    }
}