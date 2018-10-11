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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.buildStatement
import org.jetbrains.kotlin.ir.builders.irIfThenMaybeElse
import org.jetbrains.kotlin.ir.builders.whenComma
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.psi2ir.intermediate.defaultLoad
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.utils.SmartList

class BranchingExpressionGenerator(statementGenerator: StatementGenerator) : StatementGeneratorExtension(statementGenerator) {

    fun generateIfExpression(expression: KtIfExpression): IrExpression {
        val resultType = getInferredTypeWithImplicitCastsOrFail(expression).toIrType()

        var ktLastIf: KtIfExpression = expression
        val irBranches = SmartList<IrBranch>()
        var irElseBranch: IrExpression? = null

        whenBranches@ while (true) {
            val irCondition = ktLastIf.condition!!.genExpr()
            val irThenBranch = ktLastIf.then!!.genExpr()
            irBranches.add(IrBranchImpl(irCondition, irThenBranch))

            val ktElse = ktLastIf.`else`?.deparenthesize()
            when (ktElse) {
                null -> break@whenBranches
                is KtIfExpression -> ktLastIf = ktElse
                is KtExpression -> {
                    irElseBranch = ktElse.genExpr()
                    break@whenBranches
                }
                else -> throw AssertionError("Unexpected else expression: ${ktElse.text}")
            }
        }

        return createIrWhen(expression, irBranches, irElseBranch, resultType)
    }

    private fun createIrWhen(
        ktIf: KtIfExpression,
        irBranches: List<IrBranch>,
        irElseResult: IrExpression?,
        resultType: IrType
    ): IrWhen {
        if (irBranches.size == 1) {
            val irBranch0 = irBranches[0]
            return buildStatement(ktIf.startOffset, ktIf.endOffset) {
                irIfThenMaybeElse(resultType, irBranch0.condition, irBranch0.result, irElseResult)
            }
        }

        val irWhen = IrWhenImpl(ktIf.startOffset, ktIf.endOffset, resultType, IrStatementOrigin.WHEN)

        irWhen.branches.addAll(irBranches)

        irElseResult?.let {
            irWhen.branches.add(elseBranch(it))
        }

        return irWhen
    }

    private fun elseBranch(result: IrExpression) =
        IrElseBranchImpl(
            IrConstImpl.boolean(result.startOffset, result.endOffset, context.irBuiltIns.booleanType, true),
            result
        )

    fun generateWhenExpression(expression: KtWhenExpression): IrExpression {
        val irSubject = generateWhenSubject(expression)

        val inferredType = getInferredTypeWithImplicitCastsOrFail(expression)

        // TODO relies on ControlFlowInformationProvider, get rid of it
        val isUsedAsExpression = get(BindingContext.USED_AS_EXPRESSION, expression) ?: false
        val isExhaustive = expression.isExhaustiveWhen()

        val resultType = when {
            isUsedAsExpression -> inferredType.toIrType()
            KotlinBuiltIns.isNothing(inferredType) -> inferredType.toIrType()
            else -> context.irBuiltIns.unitType
        }

        val irWhen = IrWhenImpl(expression.startOffset, expression.endOffset, resultType, IrStatementOrigin.WHEN)

        for (ktEntry in expression.entries) {
            if (ktEntry.isElse) {
                val irElseResult = ktEntry.expression!!.genExpr()
                irWhen.branches.add(elseBranch(irElseResult))
                break
            }

            var irBranchCondition: IrExpression? = null
            for (ktCondition in ktEntry.conditions) {
                val irCondition =
                    if (irSubject != null)
                        generateWhenConditionWithSubject(ktCondition, irSubject)
                    else
                        generateWhenConditionNoSubject(ktCondition)
                irBranchCondition = irBranchCondition?.let { context.whenComma(it, irCondition) } ?: irCondition
            }

            val irBranchResult = ktEntry.expression!!.genExpr()
            irWhen.branches.add(IrBranchImpl(irBranchCondition!!, irBranchResult))
        }
        addElseBranchForExhaustiveWhenIfNeeded(irWhen, expression)

        return generateWhenBody(expression, irSubject, irWhen)
    }

    private fun generateWhenSubject(expression: KtWhenExpression): IrVariable? {
        val subjectVariable = expression.subjectVariable
        val subjectExpression = expression.subjectExpression
        return when {
            subjectVariable != null -> statementGenerator.visitProperty(subjectVariable, null) as IrVariable
            subjectExpression != null -> scope.createTemporaryVariable(subjectExpression.genExpr(), "subject")
            else -> null
        }
    }

    private fun addElseBranchForExhaustiveWhenIfNeeded(irWhen: IrWhen, whenExpression: KtWhenExpression) {
        if (irWhen.branches.filterIsInstance<IrElseBranch>().isEmpty()) {
            //TODO: check condition: seems it's safe to always generate exception
            val isExhaustive = whenExpression.isExhaustiveWhen()

            if (isExhaustive) {
                val call = IrCallImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    context.irBuiltIns.nothingType,
                    context.irBuiltIns.noWhenBranchMatchedExceptionSymbol
                )
                irWhen.branches.add(elseBranch(call))
            }
        }
    }

    private fun KtWhenExpression.isExhaustiveWhen(): Boolean =
        elseExpression != null // TODO front-end should provide correct exhaustiveness information
                || true == get(BindingContext.EXHAUSTIVE_WHEN, this)
                || true == get(BindingContext.IMPLICIT_EXHAUSTIVE_WHEN, this)

    private fun generateWhenBody(expression: KtWhenExpression, irSubject: IrVariable?, irWhen: IrWhen): IrExpression =
        if (irSubject == null) {
            if (irWhen.branches.isEmpty())
                IrBlockImpl(expression.startOffset, expression.endOffset, context.irBuiltIns.unitType, IrStatementOrigin.WHEN)
            else
                irWhen
        } else {
            if (irWhen.branches.isEmpty()) {
                val irBlock = IrBlockImpl(expression.startOffset, expression.endOffset, context.irBuiltIns.unitType, IrStatementOrigin.WHEN)
                irBlock.statements.add(irSubject)
                irBlock
            } else {
                val irBlock = IrBlockImpl(expression.startOffset, expression.endOffset, irWhen.type, IrStatementOrigin.WHEN)
                irBlock.statements.add(irSubject)
                irBlock.statements.add(irWhen)
                irBlock
            }
        }

    private fun generateWhenConditionNoSubject(ktCondition: KtWhenCondition): IrExpression =
        (ktCondition as KtWhenConditionWithExpression).expression!!.genExpr()

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
        val typeOperand = getOrFail(BindingContext.TYPE, ktCondition.typeReference)
        val irTypeOperand = typeOperand.toIrType()
        val typeSymbol = irTypeOperand.classifierOrNull ?: throw AssertionError("Not a classifier type: $typeOperand")
        val irInstanceOf = IrTypeOperatorCallImpl(
            ktCondition.startOffset, ktCondition.endOffset,
            context.irBuiltIns.booleanType,
            IrTypeOperator.INSTANCEOF,
            irTypeOperand, typeSymbol,
            irSubject.defaultLoad()
        )
        return if (ktCondition.isNegated)
            IrUnaryPrimitiveImpl(
                ktCondition.startOffset, ktCondition.endOffset,
                context.irBuiltIns.booleanType,
                IrStatementOrigin.EXCL, context.irBuiltIns.booleanNotSymbol,
                irInstanceOf
            )
        else
            irInstanceOf
    }

    private fun generateInRangeCondition(irSubject: IrVariable, ktCondition: KtWhenConditionInRange): IrExpression {
        val inCall = statementGenerator.pregenerateCall(getResolvedCall(ktCondition.operationReference)!!)
        inCall.irValueArgumentsByIndex[0] = irSubject.defaultLoad()
        val inOperator = getInfixOperator(ktCondition.operationReference.getReferencedNameElementType())
        val irInCall = CallGenerator(statementGenerator).generateCall(ktCondition, inCall, inOperator)
        return when (inOperator) {
            IrStatementOrigin.IN ->
                irInCall
            IrStatementOrigin.NOT_IN ->
                IrUnaryPrimitiveImpl(
                    ktCondition.startOffset, ktCondition.endOffset,
                    context.irBuiltIns.booleanType,
                    IrStatementOrigin.EXCL, context.irBuiltIns.booleanNotSymbol,
                    irInCall
                )
            else -> throw AssertionError("Expected 'in' or '!in', got $inOperator")
        }
    }

    private fun generateEqualsCondition(irSubject: IrVariable, ktCondition: KtWhenConditionWithExpression): IrExpression {
        val ktExpression = ktCondition.expression
        val irExpression = ktExpression!!.genExpr()
        return OperatorExpressionGenerator(statementGenerator).generateEquality(
            ktCondition.startOffset, ktCondition.endOffset, IrStatementOrigin.EQEQ,
            irSubject.defaultLoad(), irExpression,
            context.bindingContext[BindingContext.PRIMITIVE_NUMERIC_COMPARISON_INFO, ktExpression]
        )
    }
}