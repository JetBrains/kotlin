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
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.ide.scratch.ScratchFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.scratch.ScratchExpression
import org.jetbrains.kotlin.idea.scratch.ScratchFile
import org.jetbrains.kotlin.psi.KtPsiFactory

object ToolWindowScratchOutputHandler : ScratchOutputHandlerAdapter() {

    override fun handle(file: ScratchFile, expression: ScratchExpression, output: ScratchOutput) {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            return testPrint(file, output.text, expression)
        }

        printToConsole(file.project) {
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
        if (ApplicationManager.getApplication().isUnitTestMode) {
            return testPrint(file, message)
        }

        printToConsole(file.project) {
            print(message, ConsoleViewContentType.ERROR_OUTPUT)
        }
    }

    private fun printToConsole(project: Project, print: ConsoleViewImpl.() -> Unit) {
        ApplicationManager.getApplication().invokeLater {
            val toolWindow = getToolWindow(project) ?: createToolWindow(project)
            val contents = toolWindow.contentManager.contents
            for (content in contents) {
                val component = content.component
                if (component is ConsoleViewImpl) {
                    component.print()
                    component.print("\n", ConsoleViewContentType.NORMAL_OUTPUT)
                }
            }
            toolWindow.setAvailable(true, null)
            toolWindow.show(null)
        }
    }

    override fun clear(file: ScratchFile) {
        if (ApplicationManager.getApplication().isUnitTestMode) return

        ApplicationManager.getApplication().invokeLater {
            val toolWindow = getToolWindow(file.project) ?: return@invokeLater
            val contents = toolWindow.contentManager.contents
            for (content in contents) {
                val component = content.component
                if (component is ConsoleViewImpl) {
                    component.clear()
                }
            }
            toolWindow.setAvailable(false, null)
            toolWindow.hide(null)
        }
    }

    private fun getLineInfo(psiFile: PsiFile, expression: ScratchExpression) =
        "${psiFile.name}:${expression.lineStart + 1}"

    private fun ScratchOutputType.convert() = when (this) {
        ScratchOutputType.OUTPUT -> ConsoleViewContentType.SYSTEM_OUTPUT
        ScratchOutputType.RESULT -> ConsoleViewContentType.NORMAL_OUTPUT
        ScratchOutputType.ERROR -> ConsoleViewContentType.ERROR_OUTPUT
    }

    private fun getToolWindow(project: Project): ToolWindow? {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        return toolWindowManager.getToolWindow(ScratchToolWindowFactory.ID)
    }

    private fun createToolWindow(project: Project): ToolWindow {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        toolWindowManager.registerToolWindow(ScratchToolWindowFactory.ID, true, ToolWindowAnchor.BOTTOM)
        val window = toolWindowManager.getToolWindow(ScratchToolWindowFactory.ID)
        ScratchToolWindowFactory().createToolWindowContent(project, window)
        return window
    }

    @TestOnly
    private fun testPrint(file: ScratchFile, output: String, expression: ScratchExpression? = null) {
        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(file.project) {
                val psiFile = file.getPsiFile()!!
                psiFile.addAfter(
                    KtPsiFactory(file.project).createComment(
                        "/** ${expression?.let { getLineInfo(psiFile, expression) + " " } ?: ""}$output */"
                    ),
                    psiFile.lastChild
                )
            }
        }
    }
}

private class ScratchToolWindowFactory : ToolWindowFactory {
    companion object {
        const val ID = "Scratch Output"
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val consoleView = ConsoleViewImpl(project, true)
        toolWindow.isToHideOnEmptyContent = true
        toolWindow.icon = ScratchFileType.INSTANCE.icon

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
