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

public class MergeElseIfIntention : JetSelfTargetingIntention<JetIfExpression> ("merge.else.if", javaClass()) {
    private var expressionKind: ExpressionKind? = null
    private var caretLocation: Int = 1

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        caretLocation = editor.getCaretModel().getOffset()
        return getTarget(editor, file) != null
    }

    override fun isApplicableTo(element: JetIfExpression): Boolean {
        expressionKind = element.getExpressionKind(caretLocation)
        if (expressionKind == null) return false
        if (expressionKind!!.text != "else") return false

        val elseStatement = element.getElse()!!
        val children = elseStatement.getChildren()

        if (children.size != 1) return false
        if (!(children[0] is JetIfExpression)) return false

        return true
    }

    override fun applyTo(element: JetIfExpression, editor: Editor) {
        val elseStatement = element.getElse()!!
        val innerIfStatement2 = elseStatement.getChildren()[0]
        val newElseStatement = innerIfStatement2.copy() as JetIfExpression
        element.replace(JetPsiFactory.createIf(element.getProject(), element.getCondition(), element.getThen(), newElseStatement))
    }
}