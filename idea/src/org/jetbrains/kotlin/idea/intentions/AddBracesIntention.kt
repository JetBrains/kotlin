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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.j2k.isInSingleLine
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.nextLeafs
import org.jetbrains.kotlin.psi.psiUtil.prevLeafs
import org.jetbrains.kotlin.psi.psiUtil.startOffset
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

        if (element.nextSibling?.text == ";") {
            element.nextSibling!!.delete()
        }

        val (prevComment, nextComment) = deleteCommentsOnSameLine(expression, element)

        val psiFactory = KtPsiFactory(element)
        expression.replace(psiFactory.createSingleStatementBlock(expression, prevComment, nextComment))

        if (element is KtDoWhileExpression) { // remove new line between '}' and while
            (element.body!!.parent.nextSibling as? PsiWhiteSpace)?.delete()
        }
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

    private fun deleteCommentsOnSameLine(expression: KtExpression, element: KtElement): Pair<String?, String?> {
        val lineNumber = expression.getLineNumber()

        val prevComments = getCommentsOnSameLine(lineNumber, expression.prevLeafs).reversed()
        val prevCommentText = createCommentText(prevComments)

        val nextLeafs = when {
            expression.parent.node.elementType == KtNodeTypes.THEN && (element as? KtIfExpression)?.`else` != null -> expression.nextLeafs
            element is KtDoWhileExpression -> expression.nextLeafs
            else -> expression.nextLeafs
        }
        val nextComments = getCommentsOnSameLine(lineNumber, nextLeafs)
        val nextCommentText = createCommentText(nextComments)

        (prevComments + nextComments).forEach { (it as? PsiComment)?.delete() }

        return prevCommentText to nextCommentText
    }

    private fun getCommentsOnSameLine(lineNumber: Int, elements: Sequence<PsiElement>): List<PsiElement> {
        return elements
            .takeWhile { (it is PsiWhiteSpace || it is PsiComment) && lineNumber == it.getLineNumber() && it.isInSingleLine() }
            .dropWhile { it is PsiWhiteSpace }
            .toList()
            .dropLastWhile { it is PsiWhiteSpace }
    }

    private fun createCommentText(comments: List<PsiElement>): String? =
        if (comments.isEmpty()) null else comments.joinToString("") { it.text }

}
