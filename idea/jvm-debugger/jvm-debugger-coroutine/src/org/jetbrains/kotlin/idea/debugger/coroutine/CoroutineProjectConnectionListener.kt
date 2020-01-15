/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine

import com.intellij.debugger.DebuggerInvocationUtil
import com.intellij.debugger.engine.JavaDebugProcess
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.execution.ui.layout.PlaceInGrid
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.content.Content
import com.intellij.util.messages.MessageBusConnection
import com.intellij.xdebugger.*
import com.intellij.xdebugger.impl.XDebugSessionImpl
import org.jetbrains.kotlin.idea.debugger.coroutine.util.*
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
            if (debugProcess is JavaDebugProcess) {
                registerXCoroutinesPanel(debugProcess.session)
            }
        }

    override fun processStopped(debugProcess: XDebugProcess) {
        if (processCounter.compareAndSet(1, 0)) {
            connection?.disconnect()
            connection = null
        } else
            processCounter.decrementAndGet()
    }

    private fun registerXCoroutinesPanel(session: XDebugSession) {
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
