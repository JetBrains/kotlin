/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers

import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceData
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceReplacement
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.getGenericTypeFromExpression
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBreak
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols

internal class UnknownVariableStrategy(
    val newIteratorTarget: IrExpression,
    val context: JvmBackendContext
) : ProducerStrategy() {

    override fun fuseConsumer(
        builderWithParent: IrBuilderWithParent,
        sequenceData: SequenceData,
        sequenceReplacement: SequenceReplacement,
    ): IrContainerExpression? {
        val [builder, parent] = builderWithParent

        val baseType = getGenericTypeFromExpression(newIteratorTarget, context) ?: return null
        val iteratorType = builder.context.irBuiltIns.iteratorClass.typeWith(baseType)

        return builder.irBlock {
            val iteratorTarget = (newIteratorTarget as? IrGetValue)?.takeIf { (it.symbol.owner as? IrVariable)?.isVar != true }
                ?: irGet(
                    irTemporary(newIteratorTarget.deepCopyWithSymbols(parent), nameHint = "iteratorTarget", irType = newIteratorTarget.type)
                )
            +sequenceReplacement.initialDeclarations

            val iteratorCall = builderWithParent.buildCallWithReceiver(iteratorTarget, newIteratorTarget.type, "iterator")
                ?: return null
            val iteratorDeclaration = scope.createTemporaryVariable(
                iteratorCall,
                isMutable = false,
                nameHint = "replacementIterator",
                irType = iteratorType,
            ).apply { markAsSynthetic() }

            val loopCondition = builderWithParent.buildCallWithReceiver(irGet(iteratorDeclaration), iteratorType, "hasNext")
                ?: return null
            val loop = createSequenceWhile()
            val nextCall = builderWithParent.buildCallWithReceiver(irGet(iteratorDeclaration), iteratorType, "next")!!
            val outerLoopVariable = scope.createTemporaryVariable(
                nextCall,
                isMutable = false,
                nameHint = "outerLoopVariable",
                irType = baseType,
            ).apply { markAsSynthetic() }

            loop.apply {
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
            +iteratorDeclaration
            +loop
            +sequenceReplacement.finalExpression
        }
    }
}
