/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines

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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.text.DateFormatUtil
import com.intellij.xdebugger.impl.XDebuggerManagerImpl
import org.jetbrains.kotlin.idea.debugger.evaluate.ExecutionContext

@Suppress("ComponentNotRegistered")
class CoroutineDumpAction : AnAction(), AnAction.TransparentUpdate {
    private val logger = Logger.getInstance(this::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val context = DebuggerManagerEx.getInstanceEx(project).context
        val session = context.debuggerSession
        if (session != null && session.isAttached) {
            val process = context.debugProcess ?: return
            process.managerThread.schedule(object : SuspendContextCommandImpl(context.suspendContext) {
                override fun contextAction() {
                    val evalContext = context.createEvaluationContext()
                    val frameProxy = evalContext?.frameProxy ?: return
                    val execContext = ExecutionContext(evalContext, frameProxy)
                    val states = CoroutinesDebugProbesProxy.dumpCoroutines(execContext)
                    if (states.isLeft) {
                        logger.warn(states.left)
                        XDebuggerManagerImpl.NOTIFICATION_GROUP
                            .createNotification(
                                "Coroutine dump failed. See log",
                                MessageType.WARNING
                            ).notify(project)
                        return
                    }
                    val f = fun() {
                        addCoroutineDump(
                            project,
                            states.get(),
                            session.xDebugSession?.ui ?: return,
                            session.searchScope
                        )
                    }
                    ApplicationManager.getApplication().invokeLater(f, ModalityState.NON_MODAL)
                }
            })
        }
    }

    /**
     * Analog of [DebuggerUtilsEx.addThreadDump].
     */
    fun addCoroutineDump(project: Project, coroutines: List<CoroutineState>, ui: RunnerLayoutUi, searchScope: GlobalSearchScope) {
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
            description = "Coroutine Dump"
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
        presentation.isEnabled = debuggerSession != null && debuggerSession.isAttached && Registry.`is`("kotlin.debugger" + ".coroutines")
        presentation.isVisible = presentation.isEnabled
    }
}