/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine

import com.intellij.debugger.DebuggerInvocationUtil
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.JavaDebugProcess
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.impl.PrioritizedTask
import com.intellij.execution.configurations.DebuggingRunnerData
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.execution.ui.layout.PlaceInGrid
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.content.Content
import com.intellij.util.messages.MessageBusConnection
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerManagerListener
import com.intellij.xdebugger.impl.XDebugSessionImpl
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.ManagerThreadExecutor
import org.jetbrains.kotlin.idea.debugger.coroutine.util.CreateContentParamsProvider
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger
import org.jetbrains.kotlin.idea.debugger.coroutine.view.XCoroutineView

class DebuggerConnection(
    val project: Project,
    val configuration: RunConfigurationBase<*>,
    val params: JavaParameters?,
    val runnerSettings: DebuggingRunnerData?
) : XDebuggerManagerListener {
    var disposable: Disposable? = null
    var connection: MessageBusConnection? = null
    private val log by logger

    init {
        if (params is JavaParameters) {
            // gradle related logic in KotlinGradleCoroutineDebugProjectResolver
            val kotlinxCoroutinesClassPathLib =
                params.classPath?.pathList?.firstOrNull { it.contains("kotlinx-coroutines-debug") }
            if (kotlinxCoroutinesClassPathLib is String)
                initializeCoroutineAgent(params, kotlinxCoroutinesClassPathLib)
            else
                log.warn("'kotlinx-coroutines-debug' not found in classpath.")
        }
        connect()
    }

    private fun initializeCoroutineAgent(params: JavaParameters, it: String?) {
        params.vmParametersList?.add("-javaagent:$it")
        params.vmParametersList?.add("-ea")
    }

    private fun connect() {
        connection = project.messageBus.connect()
        connection?.subscribe(XDebuggerManager.TOPIC, this)
    }

    override fun processStarted(debugProcess: XDebugProcess) =
        DebuggerInvocationUtil.swingInvokeLater(project) {
            if (debugProcess is JavaDebugProcess) {
                disposable = registerXCoroutinesPanel(debugProcess.session)
            }
        }

    override fun processStopped(debugProcess: XDebugProcess) {
        val rootDisposable = disposable
        if (rootDisposable is Disposable && debugProcess is JavaDebugProcess && debugProcess.session.suspendContext is SuspendContextImpl) {
            ManagerThreadExecutor(debugProcess).on(debugProcess.session.suspendContext).schedule {
                Disposer.dispose(rootDisposable)
                disposable = null
            }
        }
        connection?.disconnect()
        connection = null
    }

    private fun registerXCoroutinesPanel(session: XDebugSession): Disposable? {
        val ui = session.ui ?: return null
        val xCoroutineThreadView = XCoroutineView(project, session as XDebugSessionImpl)
        val framesContent: Content = createContent(ui, xCoroutineThreadView)
        framesContent.isCloseable = false
        ui.addContent(framesContent, 0, PlaceInGrid.right, false)
        session.addSessionListener(xCoroutineThreadView.debugSessionListener(session))
        session.rebuildViews()
        return xCoroutineThreadView
    }

    private fun createContent(ui: RunnerLayoutUi, createContentParamProvider: CreateContentParamsProvider): Content {
        val param = createContentParamProvider.createContentParams()
        return ui.createContent(param.id, param.component, param.displayName, param.icon, param.parentComponent)
    }
}

fun standaloneCoroutineDebuggerEnabled() = Registry.`is`("kotlin.debugger.coroutines.standalone")

fun coroutineDebuggerTraceEnabled() = Registry.`is`("kotlin.debugger.coroutines.trace")
