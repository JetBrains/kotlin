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
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.toExpression
import org.jetbrains.kotlin.psi.*

public class WhenToIfIntention : SelfTargetingRangeIntention<KtWhenExpression>(javaClass(), "Replace 'when' with 'if'"), LowPriorityAction {
    override fun applicabilityRange(element: KtWhenExpression): TextRange? {
        val entries = element.getEntries()
        if (entries.isEmpty()) return null
        val lastEntry = entries.last()
        if (entries.any { it != lastEntry && it.isElse() }) return null
        return element.getWhenKeyword().getTextRange()
    }

    override fun applyTo(element: KtWhenExpression, editor: Editor) {
        val factory = KtPsiFactory(element)
        val ifExpression = factory.buildExpression {
            val entries = element.getEntries()
            for ((i, entry) in entries.withIndex()) {
                if (i > 0) {
                    appendFixedText("else ")
                }
                val branch = entry.getExpression()
                if (entry.isElse()) {
                    appendExpression(branch)
                }
                else {
                    val condition = factory.combineWhenConditions(entry.getConditions(), element.getSubjectExpression())
                    appendFixedText("if (")
                    appendExpression(condition)
                    appendFixedText(")")
                    appendExpression(branch)
                }
                if (i != entries.lastIndex) {
                    appendFixedText("\n")
                }
            }
        }

        element.replace(ifExpression)
    }

    private fun KtPsiFactory.combineWhenConditions(conditions: Array<KtWhenCondition>, subject: KtExpression?): KtExpression? {
        when (conditions.size()) {
            0 -> return null

            1 -> return conditions[0].toExpression(subject)

            else -> {
                return buildExpression {
                    appendExpressions(conditions.map { it.toExpression(subject) }, separator = "||")
                }
            }
        }
    }
}
