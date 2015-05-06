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

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.toExpressionText
import org.jetbrains.kotlin.psi.*

public class WhenToIfIntention : JetSelfTargetingIntention<JetWhenExpression>(javaClass(), "Replace 'when' with 'if'") {
    override fun isApplicableTo(element: JetWhenExpression, caretOffset: Int): Boolean {
        val entries = element.getEntries()
        if (entries.isEmpty()) return false
        val lastEntry = entries.last()
        if (entries.any { it != lastEntry && it.isElse() }) return false

        return element.getWhenKeywordElement().getTextRange().containsOffset(caretOffset)
    }

    override fun applyTo(element: JetWhenExpression, editor: Editor) {
        val ifExpression = JetPsiFactory(element).buildExpression {
            for ((i, entry) in element.getEntries().withIndex()) {
                if (i > 0) {
                    appendFixedText("else ")
                }
                val branch = entry.getExpression()
                if (entry.isElse()) {
                    appendExpression(branch)
                    appendFixedText("\n")
                }
                else {
                    val branchConditionText = combineWhenConditions(entry.getConditions(), element.getSubjectExpression())
                    appendFixedText("if (")
                    appendNonFormattedText(branchConditionText)
                    appendFixedText(")")
                    appendExpression(branch)
                    appendFixedText("\n")
                }
            }
        }

        element.replace(ifExpression)
    }

    private fun combineWhenConditions(conditions: Array<JetWhenCondition>, subject: JetExpression?): String {
        return when (conditions.size()) {
            0 -> ""
            1 -> conditions[0].toExpressionText(subject)
            else -> {
                conditions
                        .map { condition -> JetPsiUnparsingUtils.parenthesizeTextIfNeeded(condition.toExpressionText(subject)) }
                        .joinToString(separator = " || ")
            }
        }
    }
}
