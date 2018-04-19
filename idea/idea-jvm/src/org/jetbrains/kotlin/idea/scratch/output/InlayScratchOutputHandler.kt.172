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
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.util.Disposer
import com.intellij.ui.Colors
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
    private const val maxInsertOffset = 60
    private const val minSpaceCount = 4

    override fun onStart(file: ScratchFile) {
        clear(file)
    }

    override fun handle(file: ScratchFile, expression: ScratchExpression, output: ScratchOutput) {
        createInlay(file, expression.lineStart, output.text.substringBefore("\n"), output.type)

        if (output.type == ScratchOutputType.ERROR) {
            error(file, output.text)
        }
    }

    override fun error(file: ScratchFile, message: String) {
        ScratchToolWindow.addMessageToToolWindow(file.project, message, ConsoleViewContentType.ERROR_OUTPUT)
    }

    override fun onFinish(file: ScratchFile) {

    }

    override fun clear(file: ScratchFile) {
        clearInlays(file.editor)
        ScratchToolWindow.clearToolWindow(file.project)
    }

    private fun createInlay(file: ScratchFile, line: Int, inlayText: String, outputType: ScratchOutputType) {
        UIUtil.invokeLaterIfNeeded {
            val editor = file.editor.editor

            val lineStartOffset = editor.document.getLineStartOffset(line)
            val lineEndOffset = editor.document.getLineEndOffset(line)
            val lineLength = lineEndOffset - lineStartOffset
            var spaceCount = maxLineLength(file) - lineLength + minSpaceCount

            while(spaceCount + lineLength > maxInsertOffset && spaceCount > minSpaceCount) spaceCount--

            val existing = editor.inlayModel
                .getInlineElementsInRange(lineEndOffset, lineEndOffset)
                .singleOrNull { it.renderer is ScratchFileRenderer }
            if (existing != null) {
                existing.dispose()
                editor.inlayModel.addInlineElement(
                    lineEndOffset,
                    ScratchFileRenderer((existing.renderer as ScratchFileRenderer).text + "; " + inlayText, outputType)
                )
            } else {
                editor.inlayModel.addInlineElement(lineEndOffset, ScratchFileRenderer(" ".repeat(spaceCount) + inlayText, outputType))
            }
        }
    }

    private fun maxLineLength(file: ScratchFile): Int {
        val doc = file.editor.editor.document
        return (0 until doc.lineCount)
            .map { doc.getLineEndOffset(it) - doc.getLineStartOffset(it) }
            .max()
                ?: -1
    }

    private fun clearInlays(editor: TextEditor) {
        UIUtil.invokeLaterIfNeeded {
            editor
                .editor.inlayModel.getInlineElementsInRange(0, editor.editor.document.textLength)
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

        override fun paint(editor: Editor, g: Graphics, r: Rectangle) {
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
                JBColor.GRAY,
                null, null, null,
                Font.ITALIC
            )

            private val errorAttributes = TextAttributes(
                JBColor(Colors.DARK_RED, Colors.DARK_RED),
                null, null, null,
                Font.ITALIC
            )

            private val userOutputColor = Color(0x5C5CFF)
            private val userOutputAttributes = TextAttributes(
                JBColor(userOutputColor, userOutputColor),
                null, null, null,
                Font.ITALIC
            )
        }
    }
}