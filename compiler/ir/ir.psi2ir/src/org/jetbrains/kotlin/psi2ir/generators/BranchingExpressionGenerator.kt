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

import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.buildStatement
import org.jetbrains.kotlin.ir.builders.irIfThenMaybeElse
import org.jetbrains.kotlin.ir.builders.primitiveOp1
import org.jetbrains.kotlin.ir.builders.whenComma
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.psi2ir.intermediate.loadAt
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.utils.SmartList

class BranchingExpressionGenerator(statementGenerator: StatementGenerator) : StatementGeneratorExtension(statementGenerator) {

    fun generateIfExpression(expression: KtIfExpression): IrExpression {
        var ktLastIf: KtIfExpression = expression
        val irBranches = SmartList<IrBranch>()
        var irElseBranch: IrExpression? = null

        whenBranches@ while (true) {
            val irCondition = ktLastIf.condition!!.genExpr()

            val irThenBranch = ktLastIf.then?.genExpr() ?: generateEmptyBlockForMissingBranch(ktLastIf)
            irBranches.add(IrBranchImpl(irCondition, irThenBranch))

            when (val ktElse = ktLastIf.`else`?.deparenthesize()) {
                null -> break@whenBranches
                is KtIfExpression -> ktLastIf = ktElse
                is KtExpression -> {
                    irElseBranch = ktElse.genExpr()
                    break@whenBranches
                }
                else -> throw AssertionError("Unexpected else expression: ${ktElse.text}")
            }
        }

        return createIrWhen(expression, irBranches, irElseBranch, getExpressionTypeWithCoercionToUnitOrFail(expression).toIrType())
    }

    private fun generateEmptyBlockForMissingBranch(ktLastIf: KtIfExpression) =
        IrBlockImpl(ktLastIf.startOffset, ktLastIf.endOffset, context.irBuiltIns.unitType, IrStatementOrigin.IF, listOf())

    private fun createIrWhen(
        ktIf: KtIfExpression,
        irBranches: List<IrBranch>,
        irElseResult: IrExpression?,
        resultType: IrType
    ): IrWhen {
        if (irBranches.size == 1) {
            val irBranch0 = irBranches[0]
            return buildStatement(ktIf.startOffsetSkippingComments, ktIf.endOffset) {
                irIfThenMaybeElse(resultType, irBranch0.condition, irBranch0.result, irElseResult, IrStatementOrigin.IF)
            }
        }

        val irWhen = IrWhenImpl(ktIf.startOffsetSkippingComments, ktIf.endOffset, resultType, IrStatementOrigin.IF)

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

        val irWhen = IrWhenImpl(
            expression.startOffsetSkippingComments, expression.endOffset,
            getExpressionTypeWithCoercionToUnitOrFail(expression).toIrType(), IrStatementOrigin.WHEN
        )

        var hasExplicitElseBranch = false
        for (ktEntry in expression.entries) {
            if (ktEntry.isElse) {
                val irElseResult = ktEntry.expression!!.genExpr()
                irWhen.branches.add(elseBranch(irElseResult))
                hasExplicitElseBranch = true
                break
            }

            var irBranchCondition: IrExpression? = null
            for (ktCondition in ktEntry.conditions) {
                val irCondition =
                    if (irSubject != null)
                        generateWhenConditionWithSubject(ktCondition, irSubject, expression.subjectExpression)
                    else
                        generateWhenConditionNoSubject(ktCondition)
                irBranchCondition = irBranchCondition?.let { context.whenComma(it, irCondition) } ?: irCondition
            }

            val irBranchResult = ktEntry.expression!!.genExpr()
            irWhen.branches.add(IrBranchImpl(irBranchCondition!!, irBranchResult))
        }
        if (!hasExplicitElseBranch) {
            addElseBranchForExhaustiveWhenIfNeeded(irWhen, expression)
        }

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
        val isUsedAsExpression = true == get(BindingContext.USED_AS_EXPRESSION, whenExpression)
        val isImplicitElseRequired =
            if (isUsedAsExpression)
                true == get(BindingContext.EXHAUSTIVE_WHEN, whenExpression)
            else
                true == get(BindingContext.IMPLICIT_EXHAUSTIVE_WHEN, whenExpression)
        if (isImplicitElseRequired) {
            val call = IrCallImpl.fromSymbolDescriptor(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                context.irBuiltIns.nothingType,
                context.irBuiltIns.noWhenBranchMatchedExceptionSymbol
            )
            irWhen.branches.add(elseBranch(call))
        }
    }

    private fun generateWhenBody(expression: KtWhenExpression, irSubject: IrVariable?, irWhen: IrWhen): IrExpression =
        if (irSubject == null) {
            if (irWhen.branches.isEmpty())
                IrBlockImpl(
                    expression.startOffsetSkippingComments,
                    expression.endOffset,
                    context.irBuiltIns.unitType,
                    IrStatementOrigin.WHEN
                )
            else
                irWhen
        } else {
            if (irWhen.branches.isEmpty()) {
                val irBlock = IrBlockImpl(
                    expression.startOffsetSkippingComments,
                    expression.endOffset,
                    context.irBuiltIns.unitType,
                    IrStatementOrigin.WHEN
                )
                irBlock.statements.add(irSubject)
                irBlock
            } else {
                val irBlock = IrBlockImpl(expression.startOffsetSkippingComments, expression.endOffset, irWhen.type, IrStatementOrigin.WHEN)
                irBlock.statements.add(irSubject)
                irBlock.statements.add(irWhen)
                irBlock
            }
        }

    private fun generateWhenConditionNoSubject(ktCondition: KtWhenCondition): IrExpression =
        (ktCondition as KtWhenConditionWithExpression).expression!!.genExpr()

    private fun generateWhenConditionWithSubject(
        ktCondition: KtWhenCondition, irSubject: IrVariable, ktSubject: KtExpression?
    ): IrExpression {
        return when (ktCondition) {
            is KtWhenConditionWithExpression ->
                generateEqualsCondition(irSubject, ktCondition)
            is KtWhenConditionInRange ->
                generateInCondition(irSubject, ktCondition, ktSubject)
            is KtWhenConditionIsPattern ->
                generateIsPatternCondition(irSubject, ktCondition)
            else ->
                throw AssertionError("Unexpected 'when' condition: ${ktCondition.text}")
        }
    }

    private fun generateIsPatternCondition(irSubject: IrVariable, ktCondition: KtWhenConditionIsPattern): IrExpression {
        val typeOperand = getOrFail(BindingContext.TYPE, ktCondition.typeReference)
        val irTypeOperand = typeOperand.toIrType()
        val startOffset = ktCondition.startOffsetSkippingComments
        val endOffset = ktCondition.endOffset
        val irInstanceOf = IrTypeOperatorCallImpl(
            startOffset, endOffset,
            context.irBuiltIns.booleanType,
            IrTypeOperator.INSTANCEOF,
            irTypeOperand,
            irSubject.loadAt(startOffset, startOffset)
        )
        return if (ktCondition.isNegated)
            primitiveOp1(
                ktCondition.startOffsetSkippingComments, ktCondition.endOffset,
                context.irBuiltIns.booleanNotSymbol,
                context.irBuiltIns.booleanType,
                IrStatementOrigin.EXCL,
                irInstanceOf
            )
        else
            irInstanceOf
    }

    private fun generateInCondition(irSubject: IrVariable, ktCondition: KtWhenConditionInRange, ktSubject: KtExpression?): IrExpression {
        val startOffset = ktCondition.startOffsetSkippingComments
        val endOffset = ktCondition.endOffset
        val inCall = statementGenerator.pregenerateCallUsing(getResolvedCall(ktCondition.operationReference)!!) {
            // In a `when` with a subject, `in x` is represented as `x.contains(<reference to subject expression>)`.
            if (it === ktSubject) irSubject.loadAt(startOffset, startOffset) else statementGenerator.generateExpression(it)
        }
        val inOperator = getInfixOperator(ktCondition.operationReference.getReferencedNameElementType())
        val irInCall = CallGenerator(statementGenerator).generateCall(ktCondition, inCall, inOperator)
        return when (inOperator) {
            IrStatementOrigin.IN ->
                irInCall
            IrStatementOrigin.NOT_IN ->
                primitiveOp1(
                    startOffset, endOffset,
                    context.irBuiltIns.booleanNotSymbol,
                    context.irBuiltIns.booleanType,
                    IrStatementOrigin.EXCL,
                    irInCall
                )
            else -> throw AssertionError("Expected 'in' or '!in', got $inOperator")
        }
    }

    private fun generateEqualsCondition(irSubject: IrVariable, ktCondition: KtWhenConditionWithExpression): IrExpression {
        val ktExpression = ktCondition.expression
        val irExpression = ktExpression!!.genExpr()
        val startOffset = ktCondition.startOffsetSkippingComments
        val endOffset = ktCondition.endOffset
        return OperatorExpressionGenerator(statementGenerator).generateEquality(
            startOffset, endOffset, IrStatementOrigin.EQEQ,
            irSubject.loadAt(startOffset, startOffset), irExpression,
            context.bindingContext[BindingContext.PRIMITIVE_NUMERIC_COMPARISON_INFO, ktExpression]
        )
    }
}
