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

import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.ide.scratch.ScratchFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.scratch.ScratchExpression
import org.jetbrains.kotlin.idea.scratch.ScratchFile
import org.jetbrains.kotlin.psi.KtPsiFactory

/**
 * Method to retrieve shared instance of scratches ToolWindow output handler.
 *
 * [releaseToolWindowHandler] must be called for every output handler received from this method.
 *
 * Can be called from EDT only.
 *
 * @return new toolWindow output handler if one does not exist, otherwise returns the existing one. When application in test mode,
 * returns [TestOutputHandler].
 */
fun requestToolWindowHandler(): ScratchOutputHandler {
    return if (ApplicationManager.getApplication().isUnitTestMode) {
        TestOutputHandler
    } else {
        ScratchToolWindowHandlerKeeper.requestOutputHandler()
    }
}

/**
 * Should be called once with the output handler received from the [requestToolWindowHandler] call.
 *
 * When release is called for every request, the output handler is actually disposed.
 *
 * When application in test mode, does nothing.
 *
 * Can be called from EDT only.
 */
fun releaseToolWindowHandler(scratchOutputHandler: ScratchOutputHandler) {
    if (!ApplicationManager.getApplication().isUnitTestMode) {
        ScratchToolWindowHandlerKeeper.releaseOutputHandler(scratchOutputHandler)
    }
}

/**
 * Implements logic of shared pointer for the toolWindow output handler.
 *
 * Not thread safe! Can be used only from the EDT.
 */
private object ScratchToolWindowHandlerKeeper {
    private var toolWindowHandler: ScratchOutputHandler? = null
    private var toolWindowDisposable = Disposer.newDisposable()
    private var counter = 0

    fun requestOutputHandler(): ScratchOutputHandler {
        if (counter == 0) {
            toolWindowHandler = ToolWindowScratchOutputHandler(toolWindowDisposable)
        }

        counter += 1
        return toolWindowHandler!!
    }

    fun releaseOutputHandler(scratchOutputHandler: ScratchOutputHandler) {
        require(counter > 0) { "Counter is $counter, nothing to release!" }
        require(toolWindowHandler === scratchOutputHandler) { "$scratchOutputHandler differs from stored $toolWindowHandler" }

        counter -= 1
        if (counter == 0) {
            Disposer.dispose(toolWindowDisposable)
            toolWindowDisposable = Disposer.newDisposable()
            toolWindowHandler = null
        }
    }
}

private class ToolWindowScratchOutputHandler(private val parentDisposable: Disposable) : ScratchOutputHandlerAdapter() {

    override fun handle(file: ScratchFile, expression: ScratchExpression, output: ScratchOutput) {
        printToConsole(file) {
            val psiFile = file.getPsiFile()
            if (psiFile != null) {
                printHyperlink(
                    getLineInfo(psiFile, expression),
                    OpenFileHyperlinkInfo(
                        project,
                        psiFile.virtualFile,
                        expression.lineStart
                    )
                )
                print(" ", ConsoleViewContentType.NORMAL_OUTPUT)
            }
            print(output.text, output.type.convert())
        }
    }

    override fun error(file: ScratchFile, message: String) {
        printToConsole(file) {
            print(message, ConsoleViewContentType.ERROR_OUTPUT)
        }
    }

    private fun printToConsole(file: ScratchFile, print: ConsoleViewImpl.() -> Unit) {
        ApplicationManager.getApplication().invokeLater {
            val project = file.project.takeIf { !it.isDisposed } ?: return@invokeLater

            val toolWindow = getToolWindow(project) ?: createToolWindow(file)

            val contents = toolWindow.contentManager.contents
            for (content in contents) {
                val component = content.component
                if (component is ConsoleViewImpl) {
                    component.print()
                    component.print("\n", ConsoleViewContentType.NORMAL_OUTPUT)
                }
            }

            toolWindow.setAvailable(true, null)

            if (!file.options.isInteractiveMode) {
                toolWindow.show(null)
            }

            toolWindow.setIcon(ExecutionUtil.getLiveIndicator(ScratchFileType.INSTANCE.icon))
        }
    }

    override fun clear(file: ScratchFile) {
        ApplicationManager.getApplication().invokeLater {
            val toolWindow = getToolWindow(file.project) ?: return@invokeLater
            val contents = toolWindow.contentManager.contents
            for (content in contents) {
                val component = content.component
                if (component is ConsoleViewImpl) {
                    component.clear()
                }
            }

            if (!file.options.isInteractiveMode) {
                toolWindow.hide(null)
            }

            toolWindow.setIcon(ScratchFileType.INSTANCE.icon ?: error("Text icon is expected to be present"))
        }
    }

    private fun ScratchOutputType.convert() = when (this) {
        ScratchOutputType.OUTPUT -> ConsoleViewContentType.SYSTEM_OUTPUT
        ScratchOutputType.RESULT -> ConsoleViewContentType.NORMAL_OUTPUT
        ScratchOutputType.ERROR -> ConsoleViewContentType.ERROR_OUTPUT
    }

    private fun getToolWindow(project: Project): ToolWindow? {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        return toolWindowManager.getToolWindow(ScratchToolWindowFactory.ID)
    }

    private fun createToolWindow(file: ScratchFile): ToolWindow {
        val project = file.project
        val toolWindowManager = ToolWindowManager.getInstance(project)
        toolWindowManager.registerToolWindow(ScratchToolWindowFactory.ID, true, ToolWindowAnchor.BOTTOM)
        val window =
            toolWindowManager.getToolWindow(ScratchToolWindowFactory.ID) ?: error("ScratchToolWindowFactory.ID should be registered")
        ScratchToolWindowFactory().createToolWindowContent(project, window)

        Disposer.register(parentDisposable, Disposable {
            toolWindowManager.unregisterToolWindow(ScratchToolWindowFactory.ID)
        })

        return window
    }
}

private fun getLineInfo(psiFile: PsiFile, expression: ScratchExpression) =
    "${psiFile.name}:${expression.lineStart + 1}"

private class ScratchToolWindowFactory : ToolWindowFactory {
    companion object {
        const val ID = "Scratch Output"
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val consoleView = ConsoleViewImpl(project, true)
        toolWindow.setToHideOnEmptyContent(true)
        toolWindow.setIcon(ScratchFileType.INSTANCE.icon ?: error("Text icon should be present"))
        toolWindow.hide(null)

        val contentManager = toolWindow.contentManager
        val content = contentManager.factory.createContent(consoleView.component, null, false)
        contentManager.addContent(content)
        val editor = consoleView.editor
        if (editor is EditorEx) {
            editor.isRendererMode = true
        }

        Disposer.register(project, consoleView)
    }
}

private object TestOutputHandler : ScratchOutputHandlerAdapter() {
    private val errors = arrayListOf<String>()
    private val inlays = arrayListOf<Pair<ScratchExpression, String>>()

    override fun handle(file: ScratchFile, expression: ScratchExpression, output: ScratchOutput) {
        inlays.add(expression to output.text)
    }

    override fun error(file: ScratchFile, message: String) {
        errors.add(message)
    }

    override fun onFinish(file: ScratchFile) {
        TransactionGuard.submitTransaction(file.project, Runnable {
            val psiFile = file.getPsiFile()
                ?: error(
                    "PsiFile cannot be found for scratch to render inlays in tests:\n" +
                            "project.isDisposed = ${file.project.isDisposed}\n" +
                            "inlays = ${inlays.joinToString { it.second }}\n" +
                            "errors = ${errors.joinToString()}"
                )

            if (inlays.isNotEmpty()) {
                testPrint(psiFile, inlays.map { (expression, text) ->
                    "/** ${getLineInfo(psiFile, expression)} $text */"
                })
                inlays.clear()
            }

            if (errors.isNotEmpty()) {
                testPrint(psiFile, listOf(errors.joinToString(prefix = "/** ", postfix = " */")))
                errors.clear()
            }
        })
    }

    private fun testPrint(file: PsiFile, comments: List<String>) {
        WriteCommandAction.runWriteCommandAction(file.project) {
            for (comment in comments) {
                file.addAfter(
                    KtPsiFactory(file.project).createComment(comment),
                    file.lastChild
                )
            }
        }
    }
}
