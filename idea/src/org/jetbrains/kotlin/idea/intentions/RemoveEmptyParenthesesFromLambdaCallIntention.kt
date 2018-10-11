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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInspection.CleanupLocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.conversion.copy.range
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments

class RemoveEmptyParenthesesFromLambdaCallInspection : IntentionBasedInspection<KtValueArgumentList>(
    RemoveEmptyParenthesesFromLambdaCallIntention::class
), CleanupLocalInspectionTool {
    override fun problemHighlightType(element: KtValueArgumentList): ProblemHighlightType =
        ProblemHighlightType.LIKE_UNUSED_SYMBOL
}

class RemoveEmptyParenthesesFromLambdaCallIntention : SelfTargetingRangeIntention<KtValueArgumentList>(
    KtValueArgumentList::class.java, "Remove unnecessary parentheses from function call with lambda"
) {

    override fun applicabilityRange(element: KtValueArgumentList): TextRange? {
        if (element.arguments.isNotEmpty()) return null
        val parent = element.parent as? KtCallExpression ?: return null
        val singleLambdaArgument = parent.lambdaArguments.singleOrNull() ?: return null
        if (element.getLineNumber(start = false) != singleLambdaArgument.getLineNumber(start = true)) return null
        if (element.getPrevSiblingIgnoringWhitespaceAndComments() !is KtCallExpression) return element.range
        return null
    }

    override fun applyTo(element: KtValueArgumentList, editor: Editor?) {
        element.delete()
    }
}
