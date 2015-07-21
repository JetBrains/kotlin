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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetPostfixExpression
import org.jetbrains.kotlin.psi.JetPsiFactory

public abstract class ExclExclCallFix : IntentionAction {
    override fun getFamilyName(): String = getText()

    override fun startInWriteAction(): Boolean = true

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile) = file is JetFile
}

public class RemoveExclExclCallFix(val psiElement: PsiElement) : ExclExclCallFix(), CleanupFix {
    override fun getText(): String = JetBundle.message("remove.unnecessary.non.null.assertion")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean
        = super<ExclExclCallFix>.isAvailable(project, editor, file) &&
                  getExclExclPostfixExpression() != null

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) return

        val postfixExpression = getExclExclPostfixExpression() ?: return
        val expression = JetPsiFactory(project).createExpression(postfixExpression.getBaseExpression()!!.getText())
        postfixExpression.replace(expression)
    }

    private fun getExclExclPostfixExpression(): JetPostfixExpression? {
        val operationParent = psiElement.getParent()
        if (operationParent is JetPostfixExpression && operationParent.getBaseExpression() != null) {
            return operationParent
        }
        return null
    }

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction
            = RemoveExclExclCallFix(diagnostic.getPsiElement())
    }
}

public class AddExclExclCallFix(val psiElement: PsiElement) : ExclExclCallFix() {
    override fun getText() = JetBundle.message("introduce.non.null.assertion")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean
            = super.isAvailable(project, editor, file) &&
              getExpressionForIntroduceCall() != null

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val modifiedExpression = getExpressionForIntroduceCall() ?: return
        val exclExclExpression = JetPsiFactory(project).createExpression(modifiedExpression.getText() + "!!")
        modifiedExpression.replace(exclExclExpression)
    }

    protected fun getExpressionForIntroduceCall(): JetExpression? {
        if (psiElement is LeafPsiElement && psiElement.getElementType() == JetTokens.DOT) {
            val sibling = psiElement.getPrevSibling()
            if (sibling is JetExpression) {
                return sibling
            }
        }

        return null
    }

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction
                = AddExclExclCallFix(diagnostic.getPsiElement())
    }
}
