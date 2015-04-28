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
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

public class RemoveBracesIntention : JetSelfTargetingIntention<JetBlockExpression>(javaClass(), "Remove braces") {
    override fun isApplicableTo(element: JetBlockExpression, caretOffset: Int): Boolean {
        if (element.getStatements().size() != 1) return false

        val containerNode = element.getParent() as? JetContainerNode ?: return false

        val lBrace = element.getLBrace() ?: return false
        val rBrace = element.getRBrace() ?: return false
        if (!lBrace.getTextRange().containsOffset(caretOffset) && !rBrace.getTextRange().containsOffset(caretOffset)) return false

        val description = containerNode.description() ?: return false
        setText("Remove braces from '$description' statement")
        return true
    }

    override fun applyTo(element: JetBlockExpression, editor: Editor) {
        val statement = element.getStatements().single()

        val containerNode = element.getParent() as JetContainerNode
        val construct = containerNode.getParent() as JetExpression
        handleComments(construct, element)

        val newElement = element.replace(statement.copy())

        if (construct is JetDoWhileExpression) {
            newElement.getParent()!!.addAfter(JetPsiFactory(element).createNewLine(), newElement)
        }
    }

    private fun handleComments(construct: JetExpression, block: JetBlockExpression) {
        var sibling = block.getFirstChild()?.getNextSibling()

        while (sibling != null) {
            if (sibling is PsiComment) {
                //cleans up extra whitespace
                val psiFactory = JetPsiFactory(construct)
                if (construct.getPrevSibling() is PsiWhiteSpace) {
                    construct.getPrevSibling()!!.replace(psiFactory.createNewLine())
                }
                val commentElement = construct.getParent()!!.addBefore(sibling, construct.getPrevSibling())
                construct.getParent()!!.addBefore(psiFactory.createNewLine(), commentElement)
            }
            sibling = sibling.getNextSibling()
        }
    }
}
