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

import org.jetbrains.jet.lang.psi.JetExpressionImpl
import org.jetbrains.jet.lang.psi.JetPsiFactory
import com.intellij.openapi.editor.Editor
import com.intellij.lang.ASTNode
import org.jetbrains.jet.JetNodeTypes
import com.intellij.psi.PsiWhiteSpace

public class AddBracesIntention : JetSelfTargetingIntention<JetExpressionImpl>("add.braces", javaClass()) {
    override fun isApplicableTo(element: JetExpressionImpl): Boolean {
        throw IllegalStateException("isApplicableTo(JetExpressionImpl, Editor) should be called instead")
    }

    override fun isApplicableTo(element: JetExpressionImpl, editor: Editor): Boolean {
        val expressionKind = element.getExpressionKind(editor.getCaretModel().getOffset())
        if (expressionKind == null) return false

        val jetBlockElement = element.findBlockInExpression(expressionKind)
        if (jetBlockElement != null) return false

        setText("Add braces to '${expressionKind.text}' statement")
        return true
    }

    override fun applyTo(element: JetExpressionImpl, editor: Editor) {
        val expressionKind = element.getExpressionKind(editor.getCaretModel().getOffset())!!
        val bodyNode = when (expressionKind) {
            ExpressionKind.ELSE -> element.getNode().findChildByType(JetNodeTypes.ELSE)
            ExpressionKind.IF -> element.getNode().findChildByType(JetNodeTypes.THEN)
            else -> element.getNode().findChildByType(JetNodeTypes.BODY)
        }
        generateCleanOutput(element, bodyNode, expressionKind)
    }

    fun generateCleanOutput(element: JetExpressionImpl, bodyNode: ASTNode?, expressionKind: ExpressionKind) {
        if (element.getNextSibling()?.getText() == ";") {
            element.getNextSibling()!!.delete()
        }
        val psiFactory = JetPsiFactory(element)
        val newElement = bodyNode!!.getPsi()!!.replace(psiFactory.createFunctionBody(bodyNode.getText()))

        //handles the case of the block statement being on a new line
        if (newElement.getPrevSibling() is PsiWhiteSpace) {
            newElement.getPrevSibling()!!.replace(psiFactory.createWhiteSpace())
        } else {
            //handles the case of no space between condition and statement
            newElement.addBefore(psiFactory.createWhiteSpace(), newElement.getFirstChild())
        }
        if (expressionKind == ExpressionKind.DOWHILE) {
            newElement.getNextSibling()?.delete()
        }
    }
}
