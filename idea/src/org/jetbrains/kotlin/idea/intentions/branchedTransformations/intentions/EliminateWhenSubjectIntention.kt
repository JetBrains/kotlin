/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.intentions.SelfTargetingIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.toExpression
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.buildExpression
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class EliminateWhenSubjectIntention : SelfTargetingIntention<KtWhenExpression>(KtWhenExpression::class.java, "Eliminate argument of 'when'"), LowPriorityAction {
    override fun isApplicableTo(element: KtWhenExpression, caretOffset: Int): Boolean {
        if (element.subjectExpression !is KtNameReferenceExpression) return false
        val lBrace = element.openBrace ?: return false
        return caretOffset <= lBrace.startOffset
    }

    override fun applyTo(element: KtWhenExpression, editor: Editor?) {
        val subject = element.subjectExpression!!

        val commentSaver = CommentSaver(element, saveLineBreaks = true)

        val whenExpression = KtPsiFactory(element).buildExpression {
            appendFixedText("when {\n")

            for (entry in element.entries) {
                val branchExpression = entry.expression

                if (entry.isElse) {
                    appendFixedText("else")
                }
                else {
                    appendExpressions(entry.conditions.map { it.toExpression(subject) }, separator = "||")
                }
                appendFixedText("->")

                appendExpression(branchExpression)
                appendFixedText("\n")
            }

            appendFixedText("}")
        }

        val result = element.replace(whenExpression)
        commentSaver.restore(result)
    }
}
