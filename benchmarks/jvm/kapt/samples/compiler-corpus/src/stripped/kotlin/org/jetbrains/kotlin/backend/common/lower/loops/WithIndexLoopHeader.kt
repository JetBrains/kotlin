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
import org.jetbrains.kotlin.ir.util.hasShape
import org.jetbrains.kotlin.util.OperatorNameConventions

class WithIndexLoopHeader(
    headerInfo: WithIndexHeaderInfo,
    builder: DeclarationIrBuilder,
    context: CommonBackendContext
) : ForLoopHeader {

    val nestedLoopHeader: ForLoopHeader
    val indexVariable: IrVariable
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
                indexVariable = scope.createTemporaryVariable(
                    irInt(0),
                    nameHint = "index",
                    isMutable = true,
                    inventUniqueName = false,
                )
                ownsIndexVariable = true
                // `index++` during iteration initialization
                // TODO: KT-34665: Check for overflow for Iterable and Sequence (call to checkIndexOverflow()).
                val plusFun = indexVariable.type.getClass()!!.functions.first {
                    it.name == OperatorNameConventions.PLUS &&
                            it.hasShape(
                                dispatchReceiver = true,
                                regularParameters = 1,
                                parameterTypes = listOf(null, context.irBuiltIns.intType)
                            )
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
        loopVariables: List<IrVariable>,
        loopVariableComponents: Map<Int, List<IrVariable>>,
        builder: DeclarationIrBuilder,
        backendContext: CommonBackendContext,
    ): List<IrStatement> = TODO()

    // Use the nested loop header to build the loop. More info in comments in initializeIteration().
    override fun buildLoop(builder: DeclarationIrBuilder, oldLoop: IrLoop, newBody: IrExpression?) =
        nestedLoopHeader.buildLoop(builder, oldLoop, newBody)
}