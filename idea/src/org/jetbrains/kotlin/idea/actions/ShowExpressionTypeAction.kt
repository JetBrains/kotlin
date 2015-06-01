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
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.JetLanguage
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.JetCallableDeclaration
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.JetType

public class ShowExpressionTypeAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData<Editor>(CommonDataKeys.EDITOR)!!
        val psiFile = e.getData<PsiFile>(CommonDataKeys.PSI_FILE)!!

        val type = if (editor.getSelectionModel().hasSelection()) {
            val startOffset = editor.getSelectionModel().getSelectionStart()
            val endOffset = editor.getSelectionModel().getSelectionEnd()
            val expression = CodeInsightUtilCore.findElementInRange<JetExpression>(psiFile, startOffset, endOffset, javaClass<JetExpression>(), JetLanguage.INSTANCE) ?: return
            typeByExpression(expression)
        }
        else {
            val offset = editor.getCaretModel().getOffset()
            val token = psiFile.findElementAt(offset) ?: return
            val pair = token.parents
                               .filterIsInstance<JetExpression>()
                               .map { it to typeByExpression(it) }
                               .firstOrNull { it.second != null } ?: return
            val (expression, type) = pair
            editor.getSelectionModel().setSelection(expression.startOffset, expression.endOffset)
            type
        }

        if (type != null) {
            HintManager.getInstance().showInformationHint(editor, "<html>" + DescriptorRenderer.HTML.renderType(type) + "</html>")
        }
    }

    private fun typeByExpression(expression: JetExpression): JetType? {
        val bindingContext = expression.analyze()

        if (expression is JetCallableDeclaration) {
            val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, expression] as? CallableDescriptor
            if (descriptor != null) {
                return descriptor.getReturnType()
            }
        }

        return bindingContext.getType(expression)
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData<Editor>(CommonDataKeys.EDITOR)
        val psiFile = e.getData<PsiFile>(CommonDataKeys.PSI_FILE)
        e.getPresentation().setEnabled(editor != null && psiFile is JetFile)
    }
}
