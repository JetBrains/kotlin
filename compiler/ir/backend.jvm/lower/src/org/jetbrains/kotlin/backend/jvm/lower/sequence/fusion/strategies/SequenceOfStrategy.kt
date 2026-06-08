/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.strategies

import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceData
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceSource
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irElseBranch
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols

/**
 * If we know that a sequence is a transformation of sequenceOf to which we know the arguments to,
 * we transform a loop into a block evaluating the loop body on each element of the sequence.
 * ```
 * val seq = sequenceOf(1, 2).map { it - 1 }
 * for (el in seq) println(el)
 * ```
 * becomes
 * ```
 * {
 * println({ it - 1 }(1))
 * println({ it - 1 }(2))
 * }
 * ```
 * */
internal class SequenceOfStrategy(val source: SequenceSource.SequenceOf) : LoweringStrategy() {
    override fun lowerLoop(
        builderWithParent: IrBuilderWithParent,
        loopBody: (IrVariable) -> IrContainerExpression,
        sequenceData: SequenceData,
        newLoop: IrLoop,
        loopVariable: IrVariable?,
    ): IrContainerExpression {
        val builder = builderWithParent.first

        val iteratorReplacement = createIteratorReplacement(builderWithParent)
        val newBody = builder.irBlock {
            // iteratorVariable++
            +iteratorReplacement.iteratorNextStatement
            +addReplacementsToBody(
                builderWithParent,
                loopBody,
                sequenceData,
                irGet(iteratorReplacement.outerLoopVariable),
                null,
                newLoop,
                loopVariable,
            )
        }
        return createLoweredLoop(
            iteratorReplacement.iteratorVariable,
            iteratorReplacement.outerLoopVariable,
            iteratorReplacement.condition,
            builder,
            newBody,
            sequenceData,
            newLoop
        )
    }

    override fun lowerFunction(
        builderWithParent: IrBuilderWithParent,
        function: IrRichFunctionReference,
        sequenceData: SequenceData
    ): IrExpression {
        val builder = builderWithParent.first
        val iteratorReplacement = createIteratorReplacement(builderWithParent)
        val newLoop = builder.createSequenceWhile()
        val newBody = builder.irBlock {
            // iteratorVariable++
            +iteratorReplacement.iteratorNextStatement
            +addReplacementsToForEachCall(
                builderWithParent,
                function,
                sequenceData,
                irGet(iteratorReplacement.outerLoopVariable),
                newLoop
            )
        }
        return createLoweredLoop(
            iteratorReplacement.iteratorVariable,
            iteratorReplacement.outerLoopVariable,
            iteratorReplacement.condition,
            builder,
            newBody,
            sequenceData,
            newLoop
        )
    }

    override fun prepareLoopBody(
        loopBody: IrBlock,
        builderWithParent: IrBuilderWithParent,
        oldLoopVariable: IrVariable,
        oldLoop: IrLoop?
    ): Pair<(IrVariable) -> IrContainerExpression, IrLoop> {
        return updateLoopVariableInBody(
            builderWithParent.first,
            oldLoopVariable,
            loopBody,
            oldLoop,
            builderWithParent.second
        )
    }


    private data class IteratorReplacement(
        val iteratorVariable: IrVariable,
        val outerLoopVariable: IrVariable,
        val iteratorNextStatement: IrStatement,
        val condition: IrExpression,
    )

    private fun createIteratorReplacement(
        builderWithParent: IrBuilderWithParent,
    ): IteratorReplacement {
        val builder = builderWithParent.first
        val iteratorVariable = builder.scope.createTemporaryVariable(
            builder.irInt(0),
            isMutable = true,
            origin = IrDeclarationOrigin.FOR_LOOP_ITERATOR,
            nameHint = "sequenceOfIterator"
        )
        val loopCondition = with(builder) {
            irCall(context.irBuiltIns.lessFunByOperandType[context.irBuiltIns.intClass]!!).apply {
                arguments[0] = irGet(iteratorVariable)
                arguments[1] = irInt(source.elements.size)
            }
        }
        val iteratorNextStatement = builder.irSet(
            iteratorVariable,
            builder.irCall(builder.context.irBuiltIns.intPlusSymbol).apply {
                dispatchReceiver = builder.irGet(iteratorVariable)
                arguments[1] = builder.irInt(1)
            },
        )
        val outerLoopVariable = builder.scope.createTemporaryVariable(
            generateWhen(builderWithParent, source.elements, source.type, iteratorVariable)
        )

        return IteratorReplacement(iteratorVariable, outerLoopVariable, iteratorNextStatement, loopCondition)
    }

    private fun generateWhen(
        builderWithParent: IrBuilderWithParent,
        elements: List<IrExpression>,
        returnedType: IrType,
        takeIteratorVariable: IrVariable
    ): IrExpression {
        val builder = builderWithParent.first
        with(builder) {
            val branches: MutableList<IrBranch> = elements.mapIndexed { index, element ->
                val elementCopy = element.deepCopyWithSymbols(builderWithParent.second)
                irBranch(irEquals(irGet(takeIteratorVariable), irInt(index)), elementCopy)
            }.toMutableList()
            branches.add(
                irElseBranch(
                    irCall(context.irBuiltIns.noWhenBranchMatchedExceptionSymbol)
                )
            )
            return irWhen(returnedType, branches)
        }
    }
}
