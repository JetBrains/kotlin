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
import org.jetbrains.kotlin.psi2ir.defaultLoad
import org.jetbrains.kotlin.psi2ir.generators.operators.getInfixOperator
import org.jetbrains.kotlin.resolve.BindingContext
import java.lang.AssertionError

class WhenExpressionGenerator(parentGenerator: StatementGenerator) : IrChildBodyGeneratorBase<StatementGenerator>(parentGenerator) {
    fun generate(expression: KtWhenExpression): IrExpression {
        val conditionsGenerator = CallGenerator(parentGenerator)

        val irSubject = expression.subjectExpression?.let {
            conditionsGenerator.scope.createTemporary(it, parentGenerator.generateExpression(it), "subject")
        }

        val resultType = getInferredTypeWithSmartcasts(expression)

        val irWhen = IrWhenImpl(expression.startOffset, expression.endOffset, resultType, IrOperator.WHEN)

        for (ktEntry in expression.entries) {
            if (ktEntry.isElse) {
                irWhen.elseBranch = parentGenerator.generateExpressionWithExpectedType(ktEntry.expression!!, resultType)
                break
            }

            var irBranchCondition: IrExpression? = null
            for (ktCondition in ktEntry.conditions) {
                val irCondition =
                        if (irSubject != null)
                            generateWhenConditionWithSubject(ktCondition, conditionsGenerator, irSubject)
                        else
                            generateWhenConditionNoSubject(ktCondition)
                irBranchCondition = irBranchCondition?.let { IrIfThenElseImpl.whenComma(it, irCondition) } ?: irCondition

            }

            val irBranchResult = parentGenerator.generateExpressionWithExpectedType(ktEntry.expression!!, resultType)
            irWhen.addBranch(irBranchCondition!!, irBranchResult)
        }

        return generateWhenBody(expression, irSubject, irWhen)
    }

    private fun generateWhenBody(expression: KtWhenExpression, irSubject: IrVariable?, irWhen: IrWhen): IrExpression {
        if (irSubject == null) {
            if (irWhen.branchesCount == 0)
                return IrBlockImpl(expression.startOffset, expression.endOffset, null, false, IrOperator.WHEN)
            else
                return irWhen
        }
        else {
            if (irWhen.branchesCount == 0) {
                val irBlock = IrBlockImpl(expression.startOffset, expression.endOffset, null, false, IrOperator.WHEN)
                irBlock.addStatement(irSubject)
                return irBlock
            }
            else {
                val irBlock = IrBlockImpl(expression.startOffset, expression.endOffset, irWhen.type, true, IrOperator.WHEN)
                irBlock.addStatement(irSubject)
                irBlock.addStatement(irWhen)
                return irBlock
            }
        }
    }

    private fun generateWhenConditionNoSubject(ktCondition: KtWhenCondition): IrExpression =
            parentGenerator.generateExpressionWithExpectedType((ktCondition as KtWhenConditionWithExpression).expression!!,
                                                               context.builtIns.booleanType)

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
        return IrTypeOperatorCallImpl(
                ktCondition.startOffset, ktCondition.endOffset, context.builtIns.booleanType,
                IrTypeOperator.INSTANCEOF, isType, irSubject.defaultLoad()
        )
    }

    private fun generateInRangeCondition(conditionsGenerator: CallGenerator, ktCondition: KtWhenConditionInRange): IrExpression {
        val inResolvedCall = getResolvedCall(ktCondition.operationReference)!!
        val inOperator = getInfixOperator(ktCondition.operationReference.getReferencedNameElementType())
        val irInCall = conditionsGenerator.generateCall(ktCondition, inResolvedCall, inOperator)
        return when (inOperator) {
            IrOperator.IN -> irInCall
            IrOperator.NOT_IN ->
                IrUnaryOperatorImpl(ktCondition.startOffset, ktCondition.endOffset, IrOperator.EXCL, context.irBuiltIns.booleanNot, irInCall)
            else -> throw AssertionError("Expected 'in' or '!in', got $inOperator")
        }
    }

    private fun generateEqualsCondition(irSubject: IrVariable, ktCondition: KtWhenConditionWithExpression): IrBinaryOperatorImpl =
            IrBinaryOperatorImpl(
                    ktCondition.startOffset, ktCondition.endOffset,
                    IrOperator.EQEQ, context.irBuiltIns.eqeq,
                    irSubject.defaultLoad(), parentGenerator.generateExpression(ktCondition.expression!!)
            )
}