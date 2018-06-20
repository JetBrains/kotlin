/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsResultOfLambda

fun provideLambdaReturnValueHints(expression: KtExpression): List<InlayInfo> {
    if (expression is KtIfExpression || expression is KtWhenExpression || expression is KtBlockExpression) {
        return emptyList()
    }

    if (!KtPsiUtil.isStatement(expression)) {
        if (!allowLabelOnExpressionPart(expression)) {
            return emptyList()
        }
    } else if (forceLabelOnExpressionPart(expression)) {
        return emptyList()
    }

    val functionLiteral = expression.getParentOfType<KtFunctionLiteral>(true)
    val body = functionLiteral?.bodyExpression ?: return emptyList()
    if (body.statements.size == 1 && body.statements[0] == expression) {
        return emptyList()
    }

    val bindingContext = expression.analyze()
    if (expression.isUsedAsResultOfLambda(bindingContext)) {
        val lambdaName = getNameOfFunctionThatTakesLambda(expression) ?: "lambda"
        return listOf(InlayInfo("$TYPE_INFO_PREFIX^$lambdaName", expression.startOffset))
    }
    return emptyList()
}

private fun getNameOfFunctionThatTakesLambda(expression: KtExpression): String? {
    val lambda = expression.getStrictParentOfType<KtLambdaExpression>() ?: return null
    val callExpression = lambda.getStrictParentOfType<KtCallExpression>() ?: return null
    if (callExpression.lambdaArguments.any { it.getLambdaExpression() == lambda }) {
        val parent = lambda.parent
        if (parent is KtLabeledExpression) {
            return parent.getLabelName()
        }
        return (callExpression.calleeExpression as? KtNameReferenceExpression)?.getReferencedName()
    }
    return null
}

private fun allowLabelOnExpressionPart(expression: KtExpression): Boolean {
    val parent = expression.parent as? KtExpression ?: return false
    return expression == expressionStatementPart(parent)
}

private fun forceLabelOnExpressionPart(expression: KtExpression): Boolean {
    return expressionStatementPart(expression) != null
}

private fun expressionStatementPart(expression: KtExpression): KtExpression? {
    val splitPart: KtExpression = when (expression) {
        is KtAnnotatedExpression -> expression.baseExpression
        is KtLabeledExpression -> expression.baseExpression
        else -> null
    } ?: return null

    if (!isNewLineBeforeExpression(splitPart)) {
        return null
    }

    return splitPart
}

private fun isNewLineBeforeExpression(expression: KtExpression): Boolean {
    val whiteSpace = expression.node.treePrev?.psi as? PsiWhiteSpace ?: return false
    return whiteSpace.text.contains("\n")
}
