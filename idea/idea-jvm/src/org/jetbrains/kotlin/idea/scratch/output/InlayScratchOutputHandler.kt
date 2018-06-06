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

import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.scratch.ScratchExpression
import org.jetbrains.kotlin.idea.scratch.ScratchFile
import kotlin.math.max

object InlayScratchOutputHandler : ScratchOutputHandler {
    private const val maxLineLength = 120
    private const val minInlayLength = 10
    private const val maxInsertOffset = 60
    private const val minSpaceCount = 4

    override fun onStart(file: ScratchFile) {
        clear(file)
    }

    override fun handle(file: ScratchFile, expression: ScratchExpression, output: ScratchOutput) {
        if (output.text.isBlank()) return

        createInlay(file, expression, output)

        if (output.type == ScratchOutputType.ERROR) {
            ToolWindowScratchOutputHandler.handle(file, expression, output)
        }
    }

    override fun error(file: ScratchFile, message: String) {
        ToolWindowScratchOutputHandler.error(file, message)
    }

    override fun onFinish(file: ScratchFile) {

    }

    override fun clear(file: ScratchFile) {
        clearInlays(file.editor)
        ToolWindowScratchOutputHandler.clear(file)
    }

    private fun createInlay(file: ScratchFile, expression: ScratchExpression, output: ScratchOutput) {
        UIUtil.invokeLaterIfNeeded {
            val editor = file.editor.editor
            val line = expression.lineStart

            val lineStartOffset = editor.document.getLineStartOffset(line)
            val lineEndOffset = editor.document.getLineEndOffset(line)
            val lineLength = lineEndOffset - lineStartOffset
            var spaceCount = maxLineLength(file) - lineLength + minSpaceCount

            while (spaceCount + lineLength > maxInsertOffset && spaceCount > minSpaceCount) spaceCount--

            fun addInlay(text: String) {
                val textBeforeNewLine = if (StringUtil.containsLineBreak(text)) text.substringBefore("\n") + "..." else text
                val maxInlayLength = max(maxLineLength - spaceCount - lineLength, minInlayLength)
                val shortText = StringUtil.shortenTextWithEllipsis(textBeforeNewLine, maxInlayLength, 0)
                if (shortText != text) {
                    printToToolWindow(file, expression, output)
                }
                editor.inlayModel.addInlineElement(lineEndOffset, InlayScratchFileRenderer(" ".repeat(spaceCount) + shortText, output.type))
            }

            val existing = editor.inlayModel
                .getInlineElementsInRange(lineEndOffset, lineEndOffset)
                .singleOrNull { it.renderer is InlayScratchFileRenderer }
            if (existing != null) {
                existing.dispose()
                addInlay(((existing.renderer as InlayScratchFileRenderer).text + "; " + output.text).drop(spaceCount))
            } else {
                addInlay(output.text)
            }
        }
    }

    private fun printToToolWindow(file: ScratchFile, expression: ScratchExpression, output: ScratchOutput) {
        if (output.type != ScratchOutputType.ERROR) {
            ToolWindowScratchOutputHandler.handle(file, expression, output)
        }
    }

    private fun maxLineLength(file: ScratchFile): Int {
        val doc = file.editor.editor.document
        return file.getExpressions()
            .flatMap { it.lineStart..it.lineEnd }
            .map { doc.getLineEndOffset(it) - doc.getLineStartOffset(it) }
            .max() ?: 0
    }

    private fun clearInlays(editor: TextEditor) {
        UIUtil.invokeLaterIfNeeded {
            editor
                .editor.inlayModel.getInlineElementsInRange(0, editor.editor.document.textLength)
                .filter { it.renderer is InlayScratchFileRenderer }
                .forEach { Disposer.dispose(it) }
        }
    }
}