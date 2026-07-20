/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers

import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.GenerateSequenceInitialValue
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceData
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceReplacement
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceSource
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.callPredicate
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBreak
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irNotEquals
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irWhile
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

internal fun IrBuilderWithScope.irAsNotNull(expression: IrExpression): IrExpression {
    val type = expression.type.makeNotNull()
    return irCall(context.irBuiltIns.checkNotNullSymbol, type).apply {
        typeArguments[0] = type
        arguments.assignFrom(listOf(expression))
    }
}

internal class GenerateSequenceStrategy(
    val source: SequenceSource.GenerateSequence
) : ProducerStrategy() {

    override fun fuseConsumer(
        builderWithParent: IrBuilderWithParent,
        sequenceData: SequenceData,
        sequenceReplacement: SequenceReplacement,
    ): IrContainerExpression {
        val builder = builderWithParent.first
        val parent = builderWithParent.second
        val generatingFunction = source.generatingFunction

        val oneArgumentIteratingFunction: (IrVariable) -> IrExpression = { variable ->
            builder.callPredicate(
                generatingFunction,
                parent,
                builder.irAsNotNull(builder.irGet(variable))
            )
        }

        val zeroArgumentIteratingFunction: (IrVariable) -> IrExpression = { _ ->
            builder.callPredicate(generatingFunction, parent)
        }

        val initialExpression = when (val initialValue = source.initialValue) {
            is GenerateSequenceInitialValue.InitialValue -> initialValue.expression.deepCopyWithSymbols(parent)
            is GenerateSequenceInitialValue.InitialFunction -> builder.callPredicate(initialValue.function, parent)
            is GenerateSequenceInitialValue.NoInitialValue -> builder.callPredicate(generatingFunction, parent)
        }
        val evaluateNext = when (source.initialValue) {
            is GenerateSequenceInitialValue.InitialValue -> oneArgumentIteratingFunction
            is GenerateSequenceInitialValue.InitialFunction -> oneArgumentIteratingFunction
            is GenerateSequenceInitialValue.NoInitialValue -> zeroArgumentIteratingFunction
        }

        return with(builder) {
            val stateVariable = scope.createTemporaryVariable(
                initialExpression,
                isMutable = true,
                irType = source.sequenceElementType.makeNullable(),
                nameHint = "generateSequenceState",
                origin = IrDeclarationOrigin.FOR_LOOP_ITERATOR
            )
            val loop = irWhile()

            val loopCondition = irNotEquals(irGet(stateVariable), irNull())

            loop.apply {
                origin = IrStatementOrigin.WHILE_LOOP
                condition = loopCondition

                body = irBlock {
                    +irBlock {
                        val shouldContinueVar = irTemporary(sequenceReplacement.mainBodyBuilder(stateVariable), nameHint = "shouldContinue")
                        +irIfThen(context.irBuiltIns.unitType, irNot(irGet(shouldContinueVar)), irBreak(loop))
                    }
                    +irSet(stateVariable, evaluateNext(stateVariable))
                }
            }
            irBlock {
                +stateVariable
                +sequenceReplacement.initialDeclarations
                +loop
                +sequenceReplacement.finalExpression
            }
        }
    }
}
