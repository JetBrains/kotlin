/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInspection.CleanupLocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.core.util.range
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments

@Suppress("DEPRECATION")
class RemoveEmptyParenthesesFromLambdaCallInspection : IntentionBasedInspection<KtValueArgumentList>(
    RemoveEmptyParenthesesFromLambdaCallIntention::class
), CleanupLocalInspectionTool {
    override fun problemHighlightType(element: KtValueArgumentList): ProblemHighlightType = ProblemHighlightType.LIKE_UNUSED_SYMBOL
}

class RemoveEmptyParenthesesFromLambdaCallIntention : SelfTargetingRangeIntention<KtValueArgumentList>(
    KtValueArgumentList::class.java, KotlinBundle.lazyMessage("remove.unnecessary.parentheses.from.function.call.with.lambda")
) {
    override fun applicabilityRange(element: KtValueArgumentList): TextRange? {
        if (element.arguments.isNotEmpty()) return null
        val parent = element.parent as? KtCallExpression ?: return null
        if (parent.calleeExpression?.text == KtTokens.SUSPEND_KEYWORD.value) return null
        val singleLambdaArgument = parent.lambdaArguments.singleOrNull() ?: return null
        if (element.getLineNumber(start = false) != singleLambdaArgument.getLineNumber(start = true)) return null
        val prev = element.getPrevSiblingIgnoringWhitespaceAndComments()
        if (prev is KtCallExpression || (prev as? KtQualifiedExpression)?.callExpression != null) return null
        return element.range
    }

    override fun applyTo(element: KtValueArgumentList, editor: Editor?) = element.delete()
}
