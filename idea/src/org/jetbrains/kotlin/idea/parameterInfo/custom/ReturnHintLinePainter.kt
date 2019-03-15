/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo.custom

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.EditorLinePainter
import com.intellij.openapi.editor.LineExtensionInfo
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.KotlinLanguage

class ReturnHintLinePainter : EditorLinePainter() {
    companion object {
        val SPACE_LINE_EXTENSION_INFO = LineExtensionInfo(" ", TextAttributes())
    }

    override fun getLineExtensions(project: Project, file: VirtualFile, lineNumber: Int): List<LineExtensionInfo>? {
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return null
        if (psiFile.language != KotlinLanguage.INSTANCE) {
            return null
        }

        val hint = getLineHint(project, file, lineNumber)
            ?: return null

        val textAttributes =
            EditorColorsManager.getInstance().globalScheme.getAttributes(DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT)
        val hintLineInfo = LineExtensionInfo(hint, textAttributes)

        return listOf(SPACE_LINE_EXTENSION_INFO, hintLineInfo)
    }

    private fun getLineHint(project: Project, file: VirtualFile, lineNumber: Int): String? {
        val doc = FileDocumentManager.getInstance().getDocument(file) ?: return null
        if (lineNumber >= doc.lineCount) {
            return null
        }
        val lineEndOffset = doc.getLineEndOffset(lineNumber)

        return KotlinCodeHintsModel.getInstance(project).getExtensionInfo(doc, lineEndOffset)
    }
}