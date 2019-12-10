/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.execution

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugSession
import com.jetbrains.cidr.execution.RunParameters
import com.jetbrains.cidr.execution.TrivialRunParameters
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.execution.debugger.IPhoneSimulatorDebugProcess
import com.jetbrains.cidr.execution.debugger.backend.LLDBDriverConfiguration
import com.jetbrains.cidr.execution.simulatorSupport.SimulatorConfiguration
import com.jetbrains.cidr.execution.simulatorSupport.SimulatorProcessHandler
import com.jetbrains.cidr.execution.testing.CidrLauncher
import com.jetbrains.mpp.AppleRunConfiguration
import java.io.File

abstract class AppleLauncher(
    val configuration: AppleRunConfiguration,
    val environment: ExecutionEnvironment,
    val device: AppleDevice
) : CidrLauncher() {
    override fun getProject(): Project = configuration.project

    override fun createDebugProcess(state: CommandLineState, session: XDebugSession): CidrDebugProcess {
        val parameters = createRunParameters(state)
        val process = createDebugProcess(parameters, session, state)
        configProcessHandler(process.processHandler, process.isDetachDefault, true, project)
        return process
    }

    protected fun createRunParameters(state: CommandLineState): RunParameters {
        val bundle = configuration.getProductBundle(state.environment)
        val installer = createInstaller(bundle)
        val configuration = LLDBDriverConfiguration()
        return TrivialRunParameters(configuration, installer, device.arch.type)
    }

    protected abstract fun createInstaller(bundle: File): AppleInstaller
    protected abstract fun createDebugProcess(parameters: RunParameters, session: XDebugSession, state: CommandLineState): CidrDebugProcess
}

class AppleSimulatorLauncher(
    configuration: AppleRunConfiguration,
    environment: ExecutionEnvironment,
    device: AppleSimulator,
    private val raw: SimulatorConfiguration
) : AppleLauncher(configuration, environment, device) {

    override fun createInstaller(bundle: File): AppleInstaller =
        AppleSimulatorInstaller(configuration, environment, bundle, raw)

    override fun createProcess(state: CommandLineState): ProcessHandler {
        val parameters = createRunParameters(state)
        val handler = SimulatorProcessHandler(parameters, null, device.id, false, false, true)
        configProcessHandler(handler, false, true, project)
        return handler
    }

    override fun createDebugProcess(parameters: RunParameters, session: XDebugSession, state: CommandLineState): CidrDebugProcess =
        object : IPhoneSimulatorDebugProcess(parameters, session, state.consoleBuilder, false) {
            override fun createSimulatorProcessHandler(params: RunParameters, allowConcurrentSessions: Boolean) =
                SimulatorProcessHandler(params, null, device.id, true, allowConcurrentSessions, true)
        }
}