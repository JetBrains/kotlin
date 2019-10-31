/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

val forLoopsPhase = makeIrFilePhase(
    ::ForLoopsLowering,
    name = "ForLoopsLowering",
    description = "For loops lowering"
)

/**
 * This lowering pass optimizes for-loops.
 *
 * Replace iteration over progressions (e.g., X.indices, a..b) and arrays with
 * a simple while loop over primitive induction variable.
 *
 * For example, this loop:
 * ```
 *   for (loopVar in A..B) { // Loop body }
 * ```
 * is represented in IR in such a manner:
 * ```
 *   val it = (A..B).iterator()
 *   while (it.hasNext()) {
 *       val loopVar = it.next()
 *       // Loop body
 *   }
 * ```
 * We transform it into one of the following loops:
 * ```
 *   // 1. If the induction variable cannot overflow, i.e., `B` is const and != MAX_VALUE (if increasing, or MIN_VALUE if decreasing).
 *
 *   var inductionVar = A
 *   val last = B
 *   if (inductionVar <= last) {  // (`inductionVar >= last` if the progression is decreasing)
 *       // Loop is not empty
 *       do {
 *           val loopVar = inductionVar
 *           inductionVar++  // (`inductionVar--` if the progression is decreasing)
 *           // Loop body
 *       } while (inductionVar <= last)
 *   }
 *
 *   // 2. If the induction variable CAN overflow, i.e., `last` is not const or is MAX/MIN_VALUE:
 *
 *   var inductionVar = A
 *   val last = B
 *   if (inductionVar <= last) {  // (`inductionVar >= last` if the progression is decreasing)
 *       // Loop is not empty
 *       do {
 *           val loopVar = inductionVar
 *           inductionVar++  // (`inductionVar--` if the progression is decreasing)
 *           // Loop body
 *       } while (loopVar != last)
 *   }
 * ```
 * If loop is an until loop (e.g., `for (i in A until B)`), it is transformed into:
 * ```
 *   var inductionVar = A
 *   val last = B - 1
 *   if (inductionVar <= last && B != MIN_VALUE) {
 *       // Loop is not empty
 *       do {
 *           val loopVar = inductionVar
 *           inductionVar++
 *           // Loop body
 *       } while (inductionVar <= last)
 *   }
 * ```
 * In case of iteration over an array (e.g., `for (i in array)`), we transform it into the following:
 * ```
 *   var inductionVar = 0
 *   val last = array.size
 *   while (inductionVar < last) {
 *       val loopVar = array[inductionVar++]
 *       // Loop body
 *   }
 * ```
 */
class ForLoopsLowering(val context: CommonBackendContext) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        val oldLoopToNewLoop = mutableMapOf<IrLoop, IrLoop>()
        val transformer = RangeLoopTransformer(context, oldLoopToNewLoop)
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
    val oldLoopToNewLoop: MutableMap<IrLoop, IrLoop>
) : IrElementTransformerVoidWithContext() {

    private val symbols = context.ir.symbols
    private val iteratorToLoopHeader = mutableMapOf<IrVariableSymbol, ForLoopHeader>()
    private val headerInfoBuilder = HeaderInfoBuilder(context, this::getScopeOwnerSymbol)
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

    /**
     * Lowers the "header" statement that stores the iterator into the loop variable
     * (e.g., `val it = someIterable.iterator()`) and gather information for building the for-loop
     * (as a [ForLoopHeader]).
     *
     * Returns null if the for-loop cannot be lowered.
     */
    private fun processHeader(variable: IrVariable): IrStatement? {
        assert(variable.symbol !in iteratorToLoopHeader)
        val forLoopInfo = headerProcessor.processHeader(variable)
            ?: return null  // If the for-loop cannot be lowered.
        iteratorToLoopHeader[variable.symbol] = forLoopInfo

        // Lower into a composite with additional statements (e.g., induction variable) used in the loop condition and body.
        return IrCompositeImpl(
            variable.startOffset,
            variable.endOffset,
            context.irBuiltIns.unitType,
            null,
            forLoopInfo.loopInitStatements
        )
    }

    override fun visitWhileLoop(loop: IrWhileLoop): IrExpression {
        if (loop.origin != IrStatementOrigin.FOR_LOOP_INNER_WHILE) {
            return super.visitWhileLoop(loop)
        }

        with(context.createIrBuilder(getScopeOwnerSymbol(), loop.startOffset, loop.endOffset)) {
            // Visit the loop body to process the "next" statement and lower nested loops.
            // Processing the "next" statement is necessary for building loops that need to
            // reference the loop variable in the loop condition.
            val newBody = loop.body?.transform(this@RangeLoopTransformer, null)?.let {
                if (it is IrContainerExpression && !it.isTransparentScope) {
                    IrCompositeImpl(startOffset, endOffset, it.type, it.origin, it.statements)
                } else {
                    it
                }
            }

            val loopHeader = getLoopHeader(loop.condition)
                ?: return super.visitWhileLoop(loop)  // If the for-loop cannot be lowered.
            val (newLoop, replacementExpression) = loopHeader.buildLoop(this, loop, newBody)

            // Update mapping from old to new loop so we can later update references in break/continue.
            oldLoopToNewLoop[loop] = newLoop

            return replacementExpression
        }
    }

    private fun getLoopHeader(expression: IrExpression): ForLoopHeader? {
        if (expression !is IrCall
            || (expression.origin != IrStatementOrigin.FOR_LOOP_HAS_NEXT
                    && expression.origin != IrStatementOrigin.FOR_LOOP_NEXT)
        ) {
            return null
        }
        val iterator = expression.dispatchReceiver as IrGetValue

        // Return null if we didn't lower the corresponding header.
        return iteratorToLoopHeader[iterator.symbol]
    }

    /**
     * Lowers the "next" statement that stores the next element in the iterable into the
     * loop variable, e.g., `val i = it.next()`.
     *
     * Returns null if there was no stored [ForLoopHeader] corresponding to the given "next"
     * statement.
     */
    private fun processNext(variable: IrVariable): IrExpression? {
        val initializer = variable.initializer as IrCall
        val forLoopInfo = getLoopHeader(initializer)
            ?: return null  // If the for-loop cannot be lowered.

        // The "next" statement (at the top of the loop):
        //
        //   val i = it.next()
        //
        // ...is lowered into something like:
        //
        //   val i = inductionVariable  // For progressions, or `array[inductionVariable]` for arrays
        //   inductionVariable = inductionVariable + step
        return with(context.createIrBuilder(getScopeOwnerSymbol(), initializer.startOffset, initializer.endOffset)) {
            IrCompositeImpl(
                variable.startOffset,
                variable.endOffset,
                context.irBuiltIns.unitType,
                IrStatementOrigin.FOR_LOOP_NEXT,
                forLoopInfo.initializeIteration(variable, symbols, this)
            )
        }
    }
}