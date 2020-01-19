/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan

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
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.impl.XDebugProcessConfiguratorStarter
import com.intellij.xdebugger.impl.ui.XDebugSessionData

class IdeaKonanRunner : DefaultProgramRunner() {

    override fun getRunnerId(): String = KonanBundle.message("id.runner")

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        if (profile !is IdeaKonanRunConfiguration) return false

        val workspace = IdeaKonanWorkspace.getInstance(profile.project)

        return when (executorId) {
            DefaultRunExecutor.EXECUTOR_ID -> true
            DefaultDebugExecutor.EXECUTOR_ID -> workspace.isDebugPossible && profile.selectedTarget?.isDebug ?: false
            else -> false
        }
    }

    private fun configureDebugSessionUI(session: XDebugSession) {
        val consoleView = session.consoleView
        if (consoleView is ConsoleViewWrapperBase) {
            val runnerLayoutUi = session.ui
            if (runnerLayoutUi != null) {
                (consoleView as ExecutionConsoleEx).buildUi(runnerLayoutUi)
            }
        }
    }

    @Throws(ExecutionException::class)
    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        if (DefaultDebugExecutor.EXECUTOR_ID == environment.executor.id) {
            if (state !is KonanCommandLineState && state !is KonanExternalSystemState) {
                throw ExecutionException("Unsupported RunProfileState: " + state.javaClass)
            }

            val session =
                XDebuggerManager.getInstance(environment.project).startSession(environment, object : XDebugProcessConfiguratorStarter() {
                    override fun configure(session: XDebugSessionData?) {}

                    @Throws(ExecutionException::class)
                    override fun start(session: XDebugSession): XDebugProcess {
                        if (state is KonanCommandLineState) return state.startDebugProcess(session)
                        return (state as KonanExternalSystemState).startDebugProcess(session, environment)
                    }
                })

            configureDebugSessionUI(session)
            return session.runContentDescriptor
        }

        return super.doExecute(state, environment)
    }
}