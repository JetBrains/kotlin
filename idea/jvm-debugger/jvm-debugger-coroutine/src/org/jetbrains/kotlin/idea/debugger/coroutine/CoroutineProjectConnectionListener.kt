/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine

import com.intellij.debugger.DebuggerInvocationUtil
import com.intellij.debugger.actions.ThreadDumpAction
import com.intellij.debugger.engine.JavaDebugProcess
import com.intellij.debugger.impl.DebuggerSession
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.execution.ui.layout.PlaceInGrid
import com.intellij.execution.ui.layout.impl.RunnerContentUi
import com.intellij.execution.ui.layout.impl.RunnerLayoutUiImpl
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManagerAdapter
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.util.messages.MessageBusConnection
import com.intellij.xdebugger.*
import com.intellij.xdebugger.impl.XDebugSessionImpl
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.debugger.coroutine.util.*
import org.jetbrains.kotlin.idea.debugger.coroutine.view.CoroutinesPanel
import org.jetbrains.kotlin.idea.debugger.coroutine.view.XCoroutineView
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


class CoroutineProjectConnectionListener(val project: Project) : XDebuggerManagerListener {
    var connection: MessageBusConnection? = null
    val processCounter = AtomicInteger(0)
    private val log by logger

    private fun connect() {
        connection = project.messageBus.connect()
        connection?.subscribe(XDebuggerManager.TOPIC, this)
    }

    fun configurationStarting(
        configuration: RunConfigurationBase<*>,
        params: JavaParameters?,
        runnerSettings: RunnerSettings?
    ) {
        val configurationName = configuration.type.id
        try {
            if (!gradleConfiguration(configurationName)) { // gradle test logic in KotlinGradleCoroutineDebugProjectResolver
                val kotlinxCoroutinesClassPathLib =
                    params?.classPath?.pathList?.first { it.contains("kotlinx-coroutines-debug") }
                initializeCoroutineAgent(params!!, kotlinxCoroutinesClassPathLib)
            }
            starting()
        } catch (e: NoSuchElementException) {
            log.warn("'kotlinx-coroutines-debug' not found in classpath. Coroutine debugger disabled.")
        }
    }

    private fun starting() {
        if (processCounter.compareAndSet(0, 1))
            connect()
        else
            processCounter.incrementAndGet()
    }

    private fun gradleConfiguration(configurationName: String) =
        "GradleRunConfiguration".equals(configurationName) || "KotlinGradleRunConfiguration".equals(configurationName)

    override fun processStarted(debugProcess: XDebugProcess) =
        DebuggerInvocationUtil.swingInvokeLater(project) {
            if (debugProcess is JavaDebugProcess)
                registerCoroutinesPanel(debugProcess.session, debugProcess.debuggerSession)
        }

    override fun processStopped(debugProcess: XDebugProcess) {
        if (processCounter.compareAndSet(1, 0)) {
            connection?.disconnect()
            connection = null
        } else
            processCounter.decrementAndGet()
    }

    private fun createThreadsContent(session: XDebugSession) {
        val ui = session.ui ?: return
        val xCoroutineThreadView = XCoroutineView(project, session as XDebugSessionImpl)
        val framesContent: Content = createContent(ui, xCoroutineThreadView)
        framesContent.isCloseable = false
        ui.addContent(framesContent, 0, PlaceInGrid.right, false)
        session.addSessionListener(xCoroutineThreadView.debugSessionListener(session))
        session.rebuildViews()
    }

    private fun createContent(ui: RunnerLayoutUi, createContentParamProvider: CreateContentParamsProvider): Content {
        val param = createContentParamProvider.createContentParams()
        return ui.createContent(param.id, param.component, param.displayName, param.icon, param.parentComponent)
    }

    /**
     * Adds panel to XDebugSessionTab
     */
    private fun registerCoroutinesPanel(session: XDebugSession, debuggerSession: DebuggerSession): Boolean {
        val ui = session.ui ?: return false
        val panel = CoroutinesPanel(project, debuggerSession.contextManager)
        // evaluation of `debuggerSession.contextManager.toString()` leads to
        // java.lang.Throwable: Assertion failed: Should be invoked in manager thread, use DebuggerManagerThreadImpl.getInstance(..).invoke
        // as toString() is not allowed here
        createThreadsContent(session)

        val content = ui.createContent(
            CoroutineDebuggerContentInfo.COROUTINE_THREADS_CONTENT,
            panel,
            KotlinBundle.message("debugger.session.tab.coroutine.title"),
            AllIcons.Debugger.ThreadGroup,
            panel)
        content.isCloseable = false
        ui.addContent(content, 0, PlaceInGrid.left, true)
        ui.addListener(object : ContentManagerAdapter() {
            override fun selectionChanged(event: ContentManagerEvent) {
                if (event.content === content) {
                    if (content.isSelected) {
                        panel.setUpdateEnabled(true)
                        if (panel.isRefreshNeeded) {
                            panel.rebuildIfVisible(DebuggerSession.Event.CONTEXT)
                        }
                    } else {
                        panel.setUpdateEnabled(false)
                    }
                }
            }
        }, content)
        // add coroutine dump button: due to api problem left toolbar is copied, modified and reset to tab
        val runnerContent = (ui.options as RunnerLayoutUiImpl).getData(RunnerContentUi.KEY.name) as RunnerContentUi
        val modifiedActions = runnerContent.getActions(true)
        val pos = modifiedActions.indexOfLast { it is ThreadDumpAction }
        modifiedActions.add(pos + 1, ActionManager.getInstance().getAction("Kotlin.XDebugger.CoroutinesDump"))
        ui.options.setLeftToolbar(DefaultActionGroup(modifiedActions), ActionPlaces.DEBUGGER_TOOLBAR)
        return true
    }
}

val Project.coroutineConnectionListener by projectListener

val projectListener
    get() = object : ReadOnlyProperty<Project, CoroutineProjectConnectionListener> {
        lateinit var listenerProject: CoroutineProjectConnectionListener

        override fun getValue(project: Project, property: KProperty<*>): CoroutineProjectConnectionListener {
            if (!::listenerProject.isInitialized)
                listenerProject = CoroutineProjectConnectionListener(project)
            return listenerProject
        }
    }

internal fun coroutineDebuggerEnabled() = Registry.`is`("kotlin.debugger.coroutines")

internal fun initializeCoroutineAgent(params: JavaParameters, it: String?) {
    params.vmParametersList?.add("-javaagent:$it")
    params.vmParametersList?.add("-ea")
}
