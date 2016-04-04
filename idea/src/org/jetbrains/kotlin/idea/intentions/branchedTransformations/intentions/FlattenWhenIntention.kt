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
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.intentions.SelfTargetingIntention
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.util.psi.patternMatching.matches
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class FlattenWhenIntention : SelfTargetingIntention<KtWhenExpression>(KtWhenExpression::class.java, "Flatten 'when' expression") {
    override fun isApplicableTo(element: KtWhenExpression, caretOffset: Int): Boolean {
        val subject = element.subjectExpression
        if (subject != null && subject !is KtNameReferenceExpression) return false

        if (!KtPsiUtil.checkWhenExpressionHasSingleElse(element)) return false

        val elseEntry = element.entries.singleOrNull { it.isElse } ?: return false

        val innerWhen = elseEntry.expression as? KtWhenExpression ?: return false

        if (!subject.matches(innerWhen.subjectExpression)) return false
        if (!KtPsiUtil.checkWhenExpressionHasSingleElse(innerWhen)) return false

        return elseEntry.startOffset <= caretOffset && caretOffset <= innerWhen.whenKeyword.endOffset
    }

    override fun applyTo(element: KtWhenExpression, editor: Editor?) {
        val subjectExpression = element.subjectExpression
        val nestedWhen = element.elseExpression as KtWhenExpression

        val outerEntries = element.entries
        val innerEntries = nestedWhen.entries

        val whenExpression = KtPsiFactory(element).buildExpression {
            appendFixedText("when")
            if (subjectExpression != null) {
                appendFixedText("(").appendExpression(subjectExpression).appendFixedText(")")
            }
            appendFixedText("{\n")

            for (entry in outerEntries) {
                if (entry.isElse) continue
                appendNonFormattedText(entry.text)
                appendFixedText("\n")
            }
            for (entry in innerEntries) {
                appendNonFormattedText(entry.text)
                appendFixedText("\n")
            }

            appendFixedText("}")
        } as KtWhenExpression

        val newWhen = element.replaced(whenExpression)

        val firstNewEntry = newWhen.entries[outerEntries.size - 1]
        editor?.moveCaret(firstNewEntry.textOffset)
    }
}
