/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers

import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceData
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceReplacement
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.getGenericTypeFromExpression
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBreak
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irWhile
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.typeWith

internal class UnknownVariableStrategy(
    val newIteratorTarget: IrExpression
) : ProducerStrategy() {

    override fun fuseConsumer(
        builderWithParent: IrBuilderWithParent,
        sequenceData: SequenceData,
        sequenceReplacement: SequenceReplacement,
    ): IrContainerExpression? {
        val builder = builderWithParent.first
        val parent = builderWithParent.second

        val baseType = getGenericTypeFromExpression(newIteratorTarget) ?: return null
        val iteratorType = builder.context.irBuiltIns.iteratorClass.typeWith(baseType)

        return builder.irBlock {
            val iteratorCall = builder.buildCallWithReceiver(newIteratorTarget, newIteratorTarget.type, "iterator", parent)
                ?: return null
            val iteratorDeclaration = scope.createTemporaryVariable(
                iteratorCall,
                isMutable = false,
                nameHint = "replacementIterator",
                irType = iteratorType,
            ).apply { markAsSynthetic() }

            +iteratorDeclaration
            +sequenceReplacement.initialDeclarations

            val loopCondition = builder.buildCallWithReceiver(irGet(iteratorDeclaration), iteratorType, "hasNext", parent)
                ?: return null
            val loop = irWhile()
            val nextCall = builder.buildCallWithReceiver(irGet(iteratorDeclaration), iteratorType, "next", parent)!!
            val outerLoopVariable = scope.createTemporaryVariable(
                nextCall,
                isMutable = false,
                nameHint = "outerLoopVariable",
                irType = baseType,
            ).apply { markAsSynthetic() }

            loop.apply {
                origin = IrStatementOrigin.WHILE_LOOP
                condition = loopCondition

                body = irBlock {
                    +outerLoopVariable
                    +irBlock {
                        val shouldContinueVar =
                            irTemporary(sequenceReplacement.mainBodyBuilder(outerLoopVariable), nameHint = "shouldContinue")
                        +irIfThen(builder.context.irBuiltIns.unitType, irNot(irGet(shouldContinueVar)), irBreak(loop))
                    }
                }
            }
            +loop
            +sequenceReplacement.finalExpression
        }
    }
}
