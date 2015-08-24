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

import com.intellij.execution.ExecutionManager
import com.intellij.execution.Executor
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.icons.AllIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompileStatusNotification
import com.intellij.openapi.compiler.CompilerManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.util.Consumer
import javax.swing.event.HyperlinkEvent

private fun buildAndRestartMessage(module: Module) = "Build module '${module.name}' and restart"

public class BuildAndRestartConsoleAction(
        private val project: Project,
        private val module: Module,
        private val executor: Executor,
        private val contentDescriptor: RunContentDescriptor,
        private val restarter: Consumer<Module>,
        private val testMode: Boolean
) : AnAction("Build and restart", buildAndRestartMessage(module), AllIcons.Actions.Restart) {

    init {
        if (!testMode) showOutdatedClassedNotificationIfNeeded()
    }

    override fun actionPerformed(_: AnActionEvent) = compileModule()

    private fun compileModule() {
        if (ExecutionManager.getInstance(project).contentManager.removeRunContent(executor, contentDescriptor)) {
            CompilerManager.getInstance(project).make(module, object : CompileStatusNotification {
                override fun finished(aborted: Boolean, errors: Int, warnings: Int, compileContext: CompileContext): Unit
                        = if (!module.isDisposed) restarter.consume(module)
            })
        }
    }

    private fun showOutdatedClassedNotificationIfNeeded() {
        val compilerManager = CompilerManager.getInstance(project)
        val compilerScope = compilerManager.createModuleCompileScope(module, true)
        if (compilerManager.isUpToDate(compilerScope)) return

        val warningTag = "KOTLIN REPL WARNING"
        val warningTitle = "Kotlin REPL Configuration Warning"

        val hyperlinkListener = object : NotificationListener {
            override fun hyperlinkUpdate(notification: Notification, event: HyperlinkEvent) {
                if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                    compileModule()
                    notification.expire()
                }
            }
        }

        Notifications.Bus.notify(
                Notification(
                        warningTag, warningTitle,
                        "You’re running the REPL with outdated classes<br><a href=\"Build and restart\">${buildAndRestartMessage(module)}</a>",
                        NotificationType.WARNING,
                        hyperlinkListener
                ),
                project
        )
    }
}