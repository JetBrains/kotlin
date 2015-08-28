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
import com.intellij.execution.console.LanguageConsoleImpl
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
import javax.swing.Icon

enum class ReplOutputType {
    USER_OUTPUT,
    RESULT,
    INCOMPLETE,
    ERROR
}

public class KotlinReplOutputHighlighter(
        private val runner: KotlinConsoleRunner,
        private val historyManager: KotlinConsoleHistoryManager,
        private val testMode: Boolean,
        private val previousCompilationFailed: Boolean
) {
    private val project = runner.project
    private val consoleView = runner.consoleView as LanguageConsoleImpl
    private val historyEditor = consoleView.historyViewer
    private val historyDocument = historyEditor.document
    private val historyMarkup = historyEditor.markupModel

    private fun resetConsoleEditorIndicator() = runner.changeEditorIndicatorIcon(consoleView.consoleEditor, ReplIcons.EDITOR_INDICATOR)

    private fun textOffsets(text: String): Pair<Int, Int> {
        consoleView.flushDeferredText() // flush before getting offsets to calculate them properly
        val oldLen = historyDocument.textLength
        val newLen = oldLen + text.length()

        return Pair(oldLen, newLen)
    }

    private fun printOutput(output: String, contentType: ConsoleViewContentType, icon: Icon? = null) {
        val (startOffset, endOffset) = textOffsets(output)

        consoleView.print(output, contentType)
        consoleView.flushDeferredText()

        if (icon == null) return

        historyMarkup.addRangeHighlighter(
                startOffset, endOffset, HighlighterLayer.LAST, null, HighlighterTargetArea.EXACT_RANGE
        ) apply { gutterIconRenderer = KotlinConsoleIndicatorRenderer(icon) }
    }

    private fun printWarningMessage(message: String, isAddHyperlink: Boolean) = WriteCommandAction.runWriteCommandAction(project) {
        printOutput("\n", ConsoleViewContentType.NORMAL_OUTPUT)

        printOutput(message, ReplColors.WARNING_INFO_CONTENT_TYPE, ReplIcons.BUILD_WARNING_INDICATOR)

        if (isAddHyperlink) {
            consoleView.printHyperlink("Build module '${runner.module.name}' and restart") {
                runner.compilerHelper.compileModule()
            }
        }

        printOutput("\n\n", ConsoleViewContentType.NORMAL_OUTPUT)
    }

    fun printBuildInfoWarningIfNeeded() {
        if (testMode) return
        if (previousCompilationFailed) return printWarningMessage("There were compilation errors in module ${runner.module.name}", false)
        if (runner.compilerHelper.moduleIsUpToDate()) return

        val compilerWarningMessage = "You’re running the REPL with outdated classes: "
        printWarningMessage(compilerWarningMessage, true)
    }

    fun printInitialPrompt(command: String) = consoleView.print(command, ReplColors.INITIAL_PROMPT_CONTENT_TYPE)

    fun printHelp(help: String) = WriteCommandAction.runWriteCommandAction(project) {
        resetConsoleEditorIndicator()
        historyManager.lastCommandType = ReplOutputType.USER_OUTPUT

        printOutput(help, ConsoleViewContentType.SYSTEM_OUTPUT, ReplIcons.SYSTEM_HELP)
    }

    fun printUserOutput(command: String) = WriteCommandAction.runWriteCommandAction(project) {
        resetConsoleEditorIndicator()
        historyManager.lastCommandType = ReplOutputType.USER_OUTPUT
        consoleView.print(command, ReplColors.USER_OUTPUT_CONTENT_TYPE)
    }

    fun printResultWithGutterIcon(result: String) = WriteCommandAction.runWriteCommandAction(project) {
        resetConsoleEditorIndicator()
        historyManager.lastCommandType = ReplOutputType.RESULT

        printOutput(result, ConsoleViewContentType.NORMAL_OUTPUT, ReplIcons.RESULT)
    }

    fun changeIndicatorOnIncomplete() = WriteCommandAction.runWriteCommandAction(project) {
        historyManager.lastCommandType = ReplOutputType.INCOMPLETE
        runner.changeEditorIndicatorIcon(consoleView.consoleEditor, ReplIcons.INCOMPLETE_INDICATOR)

        // remove line breaks after incomplete part
        val historyText = historyDocument.text
        val historyLength = historyText.length()
        val trimmedHistoryText = historyText.trimEnd()
        val whitespaceCharsToDelete = historyLength - trimmedHistoryText.length()

        historyDocument.deleteString(historyLength - whitespaceCharsToDelete, historyLength)
        EditorUtil.scrollToTheEnd(historyEditor)
    }

    fun highlightCompilerErrors(compilerMessages: List<SeverityDetails>) = WriteCommandAction.runWriteCommandAction(project) {
        resetConsoleEditorIndicator()
        historyManager.lastCommandType = ReplOutputType.ERROR

        val lastCommandStartOffset = historyDocument.textLength - historyManager.lastCommandLength
        val lastCommandStartLine = historyDocument.getLineNumber(lastCommandStartOffset)
        val historyCommandRunIndicator = historyMarkup.allHighlighters filter {
            historyDocument.getLineNumber(it.startOffset) == lastCommandStartLine && it.gutterIconRenderer != null
        } first { true }

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
            if (historyDocument.getLineNumber(highlighter.startOffset) == lastCommandStartLine)
                historyCommandRunIndicator.gutterIconRenderer = KotlinConsoleErrorRenderer(messages)
            else
                highlighter.gutterIconRenderer = KotlinConsoleErrorRenderer(messages)
        }
    }

    fun printRuntimeError(errorText: String) = WriteCommandAction.runWriteCommandAction(project) {
        resetConsoleEditorIndicator()
        historyManager.lastCommandType = ReplOutputType.ERROR

        printOutput(errorText, ConsoleViewContentType.ERROR_OUTPUT, ReplIcons.RUNTIME_EXCEPTION)
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

        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(consoleView.consoleEditor.document)
        val colorScheme = consoleView.consoleEditor.colorsScheme

        return highlightInfo?.getTextAttributes(psiFile, colorScheme) ?: TextAttributes()
    }
}