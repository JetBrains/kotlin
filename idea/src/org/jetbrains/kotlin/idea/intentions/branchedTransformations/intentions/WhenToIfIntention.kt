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
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.combineWhenConditions
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.buildExpression

class WhenToIfIntention : SelfTargetingRangeIntention<KtWhenExpression>(KtWhenExpression::class.java, "Replace 'when' with 'if'"), LowPriorityAction {
    override fun applicabilityRange(element: KtWhenExpression): TextRange? {
        val entries = element.entries
        if (entries.isEmpty()) return null
        val lastEntry = entries.last()
        if (entries.any { it != lastEntry && it.isElse }) return null
        if (entries.all { it.isElse }) return null // 'when' with only 'else' branch is not supported
        return element.whenKeyword.textRange
    }

    override fun applyTo(element: KtWhenExpression, editor: Editor?) {
        val commentSaver = CommentSaver(element)

        val factory = KtPsiFactory(element)
        val ifExpression = factory.buildExpression {
            val entries = element.entries
            for ((i, entry) in entries.withIndex()) {
                if (i > 0) {
                    appendFixedText("else ")
                }
                val branch = entry.expression
                if (entry.isElse) {
                    appendExpression(branch)
                }
                else {
                    val condition = factory.combineWhenConditions(entry.conditions, element.subjectExpression)
                    appendFixedText("if (")
                    appendExpression(condition)
                    appendFixedText(")")
                    if (branch is KtIfExpression) {
                        appendFixedText("{ ")
                    }
                    appendExpression(branch)
                    if (branch is KtIfExpression) {
                        appendFixedText(" }")
                    }
                }
                if (i != entries.lastIndex) {
                    appendFixedText("\n")
                }
            }
        }

        val result = element.replace(ifExpression)
        commentSaver.restore(result)
    }
}
