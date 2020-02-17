/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.execution

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.DefaultProgramRunner
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.impl.XDebugProcessConfiguratorStarter
import com.intellij.xdebugger.impl.ui.XDebugSessionData
import com.jetbrains.cidr.execution.CidrCommandLineState
import com.jetbrains.mpp.AppleRunConfiguration

class AppleRunner : DefaultProgramRunner() {

    override fun getRunnerId(): String = "AppleRunner"

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        if (profile !is AppleRunConfiguration) return false

        return when (executorId) {
            DefaultRunExecutor.EXECUTOR_ID -> profile.selectedDevice is AppleSimulator
            DefaultDebugExecutor.EXECUTOR_ID -> true
            else -> false
        }
    }

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        if (environment.executor.id == DefaultRunExecutor.EXECUTOR_ID) {
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