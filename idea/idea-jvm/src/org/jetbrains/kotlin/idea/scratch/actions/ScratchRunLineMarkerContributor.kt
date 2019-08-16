/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch.actions

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.core.util.getLineCount
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.idea.scratch.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class ScratchRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        val ktFile = element.containingFile as? KtFile
        if (ktFile?.isScript() != true) return null
        val file = ktFile.virtualFile
        if (!(file.isKotlinScratch || file.isKotlinWorksheet)) return null

        val declaration = element.getStrictParentOfType<KtNamedDeclaration>()
        if (declaration != null && declaration !is KtParameter && declaration.nameIdentifier == element) {
            return isLastExecutedExpression(element)
        }

        val scriptInitializer = element.getParentOfType<KtScriptInitializer>(true)?.body
        if (scriptInitializer != null) {
            if (scriptInitializer.findDescendantOfType<LeafPsiElement>() == element) {
                return isLastExecutedExpression(element)
            }
            return null
        }

        // Show arrow for last added empty line
        if (declaration is KtScript && element is PsiWhiteSpace) {
            val expression = getLastExecutedExpression(element)
            if (expression == null) {
                if (element.getLineNumber() == element.containingFile.getLineCount()
                    || element.getLineNumber(false) == element.containingFile.getLineCount()) {
                    return Info(RunScratchFromHereAction())
                }
            }
        }
        return null
    }

    private fun isLastExecutedExpression(element: PsiElement): Info? {
        val expression = getLastExecutedExpression(element) ?: return null
        if (element.getLineNumber(true) != expression.lineStart) {
            return null
        }

        if (PsiTreeUtil.isAncestor(expression.element, element, false)) {
            return Info(RunScratchFromHereAction())
        }
        return null
    }

    private fun getLastExecutedExpression(element: PsiElement): ScratchExpression? {
        val scratchFile = getSingleOpenedTextEditor(element.containingFile)?.getScratchFile() ?: return null
        if (!scratchFile.options.isRepl) return null
        val replExecutor = scratchFile.replScratchExecutor ?: return null
        return replExecutor.getFirstNewExpression()
    }

    /**
     * This method returns single editor in which passed [psiFile] opened.
     * If there is no such editor or there is more than one editor, it returns `null`.
     *
     * We use [PsiDocumentManager.getCachedDocument] instead of [PsiDocumentManager.getDocument]
     * so this would not require read action.
     */
    private fun getSingleOpenedTextEditor(psiFile: PsiFile): TextEditor? {
        val document = PsiDocumentManager.getInstance(psiFile.project).getCachedDocument(psiFile) ?: return null
        val singleOpenedEditor = EditorFactory.getInstance().getEditors(document, psiFile.project).singleOrNull() ?: return null
        return TextEditorProvider.getInstance().getTextEditor(singleOpenedEditor)
    }
}
