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

package org.jetbrains.kotlin.console

import com.intellij.execution.Executor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.console.ConsoleExecuteAction
import com.intellij.execution.console.LanguageConsoleBuilder
import com.intellij.execution.console.LanguageConsoleView
import com.intellij.execution.console.ProcessBackedConsoleExecuteActionHandler
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.runners.AbstractConsoleRunnerWithHistory
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.console.actions.BuildAndRestartConsoleAction
import org.jetbrains.kotlin.console.actions.KtExecuteCommandAction
import org.jetbrains.kotlin.console.gutter.ConsoleGutterContentProvider
import org.jetbrains.kotlin.console.gutter.ConsoleIndicatorRenderer
import org.jetbrains.kotlin.console.gutter.IconWithTooltip
import org.jetbrains.kotlin.console.gutter.ReplIcons
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.project.KOTLIN_CONSOLE_KEY
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.moduleInfo
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyScriptDescriptor
import org.jetbrains.kotlin.resolve.repl.ReplState
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptDefinitionProvider
import java.awt.Color
import java.awt.Font
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

private val KOTLIN_SHELL_EXECUTE_ACTION_ID = "KotlinShellExecute"

class KotlinConsoleRunner(
        val module: Module,
        private val cmdLine: GeneralCommandLine,
        internal val previousCompilationFailed: Boolean,
        myProject: Project,
        title: String,
        path: String?
) : AbstractConsoleRunnerWithHistory<LanguageConsoleView>(myProject, title, path) {

    private val replState = ReplState()
    private val consoleTerminated = CountDownLatch(1)

    override fun finishConsole() {
        KotlinConsoleKeeper.getInstance(project).removeConsole(consoleView.virtualFile)
        KotlinScriptDefinitionProvider.getInstance(project)!!.removeScriptDefinition(consoleScriptDefinition)

        if (ApplicationManager.getApplication().isUnitTestMode) {
            consoleTerminated.countDown()
            // Ignore super with myConsoleView.setEditable(false)
            return
        }

        super.finishConsole()
    }

    val commandHistory = CommandHistory()

    var isReadLineMode: Boolean = false
        set(value) {
            if (value)
                changeConsoleEditorIndicator(ReplIcons.EDITOR_READLINE_INDICATOR)
            else
                changeConsoleEditorIndicator(ReplIcons.EDITOR_INDICATOR)

            field = value
        }

    fun changeConsoleEditorIndicator(newIconWithTooltip: IconWithTooltip) = WriteCommandAction.runWriteCommandAction(project) {
        consoleEditorHighlighter.gutterIconRenderer = ConsoleIndicatorRenderer(newIconWithTooltip)
    }

    private var consoleEditorHighlighter by Delegates.notNull<RangeHighlighter>()
    private var disposableDescriptor by Delegates.notNull<RunContentDescriptor>()

    val executor = CommandExecutor(this)
    var compilerHelper: ConsoleCompilerHelper by Delegates.notNull()

    private val consoleScriptDefinition = object : KotlinScriptDefinition(Any::class) {
        override val name = "Kotlin REPL"
        override fun isScript(fileName: String): Boolean {
            return fileName == consoleView.virtualFile.name
        }
        override fun getScriptName(script: KtScript) = Name.identifier("REPL")
    }

    override fun createProcess() = cmdLine.createProcess()

    override fun createConsoleView(): LanguageConsoleView? {
        val builder = LanguageConsoleBuilder()

        val consoleView = builder.gutterContentProvider(ConsoleGutterContentProvider()).build(project, KotlinLanguage.INSTANCE)
        consoleView.virtualFile.putUserData(KOTLIN_CONSOLE_KEY, true)


        consoleView.prompt = null

        val consoleEditor = consoleView.consoleEditor

        setupPlaceholder(consoleEditor)
        val historyKeyListener = HistoryKeyListener(module.project, consoleEditor, commandHistory)
        consoleEditor.contentComponent.addKeyListener(historyKeyListener)
        commandHistory.listeners.add(historyKeyListener)

        val executeAction = KtExecuteCommandAction(consoleView.virtualFile)
        executeAction.registerCustomShortcutSet(CommonShortcuts.CTRL_ENTER, consoleView.consoleEditor.component)

        KotlinScriptDefinitionProvider.getInstance(project)!!.addScriptDefinition(consoleScriptDefinition)
        enableCompletion(consoleView)

        return consoleView
    }

    private fun enableCompletion(consoleView: LanguageConsoleView) {
        val consoleKtFile = PsiManager.getInstance(project).findFile(consoleView.virtualFile) as? KtFile ?: return
        configureFileDependencies(consoleKtFile)
    }

    override fun createProcessHandler(process: Process): OSProcessHandler {
        val processHandler = ReplOutputHandler(
                this,
                process,
                cmdLine.commandLineString
        )
        val consoleFile = consoleView.virtualFile
        val keeper = KotlinConsoleKeeper.getInstance(project)

        keeper.putVirtualFileToConsole(consoleFile, this)

        return processHandler
    }

    override fun createExecuteActionHandler() = object : ProcessBackedConsoleExecuteActionHandler(processHandler, false) {
        override fun runExecuteAction(consoleView: LanguageConsoleView) = executor.executeCommand()
    }

    override fun fillToolBarActions(toolbarActions: DefaultActionGroup,
                                    defaultExecutor: Executor,
                                    contentDescriptor: RunContentDescriptor
    ): List<AnAction> {
        disposableDescriptor = contentDescriptor
        compilerHelper = ConsoleCompilerHelper(project, module, defaultExecutor, contentDescriptor)

        val actionList = arrayListOf<AnAction>(
                BuildAndRestartConsoleAction(this),
                createConsoleExecAction(consoleExecuteActionHandler),
                createCloseAction(defaultExecutor, contentDescriptor)
        )
        toolbarActions.addAll(actionList)
        return actionList
    }

    override fun createConsoleExecAction(consoleExecuteActionHandler: ProcessBackedConsoleExecuteActionHandler)
            = ConsoleExecuteAction(consoleView, consoleExecuteActionHandler, KOTLIN_SHELL_EXECUTE_ACTION_ID, consoleExecuteActionHandler)

    override fun constructConsoleTitle(title: String) = "$title (in module ${module.name})"

    private fun setupPlaceholder(editor: EditorEx) {
        val executeCommandAction = ActionManager.getInstance().getAction(KOTLIN_SHELL_EXECUTE_ACTION_ID)
        val executeCommandActionShortcutText = KeymapUtil.getFirstKeyboardShortcutText(executeCommandAction)

        editor.setPlaceholder("<$executeCommandActionShortcutText> to execute")
        editor.setShowPlaceholderWhenFocused(true)

        val placeholderAttrs = TextAttributes()
        placeholderAttrs.foregroundColor = ReplColors.PLACEHOLDER_COLOR
        placeholderAttrs.fontType = Font.ITALIC
        editor.setPlaceholderAttributes(placeholderAttrs)
    }

    fun setupGutters() {
        fun configureEditorGutter(editor: EditorEx, color: Color, iconWithTooltip: IconWithTooltip): RangeHighlighter {
            editor.settings.isLineMarkerAreaShown = true // hack to show gutter
            editor.settings.isFoldingOutlineShown = true
            editor.gutterComponentEx.setPaintBackground(true)
            val editorColorScheme = editor.colorsScheme
            editorColorScheme.setColor(EditorColors.GUTTER_BACKGROUND, color)
            editor.colorsScheme = editorColorScheme

            return addGutterIndicator(editor, iconWithTooltip)
        }

        val historyEditor = consoleView.historyViewer
        val consoleEditor = consoleView.consoleEditor

        configureEditorGutter(historyEditor, ReplColors.HISTORY_GUTTER_COLOR, ReplIcons.HISTORY_INDICATOR)
        consoleEditorHighlighter = configureEditorGutter(consoleEditor, ReplColors.EDITOR_GUTTER_COLOR, ReplIcons.EDITOR_INDICATOR)

        historyEditor.settings.isUseSoftWraps = true
        historyEditor.settings.additionalLinesCount = 0

        consoleEditor.settings.isCaretRowShown = true
        consoleEditor.settings.additionalLinesCount = 2
    }

    fun addGutterIndicator(editor: EditorEx, iconWithTooltip: IconWithTooltip): RangeHighlighter {
        val indicator = ConsoleIndicatorRenderer(iconWithTooltip)
        val editorMarkup = editor.markupModel
        val indicatorHighlighter = editorMarkup.addRangeHighlighter(
                0, editor.document.textLength, HighlighterLayer.LAST, null, HighlighterTargetArea.LINES_IN_RANGE
        )

        return indicatorHighlighter.apply { gutterIconRenderer = indicator }
    }

    @TestOnly fun dispose() {
        processHandler.destroyProcess()
        consoleTerminated.await(1, TimeUnit.SECONDS)
        Disposer.dispose(disposableDescriptor)
    }

    fun successfulLine(text: String) {
        runReadAction {
            val lineNumber = replState.successfulLinesCount + 1
            val virtualFile =
                    LightVirtualFile("line$lineNumber${KotlinParserDefinition.STD_SCRIPT_EXT}", KotlinLanguage.INSTANCE, text).apply {
                        charset = CharsetToolkit.UTF8_CHARSET
                        isWritable = false
                    }
            val psiFile = (PsiFileFactory.getInstance(project) as PsiFileFactoryImpl).trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile?
                          ?: error("Failed to setup PSI for file:\n$text")

            replState.submitLine(psiFile)
            configureFileDependencies(psiFile)
            val scriptDescriptor = psiFile.script!!.unsafeResolveToDescriptor() as? LazyScriptDescriptor ?: error("Failed to analyze line:\n$text")
            ForceResolveUtil.forceResolveAllContents(scriptDescriptor)
            replState.lineSuccess(psiFile, scriptDescriptor)

            replState.submitLine(consoleFile) // reset file scope customizer
        }
    }

    val consoleFile: KtFile
        get() {
            val consoleFile = consoleView.virtualFile
            return PsiManager.getInstance(project).findFile(consoleFile) as KtFile
        }

    private fun configureFileDependencies(psiFile: KtFile) {
        psiFile.moduleInfo = module.testSourceInfo() ?: module.productionSourceInfo() ?: NotUnderContentRootModuleInfo
    }
}