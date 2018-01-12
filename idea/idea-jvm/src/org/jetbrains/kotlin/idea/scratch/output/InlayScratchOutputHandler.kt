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

package org.jetbrains.kotlin.idea.scratch.output

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.impl.ComplementaryFontsRegistry
import com.intellij.openapi.editor.impl.FontInfo
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.scratch.ScratchExpression
import org.jetbrains.kotlin.idea.scratch.ScratchFile
import org.jetbrains.kotlin.idea.scratch.ui.ScratchToolWindow
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Rectangle

object InlayScratchOutputHandler : ScratchOutputHandler {
    override fun onStart(file: ScratchFile) {
        clear(file)
    }

    override fun handle(file: ScratchFile, expression: ScratchExpression, output: ScratchOutput) {
        val document = PsiDocumentManager.getInstance(file.psiFile.project).getDocument(file.psiFile) ?: return
        val lineEndOffset = document.getLineEndOffset(expression.lineStart)
        createInlay(file.psiFile.project, file.psiFile.virtualFile, lineEndOffset, output.text.substringBefore("\n"), output.type)

        if (output.type == ScratchOutputType.ERROR) {
            error(file, output.text)
        }
    }

    override fun error(file: ScratchFile, message: String) {
        ScratchToolWindow.addMessageToToolWindow(file.psiFile.project, message, ConsoleViewContentType.ERROR_OUTPUT)
    }

    override fun onFinish(file: ScratchFile) {

    }

    override fun clear(file: ScratchFile) {
        clearInlays(file.psiFile.project, file.psiFile.virtualFile)
        ScratchToolWindow.clearToolWindow(file.psiFile.project)
    }

    private fun createInlay(project: Project, file: VirtualFile, offset: Int, inlayText: String, outputType: ScratchOutputType) {
        UIUtil.invokeLaterIfNeeded {
            val textEditor = FileEditorManager.getInstance(project).getSelectedEditor(file) as? TextEditor ?: return@invokeLaterIfNeeded
            val editor = textEditor.editor

            val text = editor.document.immutableCharSequence
            var insertOffset = offset
            while (insertOffset < text.length && Character.isJavaIdentifierPart(text[insertOffset])) insertOffset++

            val existing = editor.inlayModel
                .getInlineElementsInRange(insertOffset, insertOffset)
                .singleOrNull { it.renderer is ScratchFileRenderer }
            if (existing != null) {
                existing.dispose()
                editor.inlayModel.addInlineElement(
                    insertOffset,
                    ScratchFileRenderer((existing.renderer as ScratchFileRenderer).text + "; " + inlayText, outputType)
                )
            } else {
                editor.inlayModel.addInlineElement(insertOffset, ScratchFileRenderer("    $inlayText", outputType))
            }
        }
    }

    private fun clearInlays(project: Project, file: VirtualFile) {
        UIUtil.invokeLaterIfNeeded {
            val editors = FileEditorManager.getInstance(project).getEditors(file)
            editors.filterIsInstance<TextEditor>()
                .flatMap { it.editor.inlayModel.getInlineElementsInRange(0, it.editor.document.textLength) }
                .filter { it.renderer is ScratchFileRenderer }
                .forEach { Disposer.dispose(it) }
        }
    }

    class ScratchFileRenderer(val text: String, val outputType: ScratchOutputType) : EditorCustomElementRenderer {
        private fun getFontInfo(editor: Editor): FontInfo {
            val colorsScheme = editor.colorsScheme
            val fontPreferences = colorsScheme.fontPreferences
            val attributes = getAttributes()
            val fontStyle = attributes.fontType
            return ComplementaryFontsRegistry.getFontAbleToDisplay(
                'a'.toInt(), fontStyle, fontPreferences, FontInfo.getFontRenderContext(editor.contentComponent)
            )
        }

        override fun calcWidthInPixels(editor: Editor): Int {
            val fontInfo = getFontInfo(editor)
            return fontInfo.fontMetrics().stringWidth(text)
        }

        override fun paint(editor: Editor, g: Graphics, r: Rectangle, textAttributes: TextAttributes) {
            val attributes = getAttributes()
            val fgColor = attributes.foregroundColor ?: return
            g.color = fgColor
            val fontInfo = getFontInfo(editor)
            g.font = fontInfo.font
            val metrics = fontInfo.fontMetrics()
            g.drawString(text, r.x, r.y + metrics.ascent)
        }

        private fun getAttributes(): TextAttributes {
            return when (outputType) {
                ScratchOutputType.OUTPUT -> userOutputAttributes
                ScratchOutputType.RESULT -> normalAttributes
                ScratchOutputType.ERROR -> errorAttributes
            }
        }

        override fun toString(): String {
            return "${outputType.name}: ${text.trim()}"
        }

        companion object {
            private val normalAttributes = TextAttributes(
                JBColor(Gray._135, Color(0x3d8065)),
                null, null, null,
                Font.ITALIC
            )

            private val errorAttributes = TextAttributes(
                JBColor(Color.RED, Color.RED),
                null, null, null,
                Font.ITALIC
            )

            private val userOutputAttributes = TextAttributes(
                JBColor(Color.GREEN, Color.GREEN),
                null, null, null,
                Font.ITALIC
            )
        }
    }
}