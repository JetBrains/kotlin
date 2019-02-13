/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.ir.expressions.IrDynamicOperator
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.util.OperatorNameConventions

internal fun ResolvedCall<*>.isImplicitInvoke(): Boolean {
    if (resultingDescriptor.name != OperatorNameConventions.INVOKE) return false
    val callExpression = call.callElement as? KtCallExpression ?: return false
    val calleeExpression = callExpression.calleeExpression as? KtSimpleNameExpression ?: return true
    return calleeExpression.getReferencedName() != OperatorNameConventions.INVOKE.asString()
}

internal fun ResolvedCall<*>.isImplicitGet(): Boolean =
    resultingDescriptor.name == OperatorNameConventions.GET && call.callElement is KtArrayAccessExpression

internal fun ResolvedCall<*>.isImplicitSet(): Boolean =
    resultingDescriptor.name == OperatorNameConventions.SET && call.callElement is KtArrayAccessExpression

internal fun KtElement.getDynamicOperator(): IrDynamicOperator {
    return when (this) {
        is KtUnaryExpression ->
            when (operationToken) {
                KtTokens.PLUS -> IrDynamicOperator.UNARY_PLUS
                KtTokens.MINUS -> IrDynamicOperator.UNARY_MINUS
                KtTokens.EXCL -> IrDynamicOperator.EXCL
                else -> throw AssertionError("Unexpected unary operator expression: $text")
            }
        is KtBinaryExpression ->
            when (operationToken) {
                KtTokens.PLUS -> IrDynamicOperator.BINARY_PLUS
                KtTokens.MINUS -> IrDynamicOperator.BINARY_MINUS
                KtTokens.MUL -> IrDynamicOperator.MUL
                KtTokens.DIV -> IrDynamicOperator.DIV
                KtTokens.PERC -> IrDynamicOperator.MOD
                KtTokens.LT -> IrDynamicOperator.LT
                KtTokens.LTEQ -> IrDynamicOperator.LE
                KtTokens.GT -> IrDynamicOperator.GT
                KtTokens.GTEQ -> IrDynamicOperator.GE
                KtTokens.ANDAND -> IrDynamicOperator.ANDAND
                KtTokens.OROR -> IrDynamicOperator.OROR
                KtTokens.EQEQ -> IrDynamicOperator.EQEQ
                KtTokens.EQEQEQ -> IrDynamicOperator.EQEQEQ
                KtTokens.EXCLEQ -> IrDynamicOperator.EXCLEQ
                KtTokens.EXCLEQEQEQ -> IrDynamicOperator.EXCLEQEQ
                else -> throw AssertionError("Unexpected binary operator expression: $text")
            }
        else -> throw AssertionError("Unexpected operator expression: $text")
    }
}