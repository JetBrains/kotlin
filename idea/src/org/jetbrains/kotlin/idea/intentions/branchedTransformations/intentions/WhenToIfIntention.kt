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
        if (element.getEntries().isEmpty()) return false
        return element.getWhenKeywordElement().getTextRange().containsOffset(caretOffset)
    }

    override fun applyTo(element: JetWhenExpression, editor: Editor) {
        val builder = JetPsiFactory(element).IfChainBuilder()

        for (entry in element.getEntries()) {
            val branch = entry.getExpression()
            if (entry.isElse()) {
                builder.elseBranch(branch)
            }
            else {
                val branchConditionText = combineWhenConditions(entry.getConditions(), element.getSubjectExpression())
                builder.ifBranch(branchConditionText, JetPsiUtil.getText(branch))
            }
        }

        element.replace(builder.toExpression())
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
