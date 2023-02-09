/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

/**
 * Contains the loop and expression to replace the old loop.
 *
 * @param newLoop The new loop.
 * @param replacementExpression The expression to use in place of the old loop. It is either `newLoop`, or a container
 * that contains `newLoop`.
 */
data class LoopReplacement(
    val newLoop: IrLoop,
    val replacementExpression: IrExpression
)

interface ForLoopHeader {
    /** Statements used to initialize the entire loop (e.g., declare induction variable). */
    val loopInitStatements: List<IrStatement>

    /**
     * Whether or not [initializeIteration] consumes the loop variable components assigned to it.
     * If true, the component variables should be removed from the un-lowered loop.
     */
    val consumesLoopVariableComponents: Boolean

    /** Statements used to initialize an iteration of the loop (e.g., assign loop variable). */
    fun initializeIteration(
        loopVariable: IrVariable?,
        loopVariableComponents: Map<Int, IrVariable>,
        builder: DeclarationIrBuilder,
        backendContext: CommonBackendContext,
    ): List<IrStatement>

    /** Builds a new loop from the old loop. */
    fun buildLoop(builder: DeclarationIrBuilder, oldLoop: IrLoop, newBody: IrExpression?): LoopReplacement
}

internal const val inductionVariableName = "inductionVariable"

fun IrStatement.isInductionVariable(context: CommonBackendContext) =
    this is IrVariable &&
            origin == context.inductionVariableOrigin &&
            name.asString() == inductionVariableName

internal class InitializerCallReplacer(private val replacement: IrExpression) : IrElementTransformerVoid() {
    var initializerCall: IrCall? = null

    override fun visitCall(expression: IrCall): IrExpression {
        if (initializerCall != null) {
            throw IllegalStateException(
                "Multiple initializer calls found. First: ${initializerCall!!.render()}\nSecond: ${expression.render()}"
            )
        }
        initializerCall = expression
        return replacement
    }
}

/**
 * Given the for-loop iterator variable, extract information about the iterable subject
 * and create a [ForLoopHeader] from it.
 */
internal class HeaderProcessor(
    private val context: CommonBackendContext,
    private val headerInfoBuilder: HeaderInfoBuilder,
    private val scopeOwnerSymbol: () -> IrSymbol
) {

    private val symbols = context.ir.symbols

    /**
     * Extracts information for building the for-loop (as a [ForLoopHeader]) from the given
     * "header" statement that stores the iterator into the loop variable
     * (e.g., `val it = someIterable.iterator()`).
     *
     * Returns null if the for-loop cannot be lowered.
     */
    fun extractHeader(variable: IrVariable): ForLoopHeader? {
        // Verify the variable type is a subtype of Iterator<*>.
        assert(variable.origin == IrDeclarationOrigin.FOR_LOOP_ITERATOR)
        if (!variable.type.isSubtypeOfClass(symbols.iterator)) {
            return null
        }

        // Get the iterable expression, e.g., `someIterable` in the following loop variable declaration:
        //
        //   val it = someIterable.iterator()
        val iteratorCall = variable.initializer as? IrCall
        val iterable = iteratorCall?.run {
            if (extensionReceiver != null) {
                extensionReceiver
            } else {
                dispatchReceiver
            }
        }

        // Collect loop information from the iterable expression.
        val headerInfo = iterable?.accept(headerInfoBuilder, iteratorCall)
            ?: return null  // If the iterable is not supported.

        val builder = context.createIrBuilder(scopeOwnerSymbol(), variable.startOffset, variable.endOffset)
        return when (headerInfo) {
            is IndexedGetHeaderInfo -> IndexedGetLoopHeader(headerInfo, builder, context)
            is ProgressionHeaderInfo -> ProgressionLoopHeader(headerInfo, builder, context)
            is WithIndexHeaderInfo -> WithIndexLoopHeader(headerInfo, builder, context)
            is IterableHeaderInfo -> IterableLoopHeader(headerInfo)
            is FloatingPointRangeHeaderInfo, is ComparableRangeInfo -> error("Unexpected ${headerInfo::class.simpleName} for loops")
        }
    }
}
