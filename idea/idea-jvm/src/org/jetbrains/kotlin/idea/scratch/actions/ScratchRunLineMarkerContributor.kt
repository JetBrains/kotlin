/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch.actions

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.core.util.getLineCount
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.idea.scratch.ScratchExpression
import org.jetbrains.kotlin.idea.scratch.getEditorWithScratchPanel
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class ScratchRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        val ktFile = element.containingFile as? KtFile
        if (ktFile?.isScript() != true) return null
        val file = ktFile.virtualFile
        if (ScratchFileService.getInstance().getRootType(file) !is ScratchRootType) return null

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
        val (_, panel) = getEditorWithScratchPanel(
            FileEditorManager.getInstance(element.project),
            element.containingFile.virtualFile
        ) ?: return null

        val scratchFile = panel.scratchFile
        if (!scratchFile.options.isRepl) return null
        val replExecutor = scratchFile.replScratchExecutor ?: return null
        return replExecutor.getFirstNewExpression()
    }
}
