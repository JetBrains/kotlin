/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.console.actions

import com.intellij.icons.AllIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.KotlinIdeaReplBundle
import org.jetbrains.kotlin.console.KotlinConsoleKeeper
import org.jetbrains.kotlin.console.KotlinConsoleRunner

fun errorNotification(project: Project?, message: String) {
    val errorTag = "KOTLIN REPL ERROR"
    val errorTitle = KotlinIdeaReplBundle.message("kotlin.repl.configuration.error")
    Notifications.Bus.notify(Notification(errorTag, errorTitle, message, NotificationType.ERROR), project)
}

fun logError(cl: Class<*>, message: String, t: Throwable? = null) = with(Logger.getInstance(cl)) { error(message, t) }

class RunKotlinConsoleAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return errorNotification(null, KotlinIdeaReplBundle.message("project.not.found"))

        ConsoleModuleDialog(project).showIfNeeded(e.dataContext)
    }
}

class KtExecuteCommandAction(private val consoleFile: VirtualFile) : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return errorNotification(null, KotlinIdeaReplBundle.message("cannot.find.project"))
        val ktConsole = KotlinConsoleKeeper.getInstance(project).getConsoleByVirtualFile(consoleFile) ?: return

        ktConsole.executor.executeCommand()
    }
}

class BuildAndRestartConsoleAction(
    private val runner: KotlinConsoleRunner
) : AnAction(
    KotlinIdeaReplBundle.message("build.and.restart"),
    KotlinIdeaReplBundle.message("build.module.0.and.restart", runner.module.name),
    AllIcons.Actions.Restart
) {

    override fun actionPerformed(e: AnActionEvent) = runner.compilerHelper.compileModule()
}