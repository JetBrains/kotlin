/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression

class AddLabeledReturnInLambdaIntention : SelfTargetingRangeIntention<KtBlockExpression>(
    KtBlockExpression::class.java,
    "Add labeled return to last expression in a lambda"
), LowPriorityAction {
    override fun applicabilityRange(element: KtBlockExpression): TextRange? {
        if (!isApplicableTo(element)) return null
        val labelName = element.getParentLambdaLabelName() ?: return null
        if (labelName == KtTokens.SUSPEND_KEYWORD.value) return null
        text = "Add return@$labelName"
        return element.statements.lastOrNull()?.textRange
    }

    override fun applyTo(element: KtBlockExpression, editor: Editor?) {
        val labelName = element.getParentLambdaLabelName() ?: return
        val lastStatement = element.statements.lastOrNull() ?: return
        val newExpression = KtPsiFactory(element.project).createExpressionByPattern("return@$labelName $0", lastStatement)
        lastStatement.replace(newExpression)
    }

    private fun isApplicableTo(block: KtBlockExpression): Boolean {
        val lastStatement = block.statements.lastOrNull()
        return lastStatement !is KtReturnExpression && lastStatement?.isUsedAsExpression(lastStatement.analyze()) == true
    }
}