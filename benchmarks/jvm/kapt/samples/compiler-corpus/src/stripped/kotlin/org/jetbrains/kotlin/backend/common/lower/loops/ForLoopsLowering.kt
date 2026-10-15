/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * This lowering pass optimizes for-loops.
 *
 * Replace iteration over progressions (e.g., `X.indices`, `a..b`) and arrays with
 * a simple while loop over primitive induction variable.
 *
 * For example, this loop:
 * ```
 *   for (loopVar in A..B) { /* Loop body */ }
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
 * If loop is an until loop (e.g., `for (i in A until B)` or `for (i in A..<B)`, it is transformed into:
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
open class ForLoopsLowering(val context: CommonBackendContext) : BodyLoweringPass {
    open val loopBodyTransformer: ForLoopBodyTransformer?
        get() = null

    override fun lower(irBody: IrBody, container: IrDeclaration) { TODO() }
}

/**
 * Abstract class for additional for-loop bodies transformations.
 */
abstract class ForLoopBodyTransformer : IrElementTransformerVoid() {

    abstract fun transform(
        context: CommonBackendContext,
        loopBody: IrExpression,
        loopVariable: IrVariable,
        forLoopHeader: ForLoopHeader,
        loopComponents: Map<Int, List<IrVariable>>
    )
}

private class RangeLoopTransformer(
    val context: CommonBackendContext,
    val container: IrSymbolOwner,
    val oldLoopToNewLoop: MutableMap<IrLoop, IrLoop>,
    val loopBodyTransformer: ForLoopBodyTransformer? = null
) : IrElementTransformerVoidWithContext() {

    private val headerInfoBuilder = DefaultHeaderInfoBuilder(context, this::getScopeOwnerSymbol)
    private val headerProcessor = HeaderProcessor(context, headerInfoBuilder, this::getScopeOwnerSymbol)

    fun getScopeOwnerSymbol() = currentScope?.scope?.scopeOwnerSymbol ?: container.symbol

    override fun visitBlock(expression: IrBlock): IrExpression { TODO() }

    /**
     * Lowers the "header" statement that stores the iterator into the loop variable
     * (e.g., `val it = someIterable.iterator()`) and gather information for building the for-loop
     * (as a [ForLoopHeader]).
     *
     * Returns null if the for-loop cannot be lowered.
     */
    private fun lowerHeader(variable: IrVariable, loopHeader: ForLoopHeader): IrStatement { TODO() }

    private fun lowerWhileLoop(loop: IrWhileLoop, loopHeader: ForLoopHeader): LoopReplacement? { TODO() }

    /**
     * This optimization is for the stdlib extension function in package `kotlin.collections`:
     * ```
     *      @kotlin.internal.InlineOnly
     *      public inline operator fun <T> Iterator<T>.iterator(): Iterator<T> = this
     * ```
     * Let's say we have an instance of `MyIterator`, which directly implements [kotlin.collections.Iterator],
     * when it is used in a for-loop like:
     *
     * ```
     *      val iterator = MyIterator()
     *      for (x in iterator)
     *          println(x)
     * ```
     * Without this optimization, receiver type of call of `next` would be Iterator<T> instead of MyIterator, which means that
     * a less specific method would be called, which could lead to unnecessary boxing of primitives or inline classes.
     */
    private fun specializeIteratorIfPossible(irForLoopBlock: IrContainerExpression) { TODO() }

    private data class LoopVariableInfo(
        val mainLoopVariable: IrVariable,
        val mainLoopVariableIndex: Int,
        val loopVariableComponents: Map<Int, List<IrVariable>>,
        val loopVariableComponentIndices: List<Int>
    )

    private class FindInitializerCallVisitor(private val mainLoopVariable: IrVariable?) : IrVisitorVoid() {
        var initializerCall: IrCall? = null

        override fun visitElement(element: IrElement) { TODO() }

        override fun visitCall(expression: IrCall) { TODO() }
    }

    private fun gatherLoopVariableInfo(statements: MutableList<IrStatement>): LoopVariableInfo? { TODO() }

    companion object {
        val STDLIB_ITERATOR_FUNCTION_FQ_NAME = FqName("kotlin.collections.CollectionsKt.iterator")
        val STDLIB_INDEXED_VALUE_GET_INDEX_NAME = FqName("kotlin.collections.IndexedValue.<get-index>")
        val STDLIB_INDEXED_VALUE_GET_VALUE_NAME = FqName("kotlin.collections.IndexedValue.<get-value>")
    }
}
