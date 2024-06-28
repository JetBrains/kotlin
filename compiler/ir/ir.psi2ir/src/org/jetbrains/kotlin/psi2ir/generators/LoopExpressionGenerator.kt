/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.psi2ir.intermediate.VariableLValue
import org.jetbrains.kotlin.psi2ir.intermediate.setExplicitReceiverValue
import org.jetbrains.kotlin.resolve.BindingContext

internal class LoopExpressionGenerator(statementGenerator: StatementGenerator) : StatementGeneratorExtension(statementGenerator) {
    fun generateWhileLoop(ktWhile: KtWhileExpression): IrExpression {
        val irLoop = IrWhileLoopImpl(
            ktWhile.startOffsetSkippingComments, ktWhile.endOffset,
            context.irBuiltIns.unitType, IrStatementOrigin.WHILE_LOOP
        )

        irLoop.condition = ktWhile.condition!!.genExpr()

        statementGenerator.bodyGenerator.putLoop(ktWhile, irLoop)

        irLoop.label = getLoopLabel(ktWhile)

        irLoop.body = ktWhile.body?.let { ktLoopBody ->
            if (ktLoopBody is KtBlockExpression)
                generateWhileLoopBody(ktLoopBody)
            else
                ktLoopBody.genExpr()
        }

        return irLoop
    }

    fun generateDoWhileLoop(ktDoWhile: KtDoWhileExpression): IrExpression {
        val irLoop = IrDoWhileLoopImpl(
            ktDoWhile.startOffsetSkippingComments, ktDoWhile.endOffset,
            context.irBuiltIns.unitType, IrStatementOrigin.DO_WHILE_LOOP
        )

        statementGenerator.bodyGenerator.putLoop(ktDoWhile, irLoop)

        irLoop.label = getLoopLabel(ktDoWhile)

        irLoop.body = ktDoWhile.body?.let { ktLoopBody ->
            if (ktLoopBody is KtBlockExpression)
                generateDoWhileLoopBody(ktLoopBody)
            else
                ktLoopBody.genExpr()
        }

        irLoop.condition = ktDoWhile.condition!!.genExpr()

        return IrBlockImpl(ktDoWhile.startOffsetSkippingComments, ktDoWhile.endOffset, context.irBuiltIns.unitType).apply {
            statements.add(irLoop)
        }
    }

    private fun generateWhileLoopBody(ktLoopBody: KtBlockExpression): IrExpression =
        IrBlockImpl(
            ktLoopBody.startOffsetSkippingComments, ktLoopBody.endOffset, context.irBuiltIns.unitType, null,
            ktLoopBody.statements.map { it.genStmt() }
        )


    private fun generateDoWhileLoopBody(ktLoopBody: KtBlockExpression): IrExpression =
        IrCompositeImpl(
            ktLoopBody.startOffsetSkippingComments, ktLoopBody.endOffset, context.irBuiltIns.unitType, null,
            ktLoopBody.statements.map { it.genStmt() }
        )

    fun generateBreak(ktBreak: KtBreakExpression): IrExpression {
        val parentLoop = findParentLoop(ktBreak)
            ?: return ErrorExpressionGenerator(statementGenerator).generateErrorExpression(
                ktBreak, RuntimeException("Loop not found for break expression: ${ktBreak.text}")
            )
        return IrBreakImpl(
            ktBreak.startOffsetSkippingComments, ktBreak.endOffset, context.irBuiltIns.nothingType, parentLoop
        ).apply {
            label = parentLoop.label.takeIf { ktBreak.getLabelName() != null }
        }
    }

    fun generateContinue(ktContinue: KtContinueExpression): IrExpression {
        val parentLoop = findParentLoop(ktContinue)
            ?: return ErrorExpressionGenerator(statementGenerator).generateErrorExpression(
                ktContinue, RuntimeException("Loop not found for continue expression: ${ktContinue.text}")
            )
        return IrContinueImpl(
            ktContinue.startOffsetSkippingComments, ktContinue.endOffset, context.irBuiltIns.nothingType, parentLoop
        ).apply {
            label = parentLoop.label.takeIf { ktContinue.getLabelName() != null }
        }
    }

    private fun getLoopLabel(ktLoop: KtLoopExpression): String? =
        (ktLoop.parent as? KtLabeledExpression)?.getLabelName()

    private fun findParentLoop(ktWithLabel: KtExpressionWithLabel): IrLoop? =
        findParentLoop(ktWithLabel, ktWithLabel.getLabelName())

    private fun findParentLoop(ktExpression: KtExpression, targetLabel: String?): IrLoop? {
        var finger: KtExpression? = ktExpression
        BY_LOOP_EXPRESSIONS@ while (finger != null) {
            finger = finger.getParentOfType<KtLoopExpression>(true)
            if (finger == null) {
                break
            }
            if (targetLabel == null) {
                return getLoop(finger) ?: continue@BY_LOOP_EXPRESSIONS
            } else {
                var parent = finger.parent
                while (parent is KtLabeledExpression) {
                    val label = parent.getLabelName()!!
                    if (targetLabel == label) {
                        return getLoop(finger) ?: continue@BY_LOOP_EXPRESSIONS
                    }
                    parent = parent.parent
                }
            }
        }
        return null
    }

    private fun getLoop(ktLoop: KtLoopExpression): IrLoop? {
        return statementGenerator.bodyGenerator.getLoop(ktLoop)
    }

    fun generateForLoop(ktFor: KtForExpression): IrExpression {
        val ktLoopParameter = ktFor.loopParameter
        val ktLoopDestructuringDeclaration = ktFor.destructuringDeclaration
        if (ktLoopParameter == null && ktLoopDestructuringDeclaration == null) {
            throw AssertionError("Either loopParameter or destructuringParameter should be present:\n${ktFor.text}")
        }

        val ktLoopRange = ktFor.loopRange!!
        val ktForBody = ktFor.body
        val iteratorResolvedCall = getOrFail(BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL, ktLoopRange)
        val hasNextResolvedCall = getOrFail(BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL, ktLoopRange)
        val nextResolvedCall = getOrFail(BindingContext.LOOP_RANGE_NEXT_RESOLVED_CALL, ktLoopRange)

        val callGenerator = CallGenerator(statementGenerator)

        val startOffset = ktFor.startOffsetSkippingComments
        val endOffset = ktFor.endOffset

        val irForBlock = IrBlockImpl(startOffset, endOffset, context.irBuiltIns.unitType, IrStatementOrigin.FOR_LOOP)

        val iteratorCall = statementGenerator.pregenerateCall(iteratorResolvedCall)
        val irIteratorCall = callGenerator.generateCall(ktLoopRange, iteratorCall, IrStatementOrigin.FOR_LOOP_ITERATOR)
        val irIterator = scope.createTemporaryVariable(irIteratorCall, "iterator", origin = IrDeclarationOrigin.FOR_LOOP_ITERATOR)
        val iteratorValue = VariableLValue(context, irIterator)
        irForBlock.statements.add(irIterator)

        val irInnerWhile = IrWhileLoopImpl(startOffset, endOffset, context.irBuiltIns.unitType, IrStatementOrigin.FOR_LOOP_INNER_WHILE)
        irInnerWhile.label = getLoopLabel(ktFor)
        statementGenerator.bodyGenerator.putLoop(ktFor, irInnerWhile)
        irForBlock.statements.add(irInnerWhile)

        val hasNextCall = statementGenerator.pregenerateCall(hasNextResolvedCall)
        hasNextCall.setExplicitReceiverValue(iteratorValue)
        val irHasNextCall = callGenerator.generateCall(ktLoopRange, hasNextCall, IrStatementOrigin.FOR_LOOP_HAS_NEXT)
        irInnerWhile.condition = irHasNextCall

        val irInnerBody = IrBlockImpl(startOffset, endOffset, context.irBuiltIns.unitType, IrStatementOrigin.FOR_LOOP_INNER_WHILE)
        irInnerWhile.body = irInnerBody

        val nextCall = statementGenerator.pregenerateCall(nextResolvedCall)
        nextCall.setExplicitReceiverValue(iteratorValue)
        val irNextCall = callGenerator.generateCall(ktLoopRange, nextCall, IrStatementOrigin.FOR_LOOP_NEXT)
        val irLoopParameter =
            if (ktLoopParameter != null && ktLoopDestructuringDeclaration == null) {
                val loopParameter = getOrFail(BindingContext.VALUE_PARAMETER, ktLoopParameter)
                context.symbolTable.descriptorExtension.declareVariable(
                    ktLoopParameter.startOffsetSkippingComments, ktLoopParameter.endOffset, IrDeclarationOrigin.FOR_LOOP_VARIABLE,
                    loopParameter, loopParameter.type.toIrType(),
                    irNextCall
                )
            } else {
                scope.createTemporaryVariable(irNextCall, "loop_parameter")
            }
        irInnerBody.statements.add(irLoopParameter)

        if (ktLoopDestructuringDeclaration != null) {
            val firstContainerValue = VariableLValue(context, irLoopParameter)
            statementGenerator.declareComponentVariablesInBlock(
                ktLoopDestructuringDeclaration,
                irInnerBody,
                firstContainerValue,
                if (context.extensions.debugInfoOnlyOnVariablesInDestructuringDeclarations) {
                    VariableLValue(context, irLoopParameter, startOffset = SYNTHETIC_OFFSET, endOffset = SYNTHETIC_OFFSET)
                } else {
                    firstContainerValue
                }
            )
        }

        if (ktForBody != null) {
            irInnerBody.statements.add(ktForBody.genExpr())
        }

        return irForBlock
    }
}
