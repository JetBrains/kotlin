/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers

import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.GenerateSequenceInitialValue
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceData
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceReplacement
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceSource
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.callPredicate
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBreak
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.builders.irNotEquals
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols

internal class GenerateSequenceStrategy(
    val source: SequenceSource.GenerateSequence
) : ProducerStrategy() {

    override fun fuseConsumer(
        builderWithParent: IrBuilderWithParent,
        sequenceData: SequenceData,
        sequenceReplacement: SequenceReplacement,
    ): IrContainerExpression {
        val [builder, parent] = builderWithParent
        val generatingFunction = source.generatingFunction

        val oneArgumentIteratingFunction: (IrVariable) -> IrExpression = { variable ->
            builder.callPredicate(
                generatingFunction,
                parent,
                builder.irImplicitCast(builder.irGet(variable), source.sequenceElementType)
            )
        }

        val zeroArgumentIteratingFunction: (IrVariable) -> IrExpression = { _ ->
            builder.callPredicate(generatingFunction, parent)
        }

        val initialExpression = when (val initialValue = source.initialValue) {
            is GenerateSequenceInitialValue.Seed -> initialValue.expression.deepCopyWithSymbols(parent)
            is GenerateSequenceInitialValue.SeedFunction -> builder.callPredicate(initialValue.function, parent)
            is GenerateSequenceInitialValue.NextFunction -> builder.callPredicate(generatingFunction, parent)
        }
        val evaluateNext = when (source.initialValue) {
            is GenerateSequenceInitialValue.Seed -> oneArgumentIteratingFunction
            is GenerateSequenceInitialValue.SeedFunction -> oneArgumentIteratingFunction
            is GenerateSequenceInitialValue.NextFunction -> zeroArgumentIteratingFunction
        }

        return with(builder) {
            // the seed value is evaluated eagerly
            val isInitialValueEager = source.initialValue is GenerateSequenceInitialValue.Seed
            val stateVariable = scope.createTemporaryVariable(
                if (isInitialValueEager) initialExpression else irNull(),
                isMutable = true,
                irType = source.sequenceElementType.makeNullable(),
                nameHint = "generateSequenceState",
                origin = IrDeclarationOrigin.FOR_LOOP_ITERATOR
            )
            val loop = createSequenceWhile()

            val loopCondition = irNotEquals(irGet(stateVariable), irNull())

            loop.apply {
                condition = loopCondition

                body = irBlock {
                    +irBlock {
                        // condition already checked that stateVariable is not null
                        val element = irTemporary(
                            irImplicitCast(irGet(stateVariable), source.sequenceElementType),
                            nameHint = "generateSequenceElement",
                        )
                        val shouldContinueExpression = sequenceReplacement.mainBodyBuilder(element)
                        +irIfThen(context.irBuiltIns.unitType, irNot(shouldContinueExpression), irBreak(loop))
                    }
                    +irSet(stateVariable, evaluateNext(stateVariable))
                }
            }
            irBlock {
                +stateVariable
                +sequenceReplacement.initialDeclarations
                if (!isInitialValueEager) +irSet(stateVariable, initialExpression)
                +loop
                +sequenceReplacement.finalExpression
            }
        }
    }
}
