package com.jetbrains.mobile.execution

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.runner.history.ImportedTestRunnableState
import com.intellij.execution.ui.RunContentDescriptor
import com.jetbrains.cidr.execution.CidrCommandLineState
import com.jetbrains.cidr.execution.CidrRunner

class MobileRunner : CidrRunner() {
    override fun getRunnerId(): String = "MobileRunner"

    override fun canRun(executorId: String, profile: RunProfile): Boolean =
        super.canRun(executorId, profile) &&
                (executorId == DefaultRunExecutor.EXECUTOR_ID || executorId == DefaultDebugExecutor.EXECUTOR_ID)

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        if (state is ImportedTestRunnableState) {
            return super.doExecute(state, environment)
        }

        val devices = (environment.runProfile as MobileRunConfigurationBase).executionTargets
        val isDebug = environment.executor.id == DefaultDebugExecutor.EXECUTOR_ID

        val device = devices.single()
        if (device is AppleDevice && (isDebug || device is ApplePhysicalDevice)) {
            return startDebugSession(state as CidrCommandLineState, environment, !isDebug).runContentDescriptor
        }
        return super.doExecute(state, environment)
    }
}