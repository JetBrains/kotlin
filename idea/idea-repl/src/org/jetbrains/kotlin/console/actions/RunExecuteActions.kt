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

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.console.KotlinConsoleKeeper

fun errorNotification(project: Project?, message: String) {
    val tag = "KOTLIN REPL ERROR"
    val title = "Kotlin REPL Configuration Error"
    Notifications.Bus.notify(Notification(tag, title, message, NotificationType.ERROR), project)
}

fun logError(cl: Class<*>, message: String) {
    val logger = Logger.getInstance(cl)
    logger.error(message)
}

public class RunKotlinConsoleAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return errorNotification(null, "<p>Project not found</p>")

        KotlinConsoleModuleDialog(project).showIfNeeded(e.dataContext)
    }
}

public class KtExecuteCommandAction(private val consoleFile: VirtualFile) : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return errorNotification(null, "<p>Cannot find project</p>")
        val ktConsole = KotlinConsoleKeeper.getInstance(project).getConsoleByVirtualFile(consoleFile) ?: return errorNotification(project, "<p>Action performed in an invalid console</p>")

        ktConsole.executor.executeCommand()
    }
}