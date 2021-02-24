/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.command

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.engine.events.SuspendContextCommandImpl
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.execution.filters.ExceptionFilters
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.execution.ui.layout.impl.RunnerContentUi
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Disposer
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.text.DateFormatUtil
import com.intellij.xdebugger.impl.XDebuggerManagerImpl
import org.jetbrains.kotlin.idea.debugger.coroutine.KotlinDebuggerCoroutinesBundle
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CoroutineInfoData
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.CoroutineDebugProbesProxy
import org.jetbrains.kotlin.idea.debugger.coroutine.view.CoroutineDumpPanel

@Suppress("ComponentNotRegistered")
class CoroutineDumpAction : AnAction(), AnAction.TransparentUpdate {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val context = DebuggerManagerEx.getInstanceEx(project).context
        val session = context.debuggerSession
        if (session != null && session.isAttached) {
            val process = context.debugProcess ?: return
            process.managerThread.schedule(object : SuspendContextCommandImpl(context.suspendContext) {
                override fun contextAction() {
                    val states = CoroutineDebugProbesProxy(context.suspendContext ?: return)
                        .dumpCoroutines()
                    if (states.isOk()) {
                        val f = fun() {
                            val ui = session.xDebugSession?.ui ?: return
                            addCoroutineDump(project, states.cache, ui, session.searchScope)
                        }
                        ApplicationManager.getApplication().invokeLater(f, ModalityState.NON_MODAL)
                    } else {
                        val message = KotlinDebuggerCoroutinesBundle.message("coroutine.dump.failed")
                        XDebuggerManagerImpl.NOTIFICATION_GROUP.createNotification(message, MessageType.ERROR).notify(project)
                    }
                }
            })
        }
    }

    /**
     * Analog of [DebuggerUtilsEx.addThreadDump].
     */
    fun addCoroutineDump(project: Project, coroutines: List<CoroutineInfoData>, ui: RunnerLayoutUi, searchScope: GlobalSearchScope) {
        val consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project)
        consoleBuilder.filters(ExceptionFilters.getFilters(searchScope))
        val consoleView = consoleBuilder.console
        val toolbarActions = DefaultActionGroup()
        consoleView.allowHeavyFilters()
        val panel = CoroutineDumpPanel(project, consoleView, toolbarActions, coroutines)

        val id = "DumpKt " + DateFormatUtil.formatTimeWithSeconds(System.currentTimeMillis())
        val content = ui.createContent(id, panel, id, null, null).apply {
            putUserData(RunnerContentUi.LIGHTWEIGHT_CONTENT_MARKER, true)
            isCloseable = true
            description = KotlinDebuggerCoroutinesBundle.message("coroutine.dump.panel.title")
        }
        ui.addContent(content)
        ui.selectAndFocus(content, true, true)
        Disposer.register(content, consoleView)
    }

    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        val project = e.project
        if (project == null) {
            presentation.isEnabled = false
            presentation.isVisible = false
            return
        }
        // cannot be called when no SuspendContext
        if (DebuggerManagerEx.getInstanceEx(project).context.suspendContext == null) {
            presentation.isEnabled = false
            return
        }
        val debuggerSession = DebuggerManagerEx.getInstanceEx(project).context.debuggerSession
        presentation.isEnabled = debuggerSession != null && debuggerSession.isAttached
        presentation.isVisible = presentation.isEnabled
    }
}