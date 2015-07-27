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

package org.jetbrains.kotlin.console.actions

import com.intellij.execution.console.LanguageConsoleImpl
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.console.KotlinConsoleKeeper

public class KtExecuteCommandAction(private val consoleFile: VirtualFile) : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return errorNotification(null, "<p>Cannot find project</p>")
        val ktConsole = KotlinConsoleKeeper.getInstance(project).getConsoleByVirtualFile(consoleFile) ?: return errorNotification(project, "<p>Action performed in not valid console</p>")

        WriteCommandAction.runWriteCommandAction(project) {
            val consoleView = ktConsole.consoleView
            val document = consoleView.editorDocument
            val command = document.text

            document.setText(command)
            LanguageConsoleImpl.printWithHighlighting(consoleView, consoleView.consoleEditor, TextRange(0, command.length()))
            document.setText("")

            ktConsole.submitCommand(command)
        }
    }
}