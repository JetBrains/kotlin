/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers

import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceData
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceReplacement
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceSource
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.increment
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irBreak
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irElseBranch
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.builders.irWhile
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols

/**
 * If we know that a sequence is a transformation of sequenceOf to which we know the arguments to,
 * we transform a loop into a block evaluating the loop body on each element of the sequence.
 * ```
 * val seq = sequenceOf(7, 5).map { it - 1 }
 * for (el in seq) println(el)
 * ```
 * becomes
 * ```
 * var n = 0
 * while(n < 2) {
 *     // Importantly, this generates the TABLESWITCH JVM instruction, not LOOKUPSWITCH
 *     val currentElement = when (n) {
 *         0 -> 7
 *         1 -> 5
 *         else -> throw NoBranchMatchedException
 *     }
 *     println(currentElement)
 * }
 * ```
 * */

internal class SequenceOfStrategy(
    val source: SequenceSource.SequenceOf
) : ProducerStrategy() {

    override fun fuseConsumer(
        builderWithParent: IrBuilderWithParent,
        sequenceData: SequenceData,
        sequenceReplacement: SequenceReplacement,
    ): IrContainerExpression {
        val builder = builderWithParent.first

        return builder.irBlock {
            val iteratorVariable = scope.createTemporaryVariable(
                irInt(0),
                isMutable = true,
                origin = IrDeclarationOrigin.FOR_LOOP_ITERATOR,
                nameHint = "sequenceOfIterator"
            )
            +iteratorVariable
            +sequenceReplacement.initialDeclarations

            val loopCondition = irCall(context.irBuiltIns.lessFunByOperandType[context.irBuiltIns.intClass]!!).apply {
                arguments[0] = irGet(iteratorVariable)
                arguments[1] = irInt(source.elements.size)
            }

            val loop = irWhile()
            val currentElementExpr = generateWhen(builderWithParent, source.elements, source.type, iteratorVariable)

            loop.apply {
                origin = IrStatementOrigin.WHILE_LOOP
                condition = loopCondition

                body = irBlock {
                    val currentElementVar = irTemporary(currentElementExpr, nameHint = "currentElement")
                    +irBlock {
                        val shouldContinueVar =
                            irTemporary(sequenceReplacement.mainBodyBuilder(currentElementVar), nameHint = "shouldContinue")
                        +irIfThen(context.irBuiltIns.unitType, irNot(irGet(shouldContinueVar)), irBreak(loop))
                    }
                    +builder.increment(iteratorVariable)
                }
            }


            +loop
            +sequenceReplacement.finalExpression
        }
    }

    private fun generateWhen(
        builderWithParent: IrBuilderWithParent,
        elements: List<IrExpression>,
        returnedType: IrType,
        iteratorVariable: IrVariable
    ): IrExpression {
        val builder = builderWithParent.first
        return with(builder) {
            val branches = buildList {
                elements.mapIndexedTo(this) { index, element ->
                    val elementCopy = element.deepCopyWithSymbols(builderWithParent.second)
                    elementCopy.markAsSynthetic()
                    irBranch(irEquals(irGet(iteratorVariable), irInt(index)), elementCopy)
                }
                val exceptionCall = irCall(context.irBuiltIns.noWhenBranchMatchedExceptionSymbol)
                add(irElseBranch(exceptionCall))
            }
            irWhen(returnedType, branches)
        }
    }
}
