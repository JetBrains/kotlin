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
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.util.dump
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
    private val headerInfoBuilder = DefaultHeaderInfoBuilder(context, this::getScopeOwnerSymbol)
    private val headerProcessor = HeaderProcessor(context, headerInfoBuilder, this::getScopeOwnerSymbol)

    fun getScopeOwnerSymbol() = currentScope!!.scope.scopeOwnerSymbol

    override fun visitBlock(expression: IrBlock): IrExpression {
        // LoopExpressionGenerator in psi2ir lowers `for (loopVar in <someIterable>) { // Loop body }` into an IrBlock with origin FOR_LOOP.
        // This block has 2 statements:
        //
        //   // #1: The "header"
        //   val it = <someIterable>.iterator()
        //
        //   // #2: The inner while loop
        //   while (it.hasNext()) {
        //     val loopVar = it.next()
        //     // Loop body
        //   }
        //
        // We primarily need to determine HOW to optimize the for loop from the iterable expression in the header (e.g., if it's a
        // `withIndex()` call, a progression such as `10 downTo 1`). However in some cases (e.g., for `withIndex()`), we also need to
        // examine the while loop to determine if we CAN optimize the loop.
        if (expression.origin != IrStatementOrigin.FOR_LOOP) {
            return super.visitBlock(expression)  // Not a for-loop block.
        }

        with(expression.statements) {
            assert(size == 2) { "Expected 2 statements in for-loop block, was:\n${expression.dump()}" }
            val iteratorVariable = get(0) as IrVariable
            assert(iteratorVariable.origin == IrDeclarationOrigin.FOR_LOOP_ITERATOR) { "Expected FOR_LOOP_ITERATOR origin for iterator variable, was:\n${iteratorVariable.dump()}" }
            val loopHeader = headerProcessor.extractHeader(iteratorVariable)
                ?: return super.visitBlock(expression)  // The iterable in the header is not supported.
            val loweredHeader = lowerHeader(iteratorVariable, loopHeader)

            val oldLoop = get(1) as IrWhileLoop
            assert(oldLoop.origin == IrStatementOrigin.FOR_LOOP_INNER_WHILE) { "Expected FOR_LOOP_INNER_WHILE origin for while loop, was:\n${oldLoop.dump()}" }
            val (newLoop, loopReplacementExpression) = lowerWhileLoop(oldLoop, loopHeader)
                ?: return super.visitBlock(expression)  // Cannot lower the loop.

            // We can lower both the header and while loop.
            // Update mapping from old to new loop so we can later update references in break/continue.
            oldLoopToNewLoop[oldLoop] = newLoop

            set(0, loweredHeader)
            set(1, loopReplacementExpression)
        }

        return super.visitBlock(expression)
    }

    /**
     * Lowers the "header" statement that stores the iterator into the loop variable
     * (e.g., `val it = someIterable.iterator()`) and gather information for building the for-loop
     * (as a [ForLoopHeader]).
     *
     * Returns null if the for-loop cannot be lowered.
     */
    private fun lowerHeader(variable: IrVariable, loopHeader: ForLoopHeader): IrStatement {
        // Lower into a composite with additional statements (e.g., induction variable) used in the loop condition and body.
        return IrCompositeImpl(
            variable.startOffset,
            variable.endOffset,
            context.irBuiltIns.unitType,
            null,
            loopHeader.loopInitStatements
        )
    }

    private fun lowerWhileLoop(loop: IrWhileLoop, loopHeader: ForLoopHeader): LoopReplacement? {
        val loopBodyStatements = (loop.body as? IrContainerExpression)?.statements ?: return null
        val (mainLoopVariable, mainLoopVariableIndex, loopVariableComponents, loopVariableComponentIndices) = gatherLoopVariableInfo(
            loopBodyStatements
        )

        if (loopHeader.consumesLoopVariableComponents && mainLoopVariable.origin != IrDeclarationOrigin.IR_TEMPORARY_VARIABLE) {
            // We determine if there is a destructuring declaration by checking if the main loop variable is temporary.
            // This is somewhat brittle and depends on the implementation of LoopExpressionGenerator in psi2ir.
            //
            // 1. If the loop is `for ((i, v) in arr.withIndex() {}`), the loop body looks like this:
            //
            //     val tmp_loopParameter = it.next()   // origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
            //     val i = tmp_loopParameter.component1()
            //     val v = tmp_loopParameter.component2()
            //
            // 2. If the loop is `for (iv in arr.withIndex() { val (i, v) = iv }`), the loop body looks like this:
            //
            //     val iv = it.next()   // origin != IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
            //     val i = iv.component1()
            //     val v = iv.component2()
            //
            // 3. If the loop is `for ((_, _) in arr.withIndex() {}`), the loop body looks like this:
            //
            //     val tmp_loopParameter = it.next()   // origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
            //     // No component variables
            //
            // 4. If the loop is `for (iv in arr.withIndex() {}`), the loop body looks like this:
            //
            //     val iv = it.next()   // origin != IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
            //     // No component variables
            //
            // The only way to distinguish between #1 and #2, and between #3 and #4 is to check the origin of the main loop variable.
            // We need to distinguish between these because we intend to only optimize #1 and #3.
            return null
        }

        // The "next" statement (at the top of the loop):
        //
        //   val i = it.next()
        //
        // ...is lowered into something like:
        //
        //   val i = inductionVariable  // For progressions, or `array[inductionVariable]` for arrays
        //   inductionVariable = inductionVariable + step
        val initializer = mainLoopVariable.initializer as IrCall
        val replacement = with(context.createIrBuilder(getScopeOwnerSymbol(), initializer.startOffset, initializer.endOffset)) {
            IrCompositeImpl(
                mainLoopVariable.startOffset,
                mainLoopVariable.endOffset,
                context.irBuiltIns.unitType,
                IrStatementOrigin.FOR_LOOP_NEXT,
                loopHeader.initializeIteration(mainLoopVariable, loopVariableComponents, symbols, this)
            )
        }

        // Remove the main loop variable components if they are consumed in initializing the iteration.
        if (loopHeader.consumesLoopVariableComponents) {
            for (index in loopVariableComponentIndices.asReversed()) {
                assert(index > mainLoopVariableIndex)
                loopBodyStatements.removeAt(index)
            }
        }
        loopBodyStatements[mainLoopVariableIndex] = replacement

        // Variables in the loop body may be used in the loop condition, so ensure the body scope is transparent (i.e., an IrComposite).
        val newBody = loop.body?.let {
            if (it is IrContainerExpression && !it.isTransparentScope) {
                IrCompositeImpl(loop.startOffset, loop.endOffset, it.type, it.origin, it.statements)
            } else {
                it
            }
        }

        return loopHeader.buildLoop(context.createIrBuilder(getScopeOwnerSymbol(), loop.startOffset, loop.endOffset), loop, newBody)
    }

    private data class LoopVariableInfo(
        val mainLoopVariable: IrVariable,
        val mainLoopVariableIndex: Int,
        val loopVariableComponents: Map<Int, IrVariable>,
        val loopVariableComponentIndices: List<Int>
    )

    private fun gatherLoopVariableInfo(statements: MutableList<IrStatement>): LoopVariableInfo {
        // The "next" statement (at the top of the loop) looks something like:
        //
        //   val i = it.next()
        //
        // In the case of loops with a destructuring declaration (e.g., `for ((i, v) in arr.withIndex()`), the "next" statement includes
        // component variables:
        //
        //   val tmp_loopParameter = it.next()
        //   val i = tmp_loopParameter.component1()
        //   val v = tmp_loopParameter.component2()
        //
        // We find the main loop variable and all the component variables that are used to initialize the iteration.
        var mainLoopVariable: IrVariable? = null
        var mainLoopVariableIndex = -1
        val loopVariableComponents = mutableMapOf<Int, IrVariable>()
        val loopVariableComponentIndices = mutableListOf<Int>()
        for ((i, stmt) in statements.withIndex()) {
            if (stmt !is IrVariable) continue
            val initializer = stmt.initializer as? IrCall
            when (val origin = initializer?.origin) {
                IrStatementOrigin.FOR_LOOP_NEXT -> {
                    mainLoopVariable = stmt
                    mainLoopVariableIndex = i
                }
                is IrStatementOrigin.COMPONENT_N -> {
                    if (mainLoopVariable != null &&
                        (initializer.dispatchReceiver as? IrGetValue)?.symbol == mainLoopVariable.symbol
                    ) {
                        loopVariableComponents[origin.index] = stmt
                        loopVariableComponentIndices.add(i)
                    }
                }
            }
        }

        checkNotNull(mainLoopVariable) { "No 'next' statement in for-loop" }
        assert(mainLoopVariableIndex >= 0)

        return LoopVariableInfo(mainLoopVariable, mainLoopVariableIndex, loopVariableComponents, loopVariableComponentIndices)
    }
}