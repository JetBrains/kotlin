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

import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.load
import org.jetbrains.kotlin.psi2ir.toExpectedType
import org.jetbrains.kotlin.resolve.BindingContext

class WhenExpressionGenerator(val statementGenerator: StatementGenerator) : IrGenerator {
    override val context: GeneratorContext get() = statementGenerator.context

    fun generate(expression: KtWhenExpression): IrExpression {
        val conditionsGenerator = CallGenerator(statementGenerator)

        val irSubject = expression.subjectExpression?.let {
            conditionsGenerator.createTemporary(it, statementGenerator.generateExpression(it), "subject")
        }

        val resultType = getInferredTypeWithSmartcasts(expression)

        val irWhen = IrWhenExpressionImpl(expression.startOffset, expression.endOffset, resultType, irSubject)

        for (ktEntry in expression.entries) {
            if (ktEntry.isElse) {
                irWhen.elseExpression = statementGenerator.generateExpression(ktEntry.expression!!).toExpectedType(resultType)
                continue
            }

            val irBranch = IrBranchImpl(ktEntry.startOffset, ktEntry.endOffset)

            for (ktCondition in ktEntry.conditions) {
                val irCondition =
                        if (irSubject != null)
                            generateWhenConditionWithSubject(ktCondition, conditionsGenerator, irSubject)
                        else
                            generateWhenConditionNoSubject(ktCondition)
                irBranch.addCondition(irCondition)
            }

            irBranch.result = statementGenerator.generateExpression(ktEntry.expression!!).toExpectedType(resultType)

            irWhen.addBranch(irBranch)
        }

        return irWhen
    }

    private fun generateWhenConditionNoSubject(ktCondition: KtWhenCondition): IrExpression =
            statementGenerator.generateExpression((ktCondition as KtWhenConditionWithExpression).expression!!)
                    .toExpectedType(context.builtIns.booleanType)

    private fun generateWhenConditionWithSubject(
            ktCondition: KtWhenCondition,
            conditionsGenerator: CallGenerator,
            irSubject: IrVariable
    ): IrExpression {
        return when (ktCondition) {
            is KtWhenConditionWithExpression ->
                generateEqualsCondition(irSubject, ktCondition)
            is KtWhenConditionInRange ->
                generateInRangeCondition(conditionsGenerator, ktCondition)
            is KtWhenConditionIsPattern ->
                generateIsPatternCondition(irSubject, ktCondition)
            else ->
                throw AssertionError("Unexpected 'when' condition: ${ktCondition.text}")
        }
    }

    private fun generateIsPatternCondition(irSubject: IrVariable, ktCondition: KtWhenConditionIsPattern): IrExpression {
        val isType = getOrFail(BindingContext.TYPE, ktCondition.typeReference)
        return IrTypeOperatorExpressionImpl(
                ktCondition.startOffset, ktCondition.endOffset, context.builtIns.booleanType,
                IrTypeOperator.INSTANCEOF, isType, irSubject.load()
        )
    }

    private fun generateInRangeCondition(conditionsGenerator: CallGenerator, ktCondition: KtWhenConditionInRange): IrExpression {
        val inResolvedCall = getResolvedCall(ktCondition.operationReference)!!
        val inOperator = getIrBinaryOperator(ktCondition.operationReference.getReferencedNameElementType())
        val irInCall = conditionsGenerator.generateCall(ktCondition, inResolvedCall, inOperator)
        return when (inOperator) {
            IrOperator.IN -> irInCall
            IrOperator.NOT_IN ->
                IrUnaryOperatorExpressionImpl(ktCondition.startOffset, ktCondition.endOffset, context.builtIns.booleanType,
                                              IrOperator.EXCL, null, irInCall)
            else -> throw AssertionError("Expected 'in' or '!in', got $inOperator")
        }
    }

    private fun generateEqualsCondition(irSubject: IrVariable, ktCondition: KtWhenConditionWithExpression): IrBinaryOperatorExpressionImpl =
            IrBinaryOperatorExpressionImpl(
                    ktCondition.startOffset, ktCondition.endOffset,
                    context.builtIns.booleanType, IrOperator.EQEQ, null,
                    irSubject.load(), statementGenerator.generateExpression(ktCondition.expression!!)
            )
}