/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.console

import com.intellij.execution.console.LanguageConsoleImpl
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.console.gutter.ConsoleIndicatorRenderer
import org.jetbrains.kotlin.console.gutter.ReplIcons

class HistoryUpdater(private val runner: KotlinConsoleRunner) {
    private val consoleView: LanguageConsoleImpl by lazy { runner.consoleView as LanguageConsoleImpl }

    fun printNewCommandInHistory(trimmedCommandText: String): TextRange {
        val historyEditor = consoleView.historyViewer
        addLineBreakIfNeeded(historyEditor)
        val startOffset = historyEditor.document.textLength
        val endOffset = startOffset + trimmedCommandText.length

        addCommandTextToHistoryEditor(trimmedCommandText)
        addFoldingRegion(historyEditor, startOffset, endOffset, trimmedCommandText)

        historyEditor.markupModel.addRangeHighlighter(
                startOffset, endOffset, HighlighterLayer.LAST, null, HighlighterTargetArea.EXACT_RANGE
        ).apply {
            val historyMarker = if (runner.isReadLineMode) ReplIcons.READLINE_MARKER else ReplIcons.COMMAND_MARKER
            gutterIconRenderer = ConsoleIndicatorRenderer(historyMarker)
        }

        historyEditor.scrollingModel.scrollVertically(endOffset)

        return TextRange(startOffset, endOffset)
    }

    private fun addCommandTextToHistoryEditor(trimmedCommandText: String) {
        val consoleEditor = consoleView.consoleEditor
        val consoleDocument = consoleEditor.document
        consoleDocument.setText(trimmedCommandText)
        LanguageConsoleImpl.printWithHighlighting(consoleView, consoleEditor, TextRange(0, consoleDocument.textLength))
        consoleView.flushDeferredText()
        consoleDocument.setText("")
    }

    private fun addLineBreakIfNeeded(historyEditor: EditorEx) {
        if (runner.isReadLineMode) return

        val historyDocument = historyEditor.document
        val historyText = historyDocument.text
        val textLength = historyText.length

        if (!historyText.endsWith('\n')) {
            historyDocument.insertString(textLength, "\n")

            if (textLength == 0) // this will work first time after 'Clear all' action
                runner.addGutterIndicator(historyEditor, ReplIcons.HISTORY_INDICATOR)
            else
                historyDocument.insertString(textLength + 1, "\n")

        } else if (!historyText.endsWith("\n\n")) {
            historyDocument.insertString(textLength, "\n")
        }
    }

    private fun addFoldingRegion(historyEditor: EditorEx, startOffset: Int, endOffset: Int, command: String) {
        val cmdLines = command.lines()
        val linesCount = cmdLines.size
        if (linesCount < 2) return

        val foldingModel =  historyEditor.foldingModel
        foldingModel.runBatchFoldingOperation {
            foldingModel.addFoldRegion(startOffset, endOffset, "${cmdLines[0]} ...")
        }
    }
}