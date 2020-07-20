package com.jetbrains.mobile.execution

import com.intellij.execution.ExecutionTarget
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.jetbrains.cidr.execution.CidrCommandLineState
import com.jetbrains.cidr.execution.CidrConsoleBuilder
import com.jetbrains.cidr.execution.testing.CidrLauncher
import java.io.File

interface MobileRunConfiguration : RunConfiguration {
    fun getProductBundle(device: Device): File

    fun getExecutionTarget(environment: ExecutionEnvironment): ExecutionTarget =
        environment.executionTarget

    override fun getState(executor: Executor, environment: ExecutionEnvironment): CommandLineState =
        when (val device = getExecutionTarget(environment)) {
            is AppleDevice -> createAppleState(environment, executor, device)
            else -> createOtherState(environment)
        }

    fun createAppleState(environment: ExecutionEnvironment, executor: Executor, device: AppleDevice): CommandLineState {
        return CidrCommandLineState(environment, createLauncher(environment)).also {
            it.consoleBuilder = CidrConsoleBuilder(project, null, project.basePath?.let { File(it) })
        }
    }

    fun createOtherState(environment: ExecutionEnvironment): CommandLineState {
        throw IllegalStateException()
    }

    fun createLauncher(environment: ExecutionEnvironment): CidrLauncher =
        AppleLauncher(this, environment, getExecutionTarget(environment) as AppleDevice)
}