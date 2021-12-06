/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.util.OperatorNameConventions

class WithIndexLoopHeader(
    headerInfo: WithIndexHeaderInfo,
    builder: DeclarationIrBuilder,
    context: CommonBackendContext
) : ForLoopHeader {

    private val nestedLoopHeader: ForLoopHeader
    private val indexVariable: IrVariable
    private val ownsIndexVariable: Boolean
    private val incrementIndexStatement: IrStatement?

    init {
        with(builder) {
            // To build the optimized/lowered `for` loop over a `withIndex()` call, we first need the header for the underlying iterable,
            // so that we know how to build the loop for that iterable. More info in comments in initializeIteration().
            nestedLoopHeader = when (val nestedInfo = headerInfo.nestedInfo) {
                is IndexedGetHeaderInfo -> IndexedGetLoopHeader(nestedInfo, this@with, context)
                is ProgressionHeaderInfo -> ProgressionLoopHeader(nestedInfo, this@with, context)
                is IterableHeaderInfo -> IterableLoopHeader(nestedInfo)
                is WithIndexHeaderInfo -> throw IllegalStateException("Nested WithIndexHeaderInfo not allowed for WithIndexLoopHeader")
                is FloatingPointRangeHeaderInfo, is ComparableRangeInfo -> error("Unexpected ${nestedInfo::class.simpleName} for loops")
            }

            // Do not build own indexVariable if the nested loop header has an inductionVariable == 0 and step == 1.
            // This is the case when the underlying iterable is an array, CharSequence, or a progression from 0 with step 1.
            // We can use the induction variable from the underlying iterable as the index variable, since it progresses in the same way.
            if (nestedLoopHeader is NumericForLoopHeader<*> &&
                nestedLoopHeader.inductionVariable.type.isInt() &&
                nestedLoopHeader.inductionVariable.initializer?.constLongValue == 0L &&
                nestedLoopHeader.stepExpression.constLongValue == 1L
            ) {
                indexVariable = nestedLoopHeader.inductionVariable
                ownsIndexVariable = false
                incrementIndexStatement = null
            } else {
                indexVariable = scope.createTmpVariable(
                    irInt(0),
                    nameHint = "index",
                    isMutable = true
                )
                ownsIndexVariable = true
                // `index++` during iteration initialization
                // TODO: KT-34665: Check for overflow for Iterable and Sequence (call to checkIndexOverflow()).
                val plusFun = indexVariable.type.getClass()!!.functions.first {
                    it.name == OperatorNameConventions.PLUS &&
                            it.valueParameters.size == 1 &&
                            it.valueParameters[0].type.isInt()
                }
                incrementIndexStatement =
                    irSet(
                        indexVariable.symbol, irCallOp(
                            plusFun.symbol, plusFun.returnType,
                            irGet(indexVariable),
                            irInt(1)
                        )
                    )
            }
        }
    }

    // Add the index variable (if owned) to the statements from the nested loop header.
    override val loopInitStatements = nestedLoopHeader.loopInitStatements.let { if (ownsIndexVariable) it + indexVariable else it }

    override val consumesLoopVariableComponents = true

    override fun initializeIteration(
        loopVariable: IrVariable?,
        loopVariableComponents: Map<Int, IrVariable>,
        builder: DeclarationIrBuilder,
        backendContext: CommonBackendContext,
    ): List<IrStatement> =
        with(builder) {
            // The `withIndex()` extension function returns a lazy Iterable that wraps each element of the underlying iterable (e.g., array,
            // progression, Iterable, Sequence, CharSequence) into an IndexedValue containing the index of that element and the element
            // itself. The iterator for this lazy Iterable looks like this:
            //
            //   internal class IndexingIterator<out T>(private val iterator: Iterator<T>) : Iterator<IndexedValue<T>> {
            //     private var index = 0
            //     override fun hasNext() = iterator.hasNext()
            //     override fun next() = IndexedValue(checkIndexOverflow(index++), iterator.next())
            //   }
            //
            // IndexedValue looks like this:
            //
            //   data class IndexedValue<out T>(val index: Int, val value: T)
            //
            // For example, if the `for` loop is:
            //
            //   for ((i, v) in (1..10 step 2).withIndex()) { /* Loop body */ }
            //
            // ...the optimized loop for the underlying progression looks something like this:
            //
            //   var inductionVar = 1
            //   val last = 10
            //   val step = 2
            //   if (inductionVar <= last) {
            //     do {
            //       val v = inductionVar
            //       inductionVar += step
            //       // Loop body
            //     } while (inductionVar <= last)
            //   }
            //
            // ...and the optimized loop with `withIndex()` looks something like this (see "// ADDED" statements):
            //
            //   var inductionVar = 1
            //   val last = 10
            //   val step = 2
            //   var index = 0   // ADDED
            //   if (inductionVar <= last) {
            //     do {
            //       val i = index   // ADDED
            //       checkIndexOverflow(index++)   // ADDED
            //       val v = inductionVar
            //       inductionVar += step
            //       // Loop body
            //     } while (inductionVar <= last)
            //   }
            //
            // As another example, in a for-loop over a call to `Iterable<*>.withIndex()` or `Sequence<*>.withIndex()`, e.g.:
            //
            //   for ((i, v) in listOf(2, 3, 5, 7, 11).withIndex()) { /* Loop body */ }
            //
            // For-loops over an Iterable are normally not optimized, but when getting the underlying iterable for `withIndex()` (and ONLY
            // in this case), we use DefaultIterableHandler to match it and IterableLoopHeader to build the underlying loop. The optimized
            // loop with `withIndex()` looks something like this:
            //
            //   val iterator = listOf(2, 3, 5, 7, 11).iterator()
            //   var index = 0
            //   while (it.hasNext())
            //     val i = index
            //     checkIndexOverflow(index++)
            //     val v = it.next()
            //     // Loop body
            //   }
            //
            // We "wire" the 1st destructured component to index, and the 2nd to the loop variable value from the underlying iterable.
            loopVariableComponents[1]?.initializer = irGet(indexVariable)
            listOfNotNull(loopVariableComponents[1], incrementIndexStatement) +
                    nestedLoopHeader.initializeIteration(loopVariableComponents[2], linkedMapOf(), builder, backendContext)
        }

    // Use the nested loop header to build the loop. More info in comments in initializeIteration().
    override fun buildLoop(builder: DeclarationIrBuilder, oldLoop: IrLoop, newBody: IrExpression?) =
        nestedLoopHeader.buildLoop(builder, oldLoop, newBody)
}