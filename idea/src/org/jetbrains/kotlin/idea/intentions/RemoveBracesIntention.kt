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
import org.jetbrains.kotlin.psi.*

class RemoveBracesIntention : SelfTargetingIntention<KtBlockExpression>(KtBlockExpression::class.java, "Remove braces") {
    override fun isApplicableTo(element: KtBlockExpression, caretOffset: Int): Boolean {
        val singleStatement = element.statements.singleOrNull() ?: return false
        val container = element.parent
        when (container) {
            is KtContainerNode -> {
                if (singleStatement is KtIfExpression && container.parent is KtIfExpression) return false

                val lBrace = element.lBrace ?: return false
                val rBrace = element.rBrace ?: return false
                if (!lBrace.textRange.containsOffset(caretOffset) && !rBrace.textRange.containsOffset(caretOffset)) return false

                val description = container.description() ?: return false
                text = "Remove braces from '$description' statement"
                return true
            }
            is KtWhenEntry -> {
                text = "Remove braces from 'when' entry"
                return singleStatement !is KtNamedDeclaration
            }
            else -> return false
        }
    }

    override fun applyTo(element: KtBlockExpression, editor: Editor?) {
        val statement = element.statements.single()

        val container = element.parent!!
        val construct = container.parent as KtExpression
        handleComments(construct, element)

        val newElement = element.replace(statement.copy())

        if (construct is KtDoWhileExpression) {
            newElement.parent!!.addAfter(KtPsiFactory(element).createNewLine(), newElement)
        }
    }

    private fun handleComments(construct: KtExpression, block: KtBlockExpression) {
        var sibling = block.firstChild?.nextSibling

        while (sibling != null) {
            if (sibling is PsiComment) {
                //cleans up extra whitespace
                val psiFactory = KtPsiFactory(construct)
                if (construct.prevSibling is PsiWhiteSpace) {
                    construct.prevSibling!!.replace(psiFactory.createNewLine())
                }
                val commentElement = construct.parent!!.addBefore(sibling, construct.prevSibling)
                construct.parent!!.addBefore(psiFactory.createNewLine(), commentElement)
            }
            sibling = sibling.nextSibling
        }
    }
}
