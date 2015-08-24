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

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.console.KotlinConsoleHistoryManager
import org.jetbrains.kotlin.console.KotlinConsoleRunner
import org.jetbrains.kotlin.console.SeverityDetails
import org.jetbrains.kotlin.console.gutter.KotlinConsoleErrorRenderer
import org.jetbrains.kotlin.console.gutter.KotlinConsoleIndicatorRenderer
import org.jetbrains.kotlin.console.gutter.ReplIcons
import org.jetbrains.kotlin.diagnostics.Severity

enum class ReplOutputType {
    USER_OUTPUT,
    RESULT,
    INCOMPLETE,
    ERROR
}

public class KotlinReplOutputHighlighter(
        private val runner: KotlinConsoleRunner,
        private val historyManager: KotlinConsoleHistoryManager
) {
    private val consoleView = runner.consoleView
    private val historyEditor = consoleView.historyViewer
    private val historyDocument = historyEditor.document
    private val historyMarkup = historyEditor.markupModel

    private fun resetConsoleEditorIndicator() = runner.changeEditorIndicatorIcon(consoleView.consoleEditor, ReplIcons.EDITOR_INDICATOR)

    private fun insertText(text: String): Pair<Int, Int> {
        val oldLen = historyDocument.textLength
        val newLen = oldLen + text.length()

        historyDocument.insertString(oldLen, text)
        EditorUtil.scrollToTheEnd(historyEditor)

        return oldLen to newLen
    }

    fun printUserOutput(command: String) {
        resetConsoleEditorIndicator()
        historyManager.lastCommandType = ReplOutputType.USER_OUTPUT
        consoleView.print(command, ReplColors.USER_OUTPUT_CONTENT_TYPE)
    }

    fun printResultWithGutterIcon(result: String) = WriteCommandAction.runWriteCommandAction(runner.project) {
        resetConsoleEditorIndicator()
        historyManager.lastCommandType = ReplOutputType.RESULT

        val (startOffset, endOffset) = insertText(result)

        val highlighter = historyMarkup.addRangeHighlighter(startOffset, endOffset, HighlighterLayer.LAST, null, HighlighterTargetArea.EXACT_RANGE)
        highlighter.gutterIconRenderer = KotlinConsoleIndicatorRenderer(ReplIcons.RESULT)
    }

    fun changeIndicatorOnIncomplete() {
        historyManager.lastCommandType = ReplOutputType.INCOMPLETE
        runner.changeEditorIndicatorIcon(consoleView.consoleEditor, ReplIcons.INCOMPLETE_INDICATOR)
    }

    fun highlightCompilerErrors(compilerMessages: List<SeverityDetails>) = WriteCommandAction.runWriteCommandAction(runner.project) {
        resetConsoleEditorIndicator()
        historyManager.lastCommandType = ReplOutputType.ERROR

        val lastCommandStartOffset = historyDocument.textLength - historyManager.lastCommandLength
        val highlighterAndMessagesByLine = compilerMessages.filter {
            it.severity == Severity.ERROR || it.severity == Severity.WARNING
        }.groupBy { message ->
            val cmdStart = lastCommandStartOffset + message.range.startOffset
            historyEditor.document.getLineNumber(cmdStart)
        }.values().map { messages ->
            val highlighters = messages.map { message ->
                val cmdStart = lastCommandStartOffset + message.range.startOffset
                val cmdEnd = lastCommandStartOffset + message.range.endOffset

                val textAttributes = getAttributesForSeverity(cmdStart, cmdEnd, message.severity)
                historyMarkup.addRangeHighlighter(
                        cmdStart, cmdEnd, HighlighterLayer.LAST, textAttributes, HighlighterTargetArea.EXACT_RANGE
                )
            }
            Pair(highlighters.first(), messages)
        }

        for ((highlighter, messages) in highlighterAndMessagesByLine) {
            highlighter.gutterIconRenderer = KotlinConsoleErrorRenderer(messages)
        }
    }

    fun printRuntimeError(errorText: String) {
        resetConsoleEditorIndicator()
        historyManager.lastCommandType = ReplOutputType.ERROR
        consoleView.print("\t$errorText", ConsoleViewContentType.ERROR_OUTPUT)
    }

    private fun getAttributesForSeverity(start: Int, end: Int, severity: Severity): TextAttributes {
        val attributes = when (severity) {
            Severity.ERROR   -> getAttributesForSeverity(HighlightInfoType.ERROR, HighlightSeverity.ERROR, CodeInsightColors.ERRORS_ATTRIBUTES, start, end)
            Severity.WARNING -> getAttributesForSeverity(HighlightInfoType.WARNING, HighlightSeverity.WARNING, CodeInsightColors.WARNINGS_ATTRIBUTES, start, end)
            Severity.INFO    -> getAttributesForSeverity(HighlightInfoType.WEAK_WARNING, HighlightSeverity.WEAK_WARNING, CodeInsightColors.WEAK_WARNING_ATTRIBUTES, start, end)
        }
        return attributes
    }

    private fun getAttributesForSeverity(
            infoType: HighlightInfoType,
            severity: HighlightSeverity,
            insightColors: TextAttributesKey,
            start: Int,
            end: Int
    ): TextAttributes {
        val highlightInfo = HighlightInfo.newHighlightInfo(infoType).range(start, end).severity(severity).textAttributes(insightColors).create()

        val psiFile = PsiDocumentManager.getInstance(runner.project).getPsiFile(runner.consoleView.consoleEditor.document)
        val colorScheme = runner.consoleView.consoleEditor.colorsScheme

        return highlightInfo?.getTextAttributes(psiFile, colorScheme) ?: TextAttributes()
    }
}