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
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.psi2ir.generators.getInfixOperator
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.utils.SmartList
import java.lang.AssertionError

class BranchingExpressionGenerator(val statementGenerator: StatementGenerator) : GeneratorWithScope {
    override val scope: Scope get() = statementGenerator.scope
    override val context: GeneratorContext get() = statementGenerator.context

    fun generateIfExpression(expression: KtIfExpression): IrExpression {
        val resultType = getInferredTypeWithSmartcasts(expression)

        var ktLastIf: KtIfExpression = expression
        val irBranches = SmartList<Pair<IrExpression, IrExpression>>()
        var irElseBranch: IrExpression? = null

        whenBranches@while (true) {
            val irCondition = statementGenerator.generateExpression(ktLastIf.condition!!)
            val irThenBranch = statementGenerator.generateExpression(ktLastIf.then!!)
            irBranches.add(Pair(irCondition, irThenBranch))

            val ktElse = ktLastIf.`else`?.deparenthesize()
            when (ktElse) {
                null -> break@whenBranches
                is KtIfExpression -> ktLastIf = ktElse
                is KtExpression -> {
                    irElseBranch = statementGenerator.generateExpression(ktElse)
                    break@whenBranches
                }
                else -> throw AssertionError("Unexpected else expression: ${ktElse.text}")
            }
        }

        return if (irBranches.size == 1) {
            val (irCondition, irThenBranch) = irBranches[0]
            IrIfThenElseImpl(expression.startOffset, expression.endOffset, resultType,
                             irCondition, irThenBranch, irElseBranch, IrOperator.IF)
        }
        else {
            val irWhen = IrWhenImpl(expression.startOffset, expression.endOffset, resultType, IrOperator.WHEN)
            for ((irCondition, irThenBranch) in irBranches) {
                irWhen.addBranch(irCondition, irThenBranch)
            }
            irWhen.elseBranch = irElseBranch
            irWhen
        }
    }

    fun generateWhenExpression(expression: KtWhenExpression): IrExpression {
        val irSubject = expression.subjectExpression?.let {
            scope.createTemporaryVariable(statementGenerator.generateExpression(it), "subject")
        }

        val resultType = getInferredTypeWithSmartcasts(expression)

        val irWhen = IrWhenImpl(expression.startOffset, expression.endOffset, resultType, IrOperator.WHEN)

        for (ktEntry in expression.entries) {
            if (ktEntry.isElse) {
                irWhen.elseBranch = statementGenerator.generateExpression(ktEntry.expression!!)
                break
            }

            var irBranchCondition: IrExpression? = null
            for (ktCondition in ktEntry.conditions) {
                val irCondition =
                        if (irSubject != null)
                            generateWhenConditionWithSubject(ktCondition, irSubject)
                        else
                            generateWhenConditionNoSubject(ktCondition)
                irBranchCondition = irBranchCondition?.let { whenComma(it, irCondition) } ?: irCondition

            }

            val irBranchResult = statementGenerator.generateExpression(ktEntry.expression!!)
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
            statementGenerator.generateExpression((ktCondition as KtWhenConditionWithExpression).expression!!)

    private fun generateWhenConditionWithSubject(ktCondition: KtWhenCondition, irSubject: IrVariable): IrExpression {
        return when (ktCondition) {
            is KtWhenConditionWithExpression ->
                generateEqualsCondition(irSubject, ktCondition)
            is KtWhenConditionInRange ->
                generateInRangeCondition(irSubject, ktCondition)
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

    private fun generateInRangeCondition(irSubject: IrVariable, ktCondition: KtWhenConditionInRange): IrExpression {
        val inCall = statementGenerator.pregenerateCall(getResolvedCall(ktCondition.operationReference)!!)
        inCall.irValueArgumentsByIndex[0] = irSubject.defaultLoad()
        val inOperator = getInfixOperator(ktCondition.operationReference.getReferencedNameElementType())
        val irInCall = CallGenerator(statementGenerator).generateCall(ktCondition, inCall, inOperator)
        return when (inOperator) {
            IrOperator.IN -> irInCall
            IrOperator.NOT_IN ->
                IrUnaryPrimitiveImpl(ktCondition.startOffset, ktCondition.endOffset, IrOperator.EXCL, context.irBuiltIns.booleanNot, irInCall)
            else -> throw AssertionError("Expected 'in' or '!in', got $inOperator")
        }
    }

    private fun generateEqualsCondition(irSubject: IrVariable, ktCondition: KtWhenConditionWithExpression): IrBinaryPrimitiveImpl =
            IrBinaryPrimitiveImpl(
                    ktCondition.startOffset, ktCondition.endOffset,
                    IrOperator.EQEQ, context.irBuiltIns.eqeq,
                    irSubject.defaultLoad(), statementGenerator.generateExpression(ktCondition.expression!!)
            )
}