/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.consumers

import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceData
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.callRichFunctionReference
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.strategies.createSequenceWhile
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.dump

internal class FirstNotNullOfBodyReplacementCreator(
    val isOrNull: Boolean,
    val context: JvmBackendContext,
    val builder: IrBuilderWithScope,
    val parent: IrDeclarationParent,
    val sequenceData: SequenceData,
) : ConsumerBodyReplacementCreator() {
    override fun create(expression: IrCall): IrExpression? {
        val loop = builder.createSequenceWhile()
        val updateVariableBlock = createFirstBody(builder, loop)

        return firstLastDeclarations(
            expression,
            sequenceData,
            builder to parent,
            context
        ) { builder, parent, updatedSequenceData, strategy, transformLambda, resultVariable, skippedIterationVariable ->
            val transform = transformLambda?.let {
                { argument: IrExpression -> builder.callRichFunctionReference(it, parent, argument) }
            } ?: error("firstNotNullOf[orNull] found without transform argument: ${expression.dump()}")

            val oldBody = { loopVariable: IrVariable ->
                val resultValue = builder.scope.createTemporaryVariable(transform(builder.irGet(loopVariable)))
                val thenPart = builder.irBlock {
                    +irSet(skippedIterationVariable, builder.irFalse())
                    +updateVariableBlock(irGet(resultValue), resultVariable)
                }
                builder.irBlock {
                    +resultValue
                    +irIfThen(
                        context.irBuiltIns.unitType,
                        irNot(irEquals(irGet(resultValue), irNull())),
                        thenPart
                    )
                }
            }
            val newBody =
                createFirstLastBody(
                    strategy,
                    builder to parent,
                    oldBody,
                    updatedSequenceData,
                    loop,
                    skippedIterationVariable,
                    isOrNull,
                    context
                )
                    ?: return@firstLastDeclarations expression
            addResultGetValueToBody(expression, resultVariable, builder, newBody)
            newBody.type = expression.type
            newBody
        }
    }
}
