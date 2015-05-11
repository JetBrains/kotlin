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
import org.jetbrains.kotlin.idea.quickfix.moveCaret
import org.jetbrains.kotlin.idea.util.psi.patternMatching.matches
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.replaced
import org.jetbrains.kotlin.psi.psiUtil.startOffset

public class FlattenWhenIntention : JetSelfTargetingIntention<JetWhenExpression>(javaClass(), "Flatten 'when' expression") {
    override fun isApplicableTo(element: JetWhenExpression, caretOffset: Int): Boolean {
        val subject = element.getSubjectExpression()
        if (subject != null && subject !is JetSimpleNameExpression) return false

        if (!JetPsiUtil.checkWhenExpressionHasSingleElse(element)) return false

        val elseEntry = element.getEntries().singleOrNull { it.isElse() } ?: return false

        val innerWhen = elseEntry.getExpression() as? JetWhenExpression ?: return false

        if (!subject.matches(innerWhen.getSubjectExpression())) return false
        if (!JetPsiUtil.checkWhenExpressionHasSingleElse(innerWhen)) return false

        return elseEntry.startOffset <= caretOffset && caretOffset <= innerWhen.getWhenKeyword().endOffset
    }

    override fun applyTo(element: JetWhenExpression, editor: Editor) {
        val subjectExpression = element.getSubjectExpression()
        val nestedWhen = element.getElseExpression() as JetWhenExpression

        val outerEntries = element.getEntries()
        val innerEntries = nestedWhen.getEntries()

        val whenExpression = JetPsiFactory(element).buildExpression {
            appendFixedText("when")
            if (subjectExpression != null) {
                appendFixedText("(").appendExpression(subjectExpression).appendFixedText(")")
            }
            appendFixedText("{\n")

            for (entry in outerEntries) {
                if (entry.isElse()) continue
                appendNonFormattedText(entry.getText())
                appendFixedText("\n")
            }
            for (entry in innerEntries) {
                appendNonFormattedText(entry.getText())
                appendFixedText("\n")
            }

            appendFixedText("}")
        } as JetWhenExpression

        val newWhen = element.replaced(whenExpression)

        val firstNewEntry = newWhen.getEntries()[outerEntries.size() - 1]
        editor.moveCaret(firstNewEntry.getTextOffset())
    }
}
