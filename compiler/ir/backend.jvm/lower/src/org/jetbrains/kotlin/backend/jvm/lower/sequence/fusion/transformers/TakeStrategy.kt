/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.transformers

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceReplacement
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.increment
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.functions

internal class TakeStrategy(val take: SequenceTransformer.Take, builderWithParent: IrBuilderWithParent, val context: JvmBackendContext) :
    TransformerStrategy(builderWithParent) {
    override fun addTransformerToBodyBuilder(
        sequenceReplacement: SequenceReplacement,
    ): SequenceReplacement {
        val builder = builderWithParent.first
        val counter = builderWithParent.createElementCounter(take.argument)
        val mainBodyBuilder = { sequenceVariable: IrValueDeclaration ->
            builder.irBlock {
                +increment(counter.count, this@TakeStrategy.context)
                val shouldContinue =
                    irTemporary(sequenceReplacement.mainBodyBuilder(sequenceVariable), nameHint = "shouldContinue")
                +irCall(context.irBuiltIns.andandSymbol).apply {
                    arguments[0] = irGet(shouldContinue)
                    arguments[1] = irIntLess(irGet(counter.count), irGet(counter.argument))
                }
            }
        }
        // the body stops the iteration after the `n`-th element; `take(0)` must not request any element at all
        val skipIteration = with(builder) {
            val takesNothing = irIntLessOrEqual(irGet(counter.argument), irInt(0))
            val condition = droppedEverything(counter.argument, sequenceReplacement.leadingDropCounts)
                ?.let { irOrOr(takesNothing, it) }
                ?: takesNothing
            orWithPrevious(sequenceReplacement.skipIteration, condition)
        }
        return SequenceReplacement(
            listOf(counter.count, counter.argument, counter.negativeArgumentCheck) + sequenceReplacement.initialDeclarations,
            skipIteration,
            mainBodyBuilder,
            sequenceReplacement.finalExpression,
            sequenceReplacement.leadingDropCounts,
        )
    }

    // `take(n).drop(m)` with `m >= n` is empty without iterating over the sequence at all
    private fun IrBuilderWithScope.droppedEverything(takeArgument: IrVariable, dropCounts: List<IrVariable>): IrExpression? {
        val intMinusSymbol = context.irBuiltIns.intClass.owner.functions.single {
            it.name.asString() == "minus" && it.parameters.getOrNull(1)?.type == context.irBuiltIns.intType
        }.symbol

        fun remainingBefore(index: Int): IrExpression =
            dropCounts.take(index).fold(irGet(takeArgument) as IrExpression) { remaining, drop ->
                irCall(intMinusSymbol).apply {
                    dispatchReceiver = remaining
                    arguments[1] = irGet(drop)
                }
            }
        return dropCounts.indices.map { index ->
            irIntLessOrEqual(remainingBefore(index), irGet(dropCounts[index]))
        }.reduceOrNull { result, check -> irOrOr(result, check) }
    }
}
