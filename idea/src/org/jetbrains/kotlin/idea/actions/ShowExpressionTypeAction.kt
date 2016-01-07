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
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType

@Deprecated("Remove once we no longer support IDEA 14.1") class ShowExpressionTypeAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) as? KtFile ?: return

        val type = if (editor.selectionModel.hasSelection()) {
            val startOffset = editor.selectionModel.selectionStart
            val endOffset = editor.selectionModel.selectionEnd
            val expression = CodeInsightUtilCore.findElementInRange<KtExpression>(psiFile, startOffset, endOffset, KtExpression::class.java, KotlinLanguage.INSTANCE) ?: return
            typeByExpression(expression)
        }
        else {
            val offset = editor.caretModel.offset
            val token = psiFile.findElementAt(offset) ?: return
            val pair = token.parents
                               .filterIsInstance<KtExpression>()
                               .map { it to typeByExpression(it) }
                               .firstOrNull { it.second != null } ?: return
            val (expression, type) = pair
            editor.selectionModel.setSelection(expression.startOffset, expression.endOffset)
            type
        }

        if (type != null) {
            HintManager.getInstance().showInformationHint(editor, renderTypeHint(type))
        }
    }


    override fun update(e: AnActionEvent) {
        // hide the action in IDEA 15 where a standard platform action is available
        if (ActionManager.getInstance().getAction("ExpressionTypeInfo") != null) {
            e.presentation.isVisible = false
            return
        }

        val editor = e.getData<Editor>(CommonDataKeys.EDITOR)
        val psiFile = e.getData<PsiFile>(CommonDataKeys.PSI_FILE)
        e.presentation.isEnabled = editor != null && psiFile is KtFile
    }

    companion object {
        fun renderTypeHint(type: KotlinType) = "<html>" + DescriptorRenderer.HTML.renderType(type) + "</html>"

        fun typeByExpression(expression: KtExpression): KotlinType? {
            val bindingContext = expression.analyze()

            if (expression is KtCallableDeclaration) {
                val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, expression] as? CallableDescriptor
                if (descriptor != null) {
                    return descriptor.returnType
                }
            }

            return bindingContext.getType(expression)
        }
    }
}
