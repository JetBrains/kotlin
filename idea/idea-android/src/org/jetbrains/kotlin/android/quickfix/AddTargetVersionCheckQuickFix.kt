/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android.quickfix

import com.intellij.codeInsight.FileModificationService
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.android.inspections.lint.AndroidLintQuickFix
import org.jetbrains.android.inspections.lint.AndroidQuickfixContexts
import org.jetbrains.kotlin.idea.codeInsight.surroundWith.statement.KotlinIfSurrounder
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.psi.*


class AddTargetVersionCheckQuickFix(val api: Int) : AndroidLintQuickFix {

    companion object {
        private val IF_SURROUNDER = KotlinIfSurrounder()
    }

    override fun apply(startElement: PsiElement, endElement: PsiElement, context: AndroidQuickfixContexts.Context) {
        val targetExpression = getTargetExpression(startElement)
        val project = targetExpression?.project ?: return
        val editor = targetExpression.findExistingEditor() ?: return

        val file = targetExpression.containingFile
        val documentManager = PsiDocumentManager.getInstance(project)
        val document = documentManager.getDocument(file) ?: return

        if (!FileModificationService.getInstance().prepareFileForWrite(file)) {
            return
        }

        val conditionRange = IF_SURROUNDER.surroundElements(project, editor, arrayOf(targetExpression)) ?: return
        val conditionText = "android.os.Build.VERSION.SDK_INT >= ${getVersionField(api, true)}"
        document.replaceString(conditionRange.startOffset, conditionRange.endOffset, conditionText)
        documentManager.commitDocument(document)

        ShortenReferences.DEFAULT.process(documentManager.getPsiFile(document) as KtFile,
                                          conditionRange.startOffset,
                                          conditionRange.startOffset + conditionText.length)
    }

    override fun isApplicable(startElement: PsiElement, endElement: PsiElement, contextType: AndroidQuickfixContexts.ContextType): Boolean =
        getTargetExpression(startElement) != null

    override fun getName(): String = "Surround with if (VERSION.SDK_INT >= VERSION_CODES.${getVersionField(api, false)}) { ... }"

    // TODO: Delegated property, initializer, annotation parameter
    private fun getTargetExpression(element: PsiElement): PsiElement? {
        var current = PsiTreeUtil.getParentOfType(element, KtExpression::class.java)
        while (current != null) {
            if (current.parent is KtBlockExpression ||
                current.parent is KtContainerNode ||
                current.parent is KtWhenEntry ||
                current.parent is KtFunction) {
                break
            }
            current = PsiTreeUtil.getParentOfType(current, KtExpression::class.java, true)
        }

        return current
    }
}