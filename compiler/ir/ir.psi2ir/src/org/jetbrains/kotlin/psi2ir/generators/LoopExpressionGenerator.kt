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
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.intermediate.VariableLValue
import org.jetbrains.kotlin.psi2ir.intermediate.setExplicitReceiverValue
import org.jetbrains.kotlin.resolve.BindingContext

class LoopExpressionGenerator(statementGenerator: StatementGenerator) : StatementGeneratorExtension(statementGenerator) {
    fun generateWhileLoop(ktWhile: KtWhileExpression): IrExpression {
        val irLoop = IrWhileLoopImpl(
            ktWhile.startOffset, ktWhile.endOffset,
            context.builtIns.unitType, IrStatementOrigin.WHILE_LOOP
        )

        irLoop.condition = statementGenerator.generateExpression(ktWhile.condition!!)

        statementGenerator.bodyGenerator.putLoop(ktWhile, irLoop)

        irLoop.body = ktWhile.body?.let { ktLoopBody ->
            if (ktLoopBody is KtBlockExpression)
                generateWhileLoopBody(ktLoopBody)
            else
                statementGenerator.generateExpression(ktLoopBody)
        }

        irLoop.label = getLoopLabel(ktWhile)

        return irLoop
    }

    fun generateDoWhileLoop(ktDoWhile: KtDoWhileExpression): IrExpression {
        val irLoop = IrDoWhileLoopImpl(
            ktDoWhile.startOffset, ktDoWhile.endOffset,
            context.builtIns.unitType, IrStatementOrigin.DO_WHILE_LOOP
        )

        statementGenerator.bodyGenerator.putLoop(ktDoWhile, irLoop)

        irLoop.body = ktDoWhile.body?.let { ktLoopBody ->
            if (ktLoopBody is KtBlockExpression)
                generateDoWhileLoopBody(ktLoopBody)
            else
                statementGenerator.generateExpression(ktLoopBody)
        }

        irLoop.condition = statementGenerator.generateExpression(ktDoWhile.condition!!)

        irLoop.label = getLoopLabel(ktDoWhile)

        return IrBlockImpl(ktDoWhile.startOffset, ktDoWhile.endOffset, context.builtIns.unitType).apply {
            statements.add(irLoop)
        }
    }

    private fun generateWhileLoopBody(ktLoopBody: KtBlockExpression): IrExpression =
        IrBlockImpl(
            ktLoopBody.startOffset, ktLoopBody.endOffset, context.builtIns.unitType, null,
            ktLoopBody.statements.map { statementGenerator.generateStatement(it) }
        )


    private fun generateDoWhileLoopBody(ktLoopBody: KtBlockExpression): IrExpression =
        IrCompositeImpl(
            ktLoopBody.startOffset, ktLoopBody.endOffset, context.builtIns.unitType, null,
            ktLoopBody.statements.map { statementGenerator.generateStatement(it) }
        )

    fun generateBreak(ktBreak: KtBreakExpression): IrExpression {
        val parentLoop = findParentLoop(ktBreak) ?: return ErrorExpressionGenerator(statementGenerator).generateErrorExpression(
            ktBreak, RuntimeException("Loop not found for break expression: ${ktBreak.text}")
        )
        return IrBreakImpl(ktBreak.startOffset, ktBreak.endOffset, context.builtIns.nothingType, parentLoop).apply {
            label = ktBreak.getLabelName()
        }
    }

    fun generateContinue(ktContinue: KtContinueExpression): IrExpression {
        val parentLoop = findParentLoop(ktContinue) ?: return ErrorExpressionGenerator(statementGenerator).generateErrorExpression(
            ktContinue, RuntimeException("Loop not found for continue expression: ${ktContinue.text}")
        )
        return IrContinueImpl(ktContinue.startOffset, ktContinue.endOffset, context.builtIns.nothingType, parentLoop).apply {
            label = ktContinue.getLabelName()
        }
    }

    private fun getLoopLabel(ktLoop: KtLoopExpression): String? =
        (ktLoop.parent as? KtLabeledExpression)?.getLabelName()

    private fun findParentLoop(ktWithLabel: KtExpressionWithLabel): IrLoop? =
        findParentLoop(ktWithLabel, ktWithLabel.getLabelName())

    private fun findParentLoop(ktExpression: KtExpression, targetLabel: String?): IrLoop? {
        var finger: KtExpression? = ktExpression
        while (finger != null) {
            finger = finger.getParentOfType<KtLoopExpression>(true)
            if (finger == null) {
                break
            }
            if (targetLabel == null) {
                return getLoop(finger) ?: continue
            } else {
                val parent = finger.parent
                if (parent is KtLabeledExpression) {
                    val label = parent.getLabelName()!!
                    if (targetLabel == label) {
                        return getLoop(finger) ?: continue
                    }
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

        val irForBlock = IrBlockImpl(ktFor.startOffset, ktFor.endOffset, context.builtIns.unitType, IrStatementOrigin.FOR_LOOP)

        val iteratorCall = statementGenerator.pregenerateCall(iteratorResolvedCall)
        val irIteratorCall = callGenerator.generateCall(ktLoopRange, iteratorCall, IrStatementOrigin.FOR_LOOP_ITERATOR)
        val irIterator = scope.createTemporaryVariable(irIteratorCall, "iterator", origin = IrDeclarationOrigin.FOR_LOOP_ITERATOR)
        val iteratorValue = VariableLValue(irIterator)
        irForBlock.statements.add(irIterator)

        val irInnerWhile =
            IrWhileLoopImpl(ktFor.startOffset, ktFor.endOffset, context.builtIns.unitType, IrStatementOrigin.FOR_LOOP_INNER_WHILE)
        irInnerWhile.label = getLoopLabel(ktFor)
        statementGenerator.bodyGenerator.putLoop(ktFor, irInnerWhile)
        irForBlock.statements.add(irInnerWhile)

        val hasNextCall = statementGenerator.pregenerateCall(hasNextResolvedCall)
        hasNextCall.setExplicitReceiverValue(iteratorValue)
        val irHasNextCall = callGenerator.generateCall(ktLoopRange, hasNextCall, IrStatementOrigin.FOR_LOOP_HAS_NEXT)
        irInnerWhile.condition = irHasNextCall

        val irInnerBody = IrBlockImpl(ktFor.startOffset, ktFor.endOffset, context.builtIns.unitType, IrStatementOrigin.FOR_LOOP_INNER_WHILE)
        irInnerWhile.body = irInnerBody

        val nextCall = statementGenerator.pregenerateCall(nextResolvedCall)
        nextCall.setExplicitReceiverValue(iteratorValue)
        val irNextCall = callGenerator.generateCall(ktLoopRange, nextCall, IrStatementOrigin.FOR_LOOP_NEXT)
        val irLoopParameter =
            if (ktLoopParameter != null && ktLoopDestructuringDeclaration == null)
                context.symbolTable.declareVariable(
                    ktLoopParameter.startOffset, ktLoopParameter.endOffset, IrDeclarationOrigin.FOR_LOOP_VARIABLE,
                    getOrFail(BindingContext.VALUE_PARAMETER, ktLoopParameter),
                    irNextCall
                )
            else
                scope.createTemporaryVariable(irNextCall, "loop_parameter", origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE)
        irInnerBody.statements.add(irLoopParameter)

        if (ktLoopDestructuringDeclaration != null) {
            statementGenerator.declareComponentVariablesInBlock(
                ktLoopDestructuringDeclaration,
                irInnerBody,
                VariableLValue(irLoopParameter)
            )
        }

        if (ktForBody != null) {
            irInnerBody.statements.add(statementGenerator.generateExpression(ktForBody))
        }

        return irForBlock
    }
}