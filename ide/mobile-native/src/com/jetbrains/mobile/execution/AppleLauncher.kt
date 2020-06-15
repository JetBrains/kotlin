/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mobile.execution

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugSession
import com.jetbrains.cidr.execution.RunParameters
import com.jetbrains.cidr.execution.TrivialRunParameters
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.execution.debugger.IPhoneDebugProcess
import com.jetbrains.cidr.execution.debugger.IPhoneSimulatorDebugProcess
import com.jetbrains.cidr.execution.debugger.backend.XcodeLLDBDriverConfiguration
import com.jetbrains.cidr.execution.deviceSupport.AMDevice
import com.jetbrains.cidr.execution.simulatorSupport.SimulatorConfiguration
import com.jetbrains.cidr.execution.simulatorSupport.SimulatorProcessHandler
import com.jetbrains.cidr.execution.CidrLauncher
import java.io.File

abstract class AppleLauncher(
    val configuration: MobileRunConfiguration,
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
        return TrivialRunParameters(XcodeLLDBDriverConfiguration(null), installer, device.arch.type)
    }

    protected abstract fun createInstaller(bundle: File): AppleInstaller
    protected abstract fun createDebugProcess(parameters: RunParameters, session: XDebugSession, state: CommandLineState): CidrDebugProcess
}

class ApplePhysicalDeviceLauncher(
    configuration: MobileRunConfiguration,
    environment: ExecutionEnvironment,
    device: ApplePhysicalDevice,
    private val raw: AMDevice
) : AppleLauncher(configuration, environment, device) {

    override fun createInstaller(bundle: File): AppleInstaller =
        ApplePhysicalDeviceInstaller(configuration, environment, bundle, raw)

    override fun createProcess(state: CommandLineState): ProcessHandler =
        throw IllegalStateException("should start debug process instead")

    override fun createDebugProcess(parameters: RunParameters, session: XDebugSession, state: CommandLineState): CidrDebugProcess =
        IPhoneDebugProcess(parameters, raw, session, state.consoleBuilder, true)
}

class AppleSimulatorLauncher(
    configuration: MobileRunConfiguration,
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