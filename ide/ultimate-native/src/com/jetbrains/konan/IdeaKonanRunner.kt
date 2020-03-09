/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.project.Project

class IdeaKonanRunner : RunnerBase() {

    override fun getRunnerId(): String = KonanBundle.message("id.runner")

    override fun getWorkspace(project: Project): WorkspaceBase {
        return IdeaKonanWorkspace.getInstance(project)
    }

    override fun canRun(executorId: String, profile: RunProfile) = when (profile) {
        is IdeaKonanRunConfiguration -> canRunBinary(executorId, profile)
        else -> false
    }

    @Throws(ExecutionException::class)
    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        if (environment.executor.id != DefaultDebugExecutor.EXECUTOR_ID) {
            return super.doExecute(state, environment)
        }

        return when (state) {
            is KonanCommandLineState -> contentDescriptor(environment) { session -> state.startDebugProcess(session) }
            is KonanExternalSystemState -> contentDescriptor(environment) { session -> state.startDebugProcess(session, environment) }
            else -> throw ExecutionException("RunProfileState  ${state.javaClass} is not supported by ${this.javaClass}")
        }
    }

}