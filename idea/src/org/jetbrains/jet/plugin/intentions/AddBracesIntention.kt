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

import org.jetbrains.jet.lang.psi.JetIfExpression
import org.jetbrains.jet.lang.psi.JetWhileExpression
import org.jetbrains.jet.lang.psi.JetForExpression
import org.jetbrains.jet.lang.psi.JetDoWhileExpression
import org.jetbrains.jet.lang.psi.JetExpressionImpl
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetBlockExpression
import com.intellij.psi.PsiElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.lang.ASTNode
import org.jetbrains.jet.JetNodeTypes
import com.intellij.psi.PsiWhiteSpace

public class AddBracesIntention : JetSelfTargetingIntention<JetExpressionImpl>("add.braces", javaClass()) {
    private var expressionKind: ExpressionKind? = null
    private var caretLocation: Int = 1

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        caretLocation = editor.getCaretModel().getOffset()
        return getTarget(editor, file) != null
    }

    override fun isApplicableTo(element: JetExpressionImpl): Boolean {
        expressionKind = element.getExpressionKind(caretLocation)
        if (expressionKind == null) return false

        val jetBlockElement = element.findBlockInExpression(expressionKind)
        if (jetBlockElement != null) return false

        setText("Add braces to '${expressionKind!!.text}' statement")
        return true
    }

    override fun applyTo(element: JetExpressionImpl, editor: Editor) {
        val bodyNode = when (expressionKind) {
            ExpressionKind.ELSE -> element.getNode().findChildByType(JetNodeTypes.ELSE)
            ExpressionKind.IF -> element.getNode().findChildByType(JetNodeTypes.THEN)
            else -> element.getNode().findChildByType(JetNodeTypes.BODY)
        }
        generateCleanOutput(element, bodyNode)
    }

    fun generateCleanOutput(element: JetExpressionImpl, bodyNode: ASTNode?) {
        if (element.getNextSibling()?.getText() == ";") {
            element.getNextSibling()!!.delete()
        }
        val newElement = bodyNode!!.getPsi()!!.replace(JetPsiFactory.createFunctionBody(element.getProject(), bodyNode.getText()))

        //handles the case of the block statement being on a new line
        if (newElement.getPrevSibling() is PsiWhiteSpace) {
            newElement.getPrevSibling()!!.replace(JetPsiFactory.createWhiteSpace(element.getProject()))
        } else {
            //handles the case of no space between condition and statement
            newElement.addBefore(JetPsiFactory.createWhiteSpace(element.getProject()), newElement.getFirstChild())
        }
        if (expressionKind == ExpressionKind.DOWHILE) {
            newElement.getNextSibling()?.delete()
        }
    }
}
