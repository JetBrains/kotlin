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

package org.jetbrains.kotlin.console

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.execution.console.LanguageConsoleImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.console.actions.logError
import org.jetbrains.kotlin.console.gutter.IconWithTooltip
import org.jetbrains.kotlin.console.gutter.ConsoleErrorRenderer
import org.jetbrains.kotlin.console.gutter.ConsoleIndicatorRenderer
import org.jetbrains.kotlin.console.gutter.ReplIcons
import org.jetbrains.kotlin.diagnostics.Severity

class ReplOutputProcessor(
        private val runner: KotlinConsoleRunner
) {
    private val project = runner.project
    private val consoleView = runner.consoleView as LanguageConsoleImpl
    private val historyEditor = consoleView.historyViewer
    private val historyDocument = historyEditor.document
    private val historyMarkup = historyEditor.markupModel

    private fun textOffsets(text: String): Pair<Int, Int> {
        consoleView.flushDeferredText() // flush before getting offsets to calculate them properly
        val oldLen = historyDocument.textLength
        val newLen = oldLen + text.length

        return Pair(oldLen, newLen)
    }

    private fun printOutput(output: String, contentType: ConsoleViewContentType, iconWithTooltip: IconWithTooltip? = null) {
        val (startOffset, endOffset) = textOffsets(output)

        consoleView.print(output, contentType)
        consoleView.flushDeferredText()

        if (iconWithTooltip == null) return

        historyMarkup.addRangeHighlighter(
                startOffset, endOffset, HighlighterLayer.LAST, null, HighlighterTargetArea.EXACT_RANGE
        ).apply { gutterIconRenderer = ConsoleIndicatorRenderer(iconWithTooltip) }
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
        if (ApplicationManager.getApplication().isUnitTestMode) return
        if (runner.previousCompilationFailed) return printWarningMessage("There were compilation errors in module ${runner.module.name}", false)
        if (runner.compilerHelper.moduleIsUpToDate()) return

        val compilerWarningMessage = "You’re running the REPL with outdated classes: "
        printWarningMessage(compilerWarningMessage, true)
    }

    fun printInitialPrompt(command: String) = consoleView.print(command, ReplColors.INITIAL_PROMPT_CONTENT_TYPE)

    fun printHelp(help: String) = WriteCommandAction.runWriteCommandAction(project) {
        printOutput(help, ConsoleViewContentType.SYSTEM_OUTPUT, ReplIcons.SYSTEM_HELP)
    }

    fun printUserOutput(command: String) = WriteCommandAction.runWriteCommandAction(project) {
        consoleView.print(command, ReplColors.USER_OUTPUT_CONTENT_TYPE)
    }

    fun printResultWithGutterIcon(result: String) = WriteCommandAction.runWriteCommandAction(project) {
        printOutput(result, ConsoleViewContentType.NORMAL_OUTPUT, ReplIcons.RESULT)
    }

    fun highlightCompilerErrors(compilerMessages: List<SeverityDetails>) = WriteCommandAction.runWriteCommandAction(project) {
        val commandHistory = runner.commandHistory
        val lastUnprocessedHistoryEntry = commandHistory.lastUnprocessedEntry() ?: return@runWriteCommandAction logError(
                ReplOutputProcessor::class.java,
                "Processed more commands than were sent. Sent commands: ${commandHistory.size}. Processed: ${commandHistory.processedEntriesCount}"
        )
        val lastCommandStartOffset = lastUnprocessedHistoryEntry.rangeInHistoryDocument.startOffset
        val lastCommandStartLine = historyDocument.getLineNumber(lastCommandStartOffset)
        val historyCommandRunIndicator = historyMarkup.allHighlighters.first {
            historyDocument.getLineNumber(it.startOffset) == lastCommandStartLine && it.gutterIconRenderer != null
        }

        val highlighterAndMessagesByLine = compilerMessages.filter {
            it.severity == Severity.ERROR || it.severity == Severity.WARNING
        }.groupBy { message ->
            val cmdStart = lastCommandStartOffset + message.range.startOffset
            historyEditor.document.getLineNumber(cmdStart)
        }.values.map { messages ->
            val highlighters = messages.map { message ->
                val cmdStart = lastCommandStartOffset + message.range.startOffset
                val cmdEnd = lastCommandStartOffset + Math.max(message.range.endOffset, message.range.startOffset + 1)

                val textAttributes = getAttributesForSeverity(cmdStart, cmdEnd, message.severity)
                historyMarkup.addRangeHighlighter(
                        cmdStart, cmdEnd, HighlighterLayer.LAST, textAttributes, HighlighterTargetArea.EXACT_RANGE
                )
            }
            Pair(highlighters.first(), messages)
        }

        for ((highlighter, messages) in highlighterAndMessagesByLine) {
            if (historyDocument.getLineNumber(highlighter.startOffset) == lastCommandStartLine)
                historyCommandRunIndicator.gutterIconRenderer = ConsoleErrorRenderer(messages)
            else
                highlighter.gutterIconRenderer = ConsoleErrorRenderer(messages)
        }
    }

    fun printRuntimeError(errorText: String) = WriteCommandAction.runWriteCommandAction(project) {
        printOutput(errorText, ConsoleViewContentType.ERROR_OUTPUT, ReplIcons.RUNTIME_EXCEPTION)
    }

    fun printInternalErrorMessage(internalErrorText: String) = WriteCommandAction.runWriteCommandAction(project) {
        val promptText = "Internal error occurred. Please, send report to developers.\n"
        printOutput(promptText, ConsoleViewContentType.ERROR_OUTPUT, ReplIcons.RUNTIME_EXCEPTION)
        logError(this::class.java, internalErrorText)
    }

    private fun getAttributesForSeverity(start: Int, end: Int, severity: Severity): TextAttributes = when (severity) {
        Severity.ERROR   ->
            getAttributesForSeverity(HighlightInfoType.ERROR, HighlightSeverity.ERROR, CodeInsightColors.ERRORS_ATTRIBUTES, start, end)
        Severity.WARNING ->
            getAttributesForSeverity(HighlightInfoType.WARNING, HighlightSeverity.WARNING, CodeInsightColors.WARNINGS_ATTRIBUTES, start, end)
        Severity.INFO    ->
            getAttributesForSeverity(HighlightInfoType.WEAK_WARNING, HighlightSeverity.WEAK_WARNING, CodeInsightColors.WEAK_WARNING_ATTRIBUTES, start, end)
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