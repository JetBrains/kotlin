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
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.toExpression
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.JetWhenExpression
import org.jetbrains.kotlin.psi.buildExpression
import org.jetbrains.kotlin.psi.psiUtil.startOffset

public class EliminateWhenSubjectIntention : JetSelfTargetingIntention<JetWhenExpression>(javaClass(), "Eliminate argument of 'when'") {
    override fun isApplicableTo(element: JetWhenExpression, caretOffset: Int): Boolean {
        if (element.getSubjectExpression() !is JetSimpleNameExpression) return false
        val lBrace = element.getOpenBrace() ?: return false
        return caretOffset <= lBrace.startOffset
    }

    override fun applyTo(element: JetWhenExpression, editor: Editor) {
        val subject = element.getSubjectExpression()!!

        val whenExpression = JetPsiFactory(element).buildExpression {
            appendFixedText("when {\n")

            for (entry in element.getEntries()) {
                val branchExpression = entry.getExpression()

                if (entry.isElse()) {
                    appendFixedText("else")
                }
                else {
                    for ((i, condition) in entry.getConditions().withIndex()) {
                        if (i > 0) appendFixedText(",")
                        appendExpression(condition.toExpression(subject))
                    }
                }
                appendFixedText("->")

                appendExpression(branchExpression)
                appendFixedText("\n")
            }

            appendFixedText("}")
        }

        element.replace(whenExpression)
    }
}
