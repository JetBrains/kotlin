/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression

class AddLabeledReturnInLambdaIntention : SelfTargetingRangeIntention<KtLambdaExpression>(
    KtLambdaExpression::class.java,
    "Add labeled return to last expression in a lambda"
), LowPriorityAction {
    override fun applicabilityRange(element: KtLambdaExpression): TextRange? {
        if (!isApplicableTo(element)) return null
        val labelName = createLabelName(element) ?: return null
        text = "Add return@$labelName"
        return element.bodyExpression?.statements?.last()?.textRange
    }

    override fun applyTo(element: KtLambdaExpression, editor: Editor?) {
        if (!isApplicableTo(element)) return
        val labelName = createLabelName(element) ?: return
        val lastStatement = element.bodyExpression?.statements?.last() ?: return
        val newExpression = KtPsiFactory(element.project).createExpressionByPattern("return@$labelName $0", lastStatement)
        lastStatement.replace(newExpression)
    }

    private fun isApplicableTo(element: KtLambdaExpression): Boolean {
        val block = element.bodyExpression ?: return false
        val lastStatement = block.statements.last()
        return lastStatement !is KtReturnExpression && lastStatement.isUsedAsExpression(lastStatement.analyze())
    }

    private fun createLabelName(element: KtLambdaExpression): String? {
        val block = element.bodyExpression ?: return null
        val callExpression = element.getStrictParentOfType<KtCallExpression>() ?: return null
        val valueArgument = callExpression.valueArguments.findArgumentWithGivenBlock(block) ?: return null
        val lambdaLabelName = (valueArgument.getArgumentExpression() as? KtLabeledExpression)?.getLabelName()
        return lambdaLabelName ?: callExpression.getCallNameExpression()?.text
    }
}