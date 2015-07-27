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

import com.intellij.execution.Executor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.console.*
import com.intellij.execution.process.*
import com.intellij.execution.runners.AbstractConsoleRunnerWithHistory
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.event.DocumentAdapter
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.console.actions.KtExecuteCommandAction
import org.jetbrains.kotlin.console.actions.logError
import org.jetbrains.kotlin.idea.JetLanguage
import java.awt.Color
import javax.swing.Icon
import kotlin.properties.Delegates

public class KotlinConsoleRunner(
        val title: String,
        private val cmdLine: GeneralCommandLine,
        myProject: Project,
        path: String?
) : AbstractConsoleRunnerWithHistory<LanguageConsoleView>(myProject, title, path) {
    companion object {
        private val HISTORY_GUTTER_ICON = AllIcons.Debugger.Console
        private val EDITOR_GUTTER_ICON = AllIcons.Debugger.CommandLine
    }

    private val keyEventListener = KtConsoleKeyListener(this)
    private var historyHighlighter: KotlinReplResultHighlighter by Delegates.notNull()
    val history: MutableList<String> = arrayListOf()

    override fun createProcess() = cmdLine.createProcess()

    override fun createConsoleView(): LanguageConsoleView? {
        val consoleView = LanguageConsoleBuilder().build(project, JetLanguage.INSTANCE)
        consoleView.prompt = null

        val historyEditor = consoleView.historyViewer
        val consoleEditor = consoleView.consoleEditor
        consoleEditor.contentComponent.addKeyListener(keyEventListener)

        historyEditor.document.addDocumentListener(object : DocumentAdapter() {
            override fun documentChanged(e: DocumentEvent): Unit =
                    if (historyEditor.document.textLength == 0) addGutterIcon(historyEditor, HISTORY_GUTTER_ICON)
        })

        historyHighlighter = KotlinReplResultHighlighter(historyEditor)
        historyEditor.document.addDocumentListener(historyHighlighter)

        consoleEditor.setPlaceholder("<Ctrl+Enter> to execute")
        consoleEditor.setShowPlaceholderWhenFocused(true)
        val placeholderAttrs = consoleEditor.foldingModel.placeholderAttributes
        placeholderAttrs.foregroundColor = Color.GRAY

        val executeAction = KtExecuteCommandAction(consoleView.virtualFile)
        executeAction.registerCustomShortcutSet(CommonShortcuts.CTRL_ENTER, consoleView.consoleEditor.component)

        return consoleView
    }

    override fun createProcessHandler(process: Process): OSProcessHandler {
        val processHandler = KotlinReplOutputHandler(process, cmdLine.commandLineString)
        historyHighlighter.rangeQueue = processHandler.rangeQueue
        val consoleFile = consoleView.virtualFile
        val keeper = KotlinConsoleKeeper.getInstance(project)

        keeper.putVirtualFileToConsole(consoleFile, this)
        processHandler.addProcessListener(object : ProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) {
                keeper.removeConsole(consoleFile)
            }
        })

        return processHandler
    }

    override fun createExecuteActionHandler() = object : ProcessBackedConsoleExecuteActionHandler(processHandler, false) {
        override fun sendText(line: String) {
            submitCommand(line)
        }
    }

    override fun fillToolBarActions(toolbarActions: DefaultActionGroup,
                                    defaultExecutor: Executor,
                                    contentDescriptor: RunContentDescriptor
    ): List<AnAction> {
        val actionList = arrayListOf<AnAction>(
            createCloseAction(defaultExecutor, contentDescriptor),
            createConsoleExecAction(consoleExecuteActionHandler)
        )
        toolbarActions.addAll(actionList)
        return actionList
    }

    override fun createConsoleExecAction(consoleExecuteActionHandler: ProcessBackedConsoleExecuteActionHandler)
        = ConsoleExecuteAction(consoleView, consoleExecuteActionHandler, "KotlinShellExecute", consoleExecuteActionHandler)

    fun setupGutters() {
        fun configureEditorGutter(editor: EditorEx, color: Color, icon: Icon) {
            editor.settings.isLineMarkerAreaShown = true // hack to show gutter
            editor.settings.isFoldingOutlineShown = true
            editor.gutterComponentEx.setPaintBackground(true)
            val editorColorScheme = editor.colorsScheme
            editorColorScheme.setColor(EditorColors.GUTTER_BACKGROUND, color)
            editor.colorsScheme = editorColorScheme

            addGutterIcon(editor, icon)
        }

        val consoleView = consoleView
        val lightGray = Color(0xF2, 0xF2, 0xF2)
        val lightBlue = Color(0x93, 0xDE, 0xFF)
        configureEditorGutter(consoleView.historyViewer, lightGray, HISTORY_GUTTER_ICON)
        configureEditorGutter(consoleView.consoleEditor, lightBlue, EDITOR_GUTTER_ICON)
    }

    private fun addGutterIcon(editor: EditorEx, icon: Icon) {
        val editorMarkup = editor.markupModel
        val highlighter = editorMarkup.addRangeHighlighter(0, editor.document.textLength, HighlighterLayer.LAST, null, HighlighterTargetArea.LINES_IN_RANGE)
        highlighter.gutterIconRenderer = object : GutterIconRenderer() {
            override fun getIcon() = icon
            override fun hashCode() = System.identityHashCode(this)
            override fun equals(other: Any?) = this === other
        }
    }

    public fun submitCommand(command: String) {
        val res = command.trim()
        if (res.isEmpty()) return

        history.add(res)
        keyEventListener.resetHistoryPosition()

        val processInputOS = processHandler.processInput ?: return logError(javaClass, "<p>Broken process stream</p>")
        val charset = (processHandler as? BaseOSProcessHandler)?.charset ?: Charsets.UTF_8
        val bytes = ("$res\n").toByteArray(charset)
        processInputOS.write(bytes)
        processInputOS.flush()
    }
}