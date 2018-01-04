/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.parameterInfo

import com.intellij.codeInsight.hints.InlayInfo
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

    if (expression.parent is KtDotQualifiedExpression || expression.parent is KtSafeQualifiedExpression) {
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
