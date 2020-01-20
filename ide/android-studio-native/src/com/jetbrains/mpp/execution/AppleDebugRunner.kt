/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.execution

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.DebuggingRunnerData
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.console.ConsoleViewWrapperBase
import com.intellij.execution.runners.DefaultProgramRunner
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ExecutionConsoleEx
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.impl.XDebugProcessConfiguratorStarter
import com.intellij.xdebugger.impl.ui.XDebugSessionData
import com.jetbrains.cidr.execution.CidrCommandLineState
import com.jetbrains.mpp.AppleRunConfiguration

class AppleDebugRunner : DefaultProgramRunner() {

    override fun getRunnerId(): String = DebuggingRunnerData.DEBUGGER_RUNNER_ID

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        if (executorId == runnerId && profile is AppleRunConfiguration) return true
        return false
    }

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        if (environment.executor.id != runnerId) {
            return super.doExecute(state, environment)
        }

        if (state !is CidrCommandLineState) {
            throw ExecutionException("Unsupported RunProfileState: " + state.javaClass)
        }

        val session =
            XDebuggerManager.getInstance(environment.project).startSession(environment, object : XDebugProcessConfiguratorStarter() {
                override fun configure(session: XDebugSessionData?) {}

                @Throws(ExecutionException::class)
                override fun start(session: XDebugSession): XDebugProcess {
                    return state.startDebugProcess(session)
                }
            })

        return session.runContentDescriptor
    }
}