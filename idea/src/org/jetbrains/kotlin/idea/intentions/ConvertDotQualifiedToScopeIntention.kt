/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression

abstract class ConvertDotQualifiedToScopeIntention(
    text: String
) : ConvertToScopeIntention<KtDotQualifiedExpression>(KtDotQualifiedExpression::class.java, text) {

    override fun isApplicableTo(element: KtDotQualifiedExpression, caretOffset: Int): Boolean {
        val receiverExpression = element.getLeftMostReceiverExpression()
        if (receiverExpression.mainReference?.resolve() is PsiClass) return false
        val receiverExpressionText = receiverExpression.text
        if (receiverExpressionText in BLACKLIST_RECEIVER_NAME) return false
        if (!isApplicableWithGivenReceiverText(element, receiverExpressionText)) return false
        val nextSibling = element.getDotQualifiedSiblingIfAny(forward = true)
        if (nextSibling != null && isApplicableWithGivenReceiverText(nextSibling, receiverExpressionText)) return true
        val prevSibling = element.getDotQualifiedSiblingIfAny(forward = false)
        return prevSibling != null && isApplicableWithGivenReceiverText(prevSibling, receiverExpressionText)
    }

    override fun applyTo(element: KtDotQualifiedExpression, editor: Editor?) {
        val receiverExpressionText = element.getReceiverExpressionText() ?: return
        applyWithGivenReceiverText(element, receiverExpressionText)
    }
}