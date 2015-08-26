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
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.Consumer
import org.jetbrains.kotlin.console.actions.BuildAndRestartConsoleAction
import org.jetbrains.kotlin.console.actions.KtExecuteCommandAction
import org.jetbrains.kotlin.console.gutter.KotlinConsoleGutterContentProvider
import org.jetbrains.kotlin.console.gutter.KotlinConsoleIndicatorRenderer
import org.jetbrains.kotlin.console.gutter.ReplIcons
import org.jetbrains.kotlin.console.highlight.KotlinReplOutputHighlighter
import org.jetbrains.kotlin.console.highlight.ReplColors
import org.jetbrains.kotlin.idea.JetLanguage
import java.awt.Color
import java.awt.Font
import java.util.concurrent.ConcurrentHashMap
import javax.swing.Icon
import kotlin.properties.Delegates

public class KotlinConsoleRunner(
        private val cmdLine: GeneralCommandLine,
        private val module: Module,
        private val testMode: Boolean,
        myProject: Project,
        title: String,
        path: String?
) : AbstractConsoleRunnerWithHistory<LanguageConsoleView>(myProject, title, path) {
    private val editorToIndicator = ConcurrentHashMap<EditorEx, RangeHighlighter>()
    private val historyManager = KotlinConsoleHistoryManager(this)
    val executor = KotlinConsoleExecutor(this, historyManager)

    private var disposableDescriptor: RunContentDescriptor by Delegates.notNull()

    override fun createProcess() = cmdLine.createProcess()

    override fun createConsoleView(): LanguageConsoleView? {
        val consoleView = LanguageConsoleBuilder()
                .gutterContentProvider(KotlinConsoleGutterContentProvider())
                .build(project, JetLanguage.INSTANCE)

        consoleView.prompt = null

        val consoleEditor = consoleView.consoleEditor

        setupPlaceholder(consoleEditor)
        consoleEditor.contentComponent.addKeyListener(historyManager)

        val executeAction = KtExecuteCommandAction(consoleView.virtualFile)
        executeAction.registerCustomShortcutSet(CommonShortcuts.CTRL_ENTER, consoleView.consoleEditor.component)

        return consoleView
    }

    override fun createProcessHandler(process: Process): OSProcessHandler {
        val processHandler = KotlinReplOutputHandler(KotlinReplOutputHighlighter(this, historyManager), process, cmdLine.commandLineString)
        val consoleFile = consoleView.virtualFile
        val keeper = KotlinConsoleKeeper.getInstance(project)

        keeper.putVirtualFileToConsole(consoleFile, this)
        processHandler.addProcessListener(object : ProcessAdapter() {
            override fun processTerminated(_: ProcessEvent) {
                keeper.removeConsole(consoleFile)
            }
        })

        return processHandler
    }

    override fun createExecuteActionHandler() = object : ProcessBackedConsoleExecuteActionHandler(processHandler, false) {
        override fun runExecuteAction(_: LanguageConsoleView) = executor.executeCommand()
    }

    override fun fillToolBarActions(toolbarActions: DefaultActionGroup,
                                    defaultExecutor: Executor,
                                    contentDescriptor: RunContentDescriptor
    ): List<AnAction> {
        disposableDescriptor = contentDescriptor

        val actionList = arrayListOf<AnAction>(
                BuildAndRestartConsoleAction(project, module, defaultExecutor, contentDescriptor, restarter, testMode),
                createConsoleExecAction(consoleExecuteActionHandler),
                createCloseAction(defaultExecutor, contentDescriptor)
        )
        toolbarActions.addAll(actionList)
        return actionList
    }

    override fun createConsoleExecAction(consoleExecuteActionHandler: ProcessBackedConsoleExecuteActionHandler)
            = ConsoleExecuteAction(consoleView, consoleExecuteActionHandler, "KotlinShellExecute", consoleExecuteActionHandler)

    private val restarter = object : Consumer<Module> {
        override fun consume(module: Module) {
            KotlinConsoleKeeper.getInstance(project).run(module)
        }
    }

    override fun constructConsoleTitle(title: String) = "$title (in module ${module.name})"

    private fun setupPlaceholder(editor: EditorEx) {
        editor.setPlaceholder("<Ctrl+Enter> to execute")
        editor.setShowPlaceholderWhenFocused(true)

        val placeholderAttrs = TextAttributes()
        placeholderAttrs.foregroundColor = ReplColors.PLACEHOLDER_COLOR
        placeholderAttrs.fontType = Font.ITALIC
        editor.setPlaceholderAttributes(placeholderAttrs)
    }

    fun setupGutters() {
        fun configureEditorGutter(editor: EditorEx, color: Color, icon: Icon) {
            editor.settings.isLineMarkerAreaShown = true // hack to show gutter
            editor.settings.isFoldingOutlineShown = true
            editor.gutterComponentEx.setPaintBackground(true)
            val editorColorScheme = editor.colorsScheme
            editorColorScheme.setColor(EditorColors.GUTTER_BACKGROUND, color)
            editor.colorsScheme = editorColorScheme

            addGutterIndicator(editor, icon)
        }

        val historyEditor = consoleView.historyViewer
        val consoleEditor = consoleView.consoleEditor

        configureEditorGutter(historyEditor, ReplColors.HISTORY_GUTTER_COLOR, ReplIcons.HISTORY_INDICATOR)
        configureEditorGutter(consoleEditor, ReplColors.EDITOR_GUTTER_COLOR, ReplIcons.EDITOR_INDICATOR)

        historyEditor.settings.isUseSoftWraps = true

        consoleEditor.settings.isCaretRowShown = true
        consoleEditor.settings.additionalLinesCount = 2
    }

    fun addGutterIndicator(editor: EditorEx, icon: Icon) {
        val indicator = KotlinConsoleIndicatorRenderer(icon)
        val editorMarkup = editor.markupModel
        val indicatorHighlighter = editorMarkup.addRangeHighlighter(
                0, editor.document.textLength, HighlighterLayer.LAST, null, HighlighterTargetArea.LINES_IN_RANGE
        )

        indicatorHighlighter.gutterIconRenderer = indicator
        editorToIndicator[editor] = indicatorHighlighter
    }

    fun changeEditorIndicatorIcon(editor: EditorEx, newIcon: Icon) {
        val oldHighlighter = editorToIndicator[editor] ?: return
        WriteCommandAction.runWriteCommandAction(project) { oldHighlighter.gutterIconRenderer = KotlinConsoleIndicatorRenderer(newIcon) }
    }

    // this method shouldn't be called in normal usage; it is for test purpose only
    fun dispose() {
        processHandler.destroyProcess()
        Disposer.dispose(disposableDescriptor)
    }
}