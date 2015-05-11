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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset

public class AddBracesIntention : JetSelfTargetingIntention<JetExpression>(javaClass(), "Add braces") {
    override fun isApplicableTo(element: JetExpression, caretOffset: Int): Boolean {
        val expression = element.getTargetExpression(caretOffset) ?: return false
        if (expression is JetBlockExpression) return false

        val description = (expression.getParent() as JetContainerNode).description()!!
        setText("Add braces to '$description' statement")
        return true
    }

    override fun applyTo(element: JetExpression, editor: Editor) {
        val expression = element.getTargetExpression(editor.getCaretModel().getOffset())!!

        if (element.getNextSibling()?.getText() == ";") {
            element.getNextSibling()!!.delete()
        }

        val psiFactory = JetPsiFactory(element)
        expression.replace(psiFactory.createFunctionBody(expression.getText()))

        if (element is JetDoWhileExpression) { // remove new line between '}' and while
            (element.getBody()!!.getParent().getNextSibling() as? PsiWhiteSpace)?.delete()
        }
    }

    private fun JetExpression.getTargetExpression(caretLocation: Int): JetExpression? {
        when (this) {
            is JetIfExpression -> {
                val thenExpr = getThen() ?: return null
                val elseExpr = getElse()
                if (elseExpr != null && caretLocation >= getElseKeyword()!!.startOffset) {
                    return elseExpr
                }
                return thenExpr
            }

            is JetWhileExpression -> return getBody()

            is JetDoWhileExpression -> return getBody()

            is JetForExpression -> return getBody()

            else -> return null
        }
    }
}
