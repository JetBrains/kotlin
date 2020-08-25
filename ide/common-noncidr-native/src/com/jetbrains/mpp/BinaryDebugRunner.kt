/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.console.ConsoleViewWrapperBase
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.DefaultProgramRunner
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ExecutionConsoleEx
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.impl.XDebugProcessConfiguratorStarter
import com.intellij.xdebugger.impl.ui.XDebugSessionData
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriver
import com.jetbrains.mpp.debugger.KonanExternalSystemState
import com.jetbrains.mpp.runconfig.BinaryRunConfiguration
import com.jetbrains.mpp.workspace.WorkspaceBase
import java.nio.file.Path
import java.nio.file.Paths

private fun initPrinters(driver: LLDBDriver, project: Project, printersPath: Path) {
    // Apply custom formatting for Kotlin/Native structs:
    driver.executeConsoleCommand("command script import \"$printersPath\"")

    // Re-draw debugger views that may be drawn by concurrent threads while formatting hasn't been applied:
    XDebuggerManager.getInstance(project).currentSession?.rebuildViews()

    driver.executeConsoleCommand("settings set target.process.thread.step-avoid-regexp ^::Kotlin_")
}

fun CidrDebugProcess.loadPythonBindings(pythonBindingsPath: Path) = postCommand { driver ->
    if (driver !is LLDBDriver) return@postCommand
    initPrinters(driver, project, pythonBindingsPath)
}

abstract class BinaryDebugRunner : DefaultProgramRunner() {
    protected abstract fun getWorkspace(project: Project): WorkspaceBase

    protected open fun getPythonBindingsPath(project: Project): Path? {
        val konanHome = getWorkspace(project).konanHome ?: return null
        return Paths.get(konanHome, "tools", "konan_lldb.py")
    }

    override fun canRun(executorId: String, profile: RunProfile): Boolean =
        if (profile is BinaryRunConfiguration) {
            when (executorId) {
                DefaultRunExecutor.EXECUTOR_ID -> true
                DefaultDebugExecutor.EXECUTOR_ID ->
                    getWorkspace(profile.project).isDebugPossible &&
                            profile.variant is BinaryExecutable.Variant.Debug
                else -> false
            }
        } else false

    @Throws(ExecutionException::class)
    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        val isDebug = environment.executor.id == DefaultDebugExecutor.EXECUTOR_ID

        return if (isDebug) when (state) {
            is KonanCommandLineState -> showDebugContent(environment) { state.startDebugProcess(it) }
            is KonanExternalSystemState -> showDebugContent(environment) { state.startDebugProcess(it, environment) }
            else -> throw ExecutionException("${state.javaClass} is not supported by ${this.javaClass}")
        } else super.doExecute(state, environment)
    }

    protected fun showDebugContent(
        environment: ExecutionEnvironment,
        muteBreakpoints: Boolean = false,
        starter: (session: XDebugSession) -> XDebugProcess
    ): RunContentDescriptor? {
        val debuggerManager = XDebuggerManager.getInstance(environment.project)
        val bindingsPath = getPythonBindingsPath(environment.project)

        val debugStarter = object : XDebugProcessConfiguratorStarter() {
            override fun configure(session: XDebugSessionData) {
                if (muteBreakpoints) session.isBreakpointsMuted = true
            }

            override fun start(session: XDebugSession): XDebugProcess {
                val process = starter(session)

                if (process is CidrDebugProcess && bindingsPath != null) {
                    process.loadPythonBindings(bindingsPath)
                }

                return process
            }
        }

        val session = debuggerManager.startSession(environment, debugStarter).also {
            configureDebugSessionUI(it)
        }
        return session.runContentDescriptor
    }

    private fun configureDebugSessionUI(session: XDebugSession) {
        val consoleView = session.consoleView
        if (consoleView is ConsoleViewWrapperBase) {
            session.ui?.let {
                (consoleView as ExecutionConsoleEx).buildUi(it)
            }
        }
    }
}