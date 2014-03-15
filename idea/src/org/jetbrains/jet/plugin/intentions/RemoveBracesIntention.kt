/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.intentions

import org.jetbrains.jet.lang.psi.JetBlockExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetWhileExpression
import org.jetbrains.jet.lang.psi.JetIfExpression
import org.jetbrains.jet.lang.psi.JetDoWhileExpression
import org.jetbrains.jet.lang.psi.JetForExpression
import org.jetbrains.jet.lang.psi.JetExpressionImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.PsiComment

public class RemoveBracesIntention : JetSelfTargetingIntention<JetExpressionImpl>("remove.braces", javaClass()) {
    private var caretLocation: Int = 1

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        caretLocation = editor.getCaretModel().getOffset()
        return getTarget(editor, file) != null
    }

    override fun isApplicableTo(element: JetExpressionImpl): Boolean {
        val expressionKind = element.getExpressionKind(caretLocation)
        if (expressionKind == null) return false

        val jetBlockElement = element.findBlockInExpression(expressionKind)
        if (jetBlockElement == null) return false

        if (jetBlockElement.getStatements().size == 1) {
            setText("Remove braces from '${expressionKind.text}' statement")
            return true
        }
        return false
    }

    override fun applyTo(element: JetExpressionImpl, editor: Editor) {
        val expressionKind = element.getExpressionKind(caretLocation)
        val jetBlockElement = element.findBlockInExpression(expressionKind)
        val firstStatement = jetBlockElement!!.getStatements().first()

        handleComments(element, jetBlockElement)

        val newElement = jetBlockElement.replace(JetPsiFactory.createExpression(element.getProject(), firstStatement.getText()))

        if (expressionKind == ExpressionKind.DOWHILE) {
            newElement.getParent()!!.addAfter(JetPsiFactory.createNewLine(element.getProject()), newElement)
        }

    }

    fun handleComments(element: JetExpressionImpl, blockElement: JetBlockExpression) {
        var sibling = blockElement.getFirstChild()?.getNextSibling()

        while (sibling != null) {
            if (sibling is PsiComment) {
                //cleans up extra whitespace
                if (element.getPrevSibling() is PsiWhiteSpace) {
                    element.getPrevSibling()!!.replace(JetPsiFactory.createNewLine(element.getProject()))
                }
                val commentElement = element.getParent()!!.addBefore(sibling as PsiComment, element.getPrevSibling())
                element.getParent()!!.addBefore(JetPsiFactory.createNewLine(element.getProject()), commentElement)
            }
            sibling = sibling!!.getNextSibling()
        }
    }
}