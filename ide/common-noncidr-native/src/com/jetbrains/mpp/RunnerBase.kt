/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import com.intellij.execution.ExecutionException
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
import com.jetbrains.mpp.runconfig.BinaryRunConfiguration

abstract class RunnerBase : DefaultProgramRunner() {
    protected abstract fun getWorkspace(project: Project): WorkspaceBase

    protected fun configureDebugSessionUI(session: XDebugSession) {
        val consoleView = session.consoleView
        if (consoleView is ConsoleViewWrapperBase) {
            session.ui?.let {
                (consoleView as ExecutionConsoleEx).buildUi(it)
            }
        }
    }

    protected fun contentDescriptor(
        environment: ExecutionEnvironment,
        muteBreakpoints: Boolean = false,
        starter: (session: XDebugSession) -> XDebugProcess
    ): RunContentDescriptor? {
        val session =
            XDebuggerManager.getInstance(environment.project).startSession(environment, object : XDebugProcessConfiguratorStarter() {
                override fun configure(session: XDebugSessionData) {
                    if (muteBreakpoints) {
                        session.isBreakpointsMuted = true
                    }
                }

                @Throws(ExecutionException::class)
                override fun start(session: XDebugSession) = starter(session)
            })

        configureDebugSessionUI(session)
        return session.runContentDescriptor
    }


    protected fun canRunBinary(executorId: String, profile: BinaryRunConfiguration): Boolean {
        val workspace = getWorkspace(profile.project)

        return when (executorId) {
            DefaultRunExecutor.EXECUTOR_ID -> true
            DefaultDebugExecutor.EXECUTOR_ID -> workspace.isDebugPossible && profile.selectedTarget?.isDebug ?: false
            else -> false
        }
    }
}