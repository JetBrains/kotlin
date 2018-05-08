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
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import java.lang.IllegalArgumentException

class AddBracesIntention : SelfTargetingIntention<KtElement>(KtElement::class.java, "Add braces") {
    override fun isApplicableTo(element: KtElement, caretOffset: Int): Boolean {
        val expression = element.getTargetExpression(caretOffset) ?: return false
        if (expression is KtBlockExpression) return false

        val parent = expression.parent
        return when (parent) {
            is KtContainerNode -> {
                val description = parent.description()!!
                text = "Add braces to '$description' statement"
                true
            }
            is KtWhenEntry -> {
                text = "Add braces to 'when' entry"
                true
            }
            else -> {
                false
            }
        }
    }

    override fun applyTo(element: KtElement, editor: Editor?) {
        if (editor == null) throw IllegalArgumentException("This intention requires an editor")
        val expression = element.getTargetExpression(editor.caretModel.offset)!!

        val psiFactory = KtPsiFactory(element)

        val semicolon = element.getNextSiblingIgnoringWhitespaceAndComments()?.takeIf { it.node.elementType == KtTokens.SEMICOLON }
        if (semicolon != null) {
            val afterSemicolon = semicolon.getNextSiblingIgnoringWhitespace()
            if (semicolon.getLineNumber() == afterSemicolon?.getLineNumber())
                semicolon.replace(psiFactory.createNewLine())
            else
                semicolon.delete()
        }

        val nextComment = when {
            element is KtDoWhileExpression -> null // bound to the closing while
            element is KtIfExpression && expression === element.then && element.`else` != null -> null // bound to else
            else -> element.getNextSiblingIgnoringWhitespace().takeIf { it is PsiComment }
        }
        val saver = if (nextComment == null) CommentSaver(element) else CommentSaver(PsiChildRange(element, nextComment))
        element.allChildren.filterIsInstance<PsiComment>().toList().forEach {
            it.delete()
        }
        nextComment?.delete()

        val result = expression.replace(psiFactory.createSingleStatementBlock(expression))

        if (element is KtDoWhileExpression) { // remove new line between '}' and while
            (element.body!!.parent.nextSibling as? PsiWhiteSpace)?.delete()
        }
        saver.restore(result)
    }

    private fun KtElement.getTargetExpression(caretLocation: Int): KtExpression? {
        return when (this) {
            is KtIfExpression -> {
                val thenExpr = then ?: return null
                val elseExpr = `else`
                if (elseExpr != null && caretLocation >= elseKeyword!!.startOffset) {
                    elseExpr
                } else {
                    thenExpr
                }
            }

            is KtLoopExpression -> body
            is KtWhenEntry -> expression
            else -> null
        }
    }
}
