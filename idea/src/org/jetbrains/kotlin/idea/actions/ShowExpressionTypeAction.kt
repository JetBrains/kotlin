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

package org.jetbrains.kotlin.idea.actions

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.JetLanguage
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.JetType

public class ShowExpressionTypeAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData<Editor>(CommonDataKeys.EDITOR)
        val psiFile = e.getData<PsiFile>(CommonDataKeys.PSI_FILE)
        assert(editor != null && psiFile != null)
        var expression: JetExpression?
        val bindingContext = (psiFile as JetFile).analyzeFully()
        if (editor!!.getSelectionModel().hasSelection()) {
            val startOffset = editor.getSelectionModel().getSelectionStart()
            val endOffset = editor.getSelectionModel().getSelectionEnd()
            expression = CodeInsightUtilCore.findElementInRange<JetExpression>(psiFile, startOffset, endOffset, javaClass<JetExpression>(), JetLanguage.INSTANCE)
        }
        else {
            val offset = editor.getCaretModel().getOffset()
            expression = PsiTreeUtil.getParentOfType<JetExpression>(psiFile.findElementAt(offset), javaClass<JetExpression>())
            while (expression != null && bindingContext.getType(expression) == null) {
                expression = PsiTreeUtil.getParentOfType<JetExpression>(expression, javaClass<JetExpression>())
            }
            if (expression != null) {
                editor.getSelectionModel().setSelection(expression.getTextRange().getStartOffset(), expression.getTextRange().getEndOffset())
            }
        }
        if (expression != null) {
            val type = bindingContext.getType(expression)
            if (type != null) {
                HintManager.getInstance().showInformationHint(editor, "<html>" + DescriptorRenderer.HTML.renderType(type) + "</html>")
            }
        }
    }

    override fun update(e: AnActionEvent?) {
        val editor = e!!.getData<Editor>(CommonDataKeys.EDITOR)
        val psiFile = e.getData<PsiFile>(CommonDataKeys.PSI_FILE)
        e.getPresentation().setEnabled(editor != null && psiFile is JetFile)
    }
}
