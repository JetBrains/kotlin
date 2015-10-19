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

public class AddBracesIntention : JetSelfTargetingIntention<KtExpression>(javaClass(), "Add braces") {
    override fun isApplicableTo(element: KtExpression, caretOffset: Int): Boolean {
        val expression = element.getTargetExpression(caretOffset) ?: return false
        if (expression is KtBlockExpression) return false

        val description = (expression.getParent() as KtContainerNode).description()!!
        setText("Add braces to '$description' statement")
        return true
    }

    override fun applyTo(element: KtExpression, editor: Editor) {
        val expression = element.getTargetExpression(editor.getCaretModel().getOffset())!!

        if (element.getNextSibling()?.getText() == ";") {
            element.getNextSibling()!!.delete()
        }

        val psiFactory = KtPsiFactory(element)
        expression.replace(psiFactory.createSingleStatementBlock(expression))

        if (element is KtDoWhileExpression) { // remove new line between '}' and while
            (element.getBody()!!.getParent().getNextSibling() as? PsiWhiteSpace)?.delete()
        }
    }

    private fun KtExpression.getTargetExpression(caretLocation: Int): KtExpression? {
        when (this) {
            is KtIfExpression -> {
                val thenExpr = getThen() ?: return null
                val elseExpr = getElse()
                if (elseExpr != null && caretLocation >= getElseKeyword()!!.startOffset) {
                    return elseExpr
                }
                return thenExpr
            }

            is KtWhileExpression -> return getBody()

            is KtDoWhileExpression -> return getBody()

            is KtForExpression -> return getBody()

            else -> return null
        }
    }
}
