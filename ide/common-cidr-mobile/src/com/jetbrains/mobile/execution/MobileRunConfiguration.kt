package com.jetbrains.mobile.execution

import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.jetbrains.cidr.execution.CidrCommandLineState
import com.jetbrains.cidr.execution.CidrConsoleBuilder
import com.jetbrains.cidr.execution.testing.CidrLauncher
import java.io.File

interface MobileRunConfiguration : RunConfiguration {
    fun getProductBundle(environment: ExecutionEnvironment): File

    override fun getState(executor: Executor, environment: ExecutionEnvironment): CommandLineState =
        when (val device = environment.executionTarget) {
            is AppleDevice -> createAppleState(environment, executor, device)
            else -> createOtherState(environment)
        }

    fun createAppleState(environment: ExecutionEnvironment, executor: Executor, device: AppleDevice): CommandLineState {
        return CidrCommandLineState(environment, createCidrLauncher(environment, device)).also {
            it.consoleBuilder = CidrConsoleBuilder(project, null, project.basePath?.let { File(it) })
        }
    }

    fun createOtherState(environment: ExecutionEnvironment): CommandLineState

    fun createCidrLauncher(environment: ExecutionEnvironment, device: AppleDevice): CidrLauncher =
        AppleLauncher(this, environment, device)
}