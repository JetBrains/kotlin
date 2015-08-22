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

package org.jetbrains.kotlin.console.highlight

import com.intellij.execution.impl.ConsoleViewUtil
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import org.jetbrains.kotlin.console.KotlinConsoleRunner
import org.jetbrains.kotlin.console.gutter.ReplIcons

public class KotlinHistoryHighlighter(private val runner: KotlinConsoleRunner ) {
    fun addAndHighlightNewCommand(command: String) {
        val historyEditor = runner.consoleView.historyViewer

        addLineBreakIfNeeded(historyEditor)

        val historyDocument = historyEditor.document
        val oldHistoryLength = historyDocument.textLength
        historyDocument.insertString(oldHistoryLength, command)
        EditorUtil.scrollToTheEnd(historyEditor)

        val inputEditor = runner.consoleView.consoleEditor
        val lexerHighlighter = inputEditor.highlighter as? LexerEditorHighlighter ?: return
        val syntaxHighlighter = lexerHighlighter.syntaxHighlighter ?: return

        val lexer = syntaxHighlighter.highlightingLexer
        lexer.start(command, 0, command.length(), 0)

        val historyMarkupModel = historyEditor.markupModel

        while (true) {
            val tokenType = lexer.tokenType ?: break
            val tokenStartOffset = oldHistoryLength + lexer.tokenStart
            val tokenEndOffset = oldHistoryLength + lexer.tokenEnd

            val contentType = ConsoleViewUtil.getContentTypeForToken(tokenType, syntaxHighlighter)
            historyMarkupModel.addRangeHighlighter(
                    tokenStartOffset, tokenEndOffset, HighlighterLayer.LAST,
                    contentType.attributes, HighlighterTargetArea.EXACT_RANGE
            )

            lexer.advance()
        }
    }

    private fun addLineBreakIfNeeded(historyEditor: EditorEx) {
        val historyDocument = historyEditor.document
        val historyText = historyDocument.text

        if (!historyText.endsWith('\n')) {
            val textLength = historyText.length()
            historyDocument.insertString(textLength, "\n")

            if (textLength == 0) // this will work first time after 'Clear all' action
                runner.addGutterIndicator(historyEditor, ReplIcons.HISTORY_INDICATOR)
        }
    }
}