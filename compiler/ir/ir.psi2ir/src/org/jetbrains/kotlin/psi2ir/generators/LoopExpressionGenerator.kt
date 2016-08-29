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
import org.jetbrains.kotlin.ir.declarations.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.intermediate.VariableLValue
import org.jetbrains.kotlin.psi2ir.intermediate.setExplicitReceiverValue
import org.jetbrains.kotlin.resolve.BindingContext

class LoopExpressionGenerator(statementGenerator: StatementGenerator) : StatementGeneratorExtension(statementGenerator){
    fun generateWhileLoop(ktWhile: KtWhileExpression): IrExpression =
            generateConditionalLoop(ktWhile,
                                    IrWhileLoopImpl(ktWhile.startOffset, ktWhile.endOffset,
                                                    context.builtIns.unitType, IrOperator.WHILE_LOOP))

    fun generateDoWhileLoop(ktDoWhile: KtDoWhileExpression): IrExpression =
            generateConditionalLoop(ktDoWhile,
                                    IrDoWhileLoopImpl(ktDoWhile.startOffset, ktDoWhile.endOffset,
                                                      context.builtIns.unitType, IrOperator.DO_WHILE_LOOP))

    private fun generateConditionalLoop(ktLoop: KtWhileExpressionBase, irLoop: IrLoopBase): IrLoop {
        statementGenerator.bodyGenerator.putLoop(ktLoop, irLoop)
        irLoop.condition = statementGenerator.generateExpression(ktLoop.condition!!)
        irLoop.body = ktLoop.body?.let { statementGenerator.generateExpression(ktLoop.body!!) }
        irLoop.label = getLoopLabel(ktLoop)
        return irLoop
    }

    fun generateBreak(ktBreak: KtBreakExpression): IrExpression {
        val parentLoop = findParentLoop(ktBreak)
        return IrBreakImpl(ktBreak.startOffset, ktBreak.endOffset, context.builtIns.nothingType, parentLoop).apply {
            label = ktBreak.getLabelName()
        }
    }

    fun generateContinue(ktContinue: KtContinueExpression): IrExpression {
        val parentLoop = findParentLoop(ktContinue)
        return IrContinueImpl(ktContinue.startOffset, ktContinue.endOffset, context.builtIns.nothingType, parentLoop).apply {
            label = ktContinue.getLabelName()
        }
    }

    private fun getLoopLabel(ktLoop: KtLoopExpression): String? =
            (ktLoop.parent as? KtLabeledExpression)?.getLabelName()

    private fun findParentLoop(ktWithLabel: KtExpressionWithLabel): IrLoop =
            findParentLoop(ktWithLabel, ktWithLabel.getLabelName())

    private fun findParentLoop(ktExpression: KtExpression, targetLabel: String?): IrLoop {
        var finger: KtExpression? = ktExpression
        while (finger != null) {
            finger = finger.getParentOfType<KtLoopExpression>(true)
            if (finger == null) {
                break
            }
            if (targetLabel == null) {
                return getLoop(finger)
            }
            else {
                val parent = finger.parent
                if (parent is KtLabeledExpression) {
                    val label = parent.getLabelName()!!
                    if (targetLabel == label) {
                        return getLoop(finger)
                    }
                }
            }
        }
        throw AssertionError("No parent loop for break/continue @$targetLabel")
    }

    private fun getLoop(ktLoop: KtLoopExpression): IrLoop {
        return statementGenerator.bodyGenerator.getLoop(ktLoop) ?:
               throw AssertionError("Loop was not visited:\n${ktLoop.text}")
    }

    fun generateForLoop(ktFor: KtForExpression): IrExpression {
        val ktLoopParameter = ktFor.loopParameter
        val ktLoopDestructuringParameter = ktFor.destructuringParameter
        if (ktLoopParameter == null && ktLoopDestructuringParameter == null) {
            throw AssertionError("Either loopParameter or destructuringParameter should be present:\n${ktFor.text}")
        }

        val ktLoopRange = ktFor.loopRange!!
        val ktForBody = ktFor.body
        val iteratorResolvedCall = getOrFail(BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL, ktLoopRange)
        val hasNextResolvedCall = getOrFail(BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL, ktLoopRange)
        val nextResolvedCall = getOrFail(BindingContext.LOOP_RANGE_NEXT_RESOLVED_CALL, ktLoopRange)

        val callGenerator = CallGenerator(statementGenerator)

        val irForBlock = IrBlockImpl(ktFor.startOffset, ktFor.endOffset, context.builtIns.unitType, IrOperator.FOR_LOOP)

        val iteratorCall = statementGenerator.pregenerateCall(iteratorResolvedCall)
        val irIteratorCall = callGenerator.generateCall(ktLoopRange, iteratorCall, IrOperator.FOR_LOOP_ITERATOR)
        val irIterator = scope.createTemporaryVariable(irIteratorCall, "iterator")
        val iteratorValue = VariableLValue(irIterator)
        irForBlock.addStatement(irIterator)

        val irInnerWhile = IrWhileLoopImpl(ktFor.startOffset, ktFor.endOffset, context.builtIns.unitType, IrOperator.FOR_LOOP_INNER_WHILE)
        irInnerWhile.label = getLoopLabel(ktFor)
        statementGenerator.bodyGenerator.putLoop(ktFor, irInnerWhile)
        irForBlock.addStatement(irInnerWhile)

        val hasNextCall = statementGenerator.pregenerateCall(hasNextResolvedCall)
        hasNextCall.setExplicitReceiverValue(iteratorValue)
        val irHasNextCall = callGenerator.generateCall(ktLoopRange, hasNextCall, IrOperator.FOR_LOOP_HAS_NEXT)
        irInnerWhile.condition = irHasNextCall

        val irInnerBody = IrBlockImpl(ktFor.startOffset, ktFor.endOffset, context.builtIns.unitType, IrOperator.FOR_LOOP_INNER_WHILE)
        irInnerWhile.body = irInnerBody

        val nextCall = statementGenerator.pregenerateCall(nextResolvedCall)
        nextCall.setExplicitReceiverValue(iteratorValue)
        val irNextCall = callGenerator.generateCall(ktLoopRange, nextCall, IrOperator.FOR_LOOP_NEXT)
        val irLoopParameter = if (ktLoopParameter != null) {
            val loopParameterDescriptor = getOrFail(BindingContext.VALUE_PARAMETER, ktLoopParameter)
            IrVariableImpl(ktLoopParameter.startOffset, ktLoopParameter.endOffset, IrDeclarationOrigin.DEFINED,
                           loopParameterDescriptor, irNextCall)
        }
        else {
            scope.createTemporaryVariable(irNextCall, "loop_parameter")
        }
        irInnerBody.addStatement(irLoopParameter)

        if (ktLoopDestructuringParameter != null) {
            statementGenerator.declareComponentVariablesInBlock(ktLoopDestructuringParameter, irInnerBody, VariableLValue(irLoopParameter))
        }

        if (ktForBody != null) {
            irInnerBody.addStatement(statementGenerator.generateExpression(ktForBody))
        }

        return irForBlock
    }
}