/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.*

object PrimitiveNumericComparisonCallChecker : CallChecker {

    private val comparisonOperatorTokens = setOf(KtTokens.EQEQ, KtTokens.EXCLEQ, KtTokens.LT, KtTokens.LTEQ, KtTokens.GT, KtTokens.GTEQ)

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        // Primitive number comparisons only take part in binary operator convention resolution
        val binaryExpression = resolvedCall.call.callElement as? KtBinaryExpression ?: return
        if (!comparisonOperatorTokens.contains(binaryExpression.operationReference.getReferencedNameElementType())) return

        val leftExpr = binaryExpression.left ?: return
        val rightExpr = binaryExpression.right ?: return

        val leftType = context.getInferredPrimitiveNumericType(leftExpr) ?: return
        val rightType = context.getInferredPrimitiveNumericType(rightExpr) ?: return

        context.trace.record(BindingContext.PRIMITIVE_NUMERIC_COMPARISON_OPERAND_TYPE, leftExpr, leftType)
        context.trace.record(BindingContext.PRIMITIVE_NUMERIC_COMPARISON_OPERAND_TYPE, rightExpr, rightType)

        val leastCommonType = leastCommonPrimitiveNumericType(leftType, rightType)

        context.trace.record(BindingContext.PRIMITIVE_NUMERIC_COMPARISON_TYPE, binaryExpression, leastCommonType)
    }

    private fun leastCommonPrimitiveNumericType(t1: KotlinType, t2: KotlinType): KotlinType {
        val pt1 = t1.promoteIntegerTypeToIntIfRequired()
        val pt2 = t2.promoteIntegerTypeToIntIfRequired()

        return when {
            pt1.isDouble() || pt2.isDouble() -> t1.builtIns.doubleType
            pt1.isFloat() || pt2.isFloat() -> t1.builtIns.floatType
            pt1.isLong() || pt2.isLong() -> t1.builtIns.longType
            pt1.isInt() || pt2.isInt() -> t1.builtIns.intType
            else -> throw AssertionError("Unexpected types: t1=$t1, t2=$t2")
        }
    }

    private fun KotlinType.promoteIntegerTypeToIntIfRequired() =
        when {
            !isPrimitiveNumberType() -> throw AssertionError("Primitive number type expected: $this")
            isByte() || isChar() || isShort() -> builtIns.intType
            else -> this
        }

    private fun CallCheckerContext.getInferredPrimitiveNumericType(expression: KtExpression): KotlinType? {
        val type = trace.bindingContext.getType(expression) ?: return null
        val dataFlowValue = DataFlowValueFactory.createDataFlowValue(
            expression, type, trace.bindingContext, resolutionContext.scope.ownerDescriptor
        )
        val dataFlowInfo = trace.get(BindingContext.EXPRESSION_TYPE_INFO, expression)?.dataFlowInfo ?: return null
        val stableTypes = dataFlowInfo.getStableTypes(dataFlowValue, languageVersionSettings)
        return (listOf(type) + stableTypes).findPrimitiveType()
    }

    private fun List<KotlinType>.findPrimitiveType() =
        find { it.isPrimitiveNumberOrNullableType() }?.makeNotNullable()
}