/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.consumers

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceData
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.callRichFunctionReference
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.strategies.createSequenceWhile
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop

internal class FirstLastBodyReplacementCreator(
    val isOrNull: Boolean,
    val isFirst: Boolean,
    val context: JvmBackendContext,
    val builder: IrBuilderWithScope,
    val parent: IrDeclarationParent,
    val sequenceData: SequenceData
) :
    ConsumerBodyReplacementCreator() {

    private fun handleFirstLast(
        builderWithParent: IrBuilderWithParent,
        expression: IrCall,
        sequenceData: SequenceData,
        updateVariableBlock: (IrExpression, IrVariable) -> IrExpression,
        loop: IrLoop,
    ): IrExpression? {
        return firstLastDeclarations(
            expression,
            sequenceData,
            builderWithParent,
            context
        ) { builder, parent, updatedSequenceData, strategy, predicateLambda, resultVariable, skippedIterationVariable ->
            val predicate = predicateLambda?.let {
                { argument: IrExpression -> builder.callRichFunctionReference(it, parent, argument) }
            }
            val oldBody = { loopVariable: IrVariable ->
                val thenPart = builder.irBlock {
                    +irSet(skippedIterationVariable, builder.irFalse())
                    +updateVariableBlock(irGet(loopVariable), resultVariable)
                }
                builder.irBlock {
                    if (predicate != null) {
                        +irIfThen(
                            context.irBuiltIns.unitType,
                            predicate(irGet(loopVariable)),
                            thenPart
                        )
                    } else +thenPart
                }
            }
            val newBody =
                createFirstLastBody(
                    strategy,
                    builderWithParent,
                    oldBody,
                    updatedSequenceData,
                    loop,
                    skippedIterationVariable,
                    isOrNull,
                    context
                )
                    ?: return null
            addResultGetValueToBody(expression, resultVariable, builder, newBody)
            newBody.type = expression.type
            return newBody
        }
    }

    override fun create(expression: IrCall): IrExpression? {
        return if (isFirst) {
            val loop = builder.createSequenceWhile()
            handleFirstLast(builder to parent, expression, sequenceData, createFirstBody(builder, loop), loop)
        } else {
            val loop = builder.createSequenceWhile()
            handleFirstLast(builder to parent, expression, sequenceData, createLastBody(builder), loop)
        }
    }
}
