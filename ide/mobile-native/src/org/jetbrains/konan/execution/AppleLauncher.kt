/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugSession
import com.jetbrains.cidr.ArchitectureType
import com.jetbrains.cidr.execution.CidrConsoleBuilder
import com.jetbrains.cidr.execution.RunParameters
import com.jetbrains.cidr.execution.TrivialRunParameters
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.execution.debugger.IPhoneDebugProcess
import com.jetbrains.cidr.execution.debugger.IPhoneSimulatorDebugProcess
import com.jetbrains.cidr.execution.debugger.backend.XcodeLLDBDriverConfiguration
import com.jetbrains.cidr.execution.deviceSupport.AMDevice
import com.jetbrains.cidr.execution.simulatorSupport.SimulatorProcessHandler
import com.jetbrains.cidr.execution.testing.CidrLauncher
import java.io.File

abstract class AppleLauncher(val configuration: MobileRunConfiguration, val arch: ArchitectureType) : CidrLauncher() {
    override fun getProject(): Project = configuration.project

    protected fun configureConsole(state: CommandLineState) {
        state.consoleBuilder = CidrConsoleBuilder(project, null, project.basePath?.let { File(it) })
    }

    override fun createDebugProcess(state: CommandLineState, session: XDebugSession): CidrDebugProcess {
        configureConsole(state)

        val parameters = createRunParameters(state)
        val process = createDebugProcess(parameters, session, state)
        configProcessHandler(process.processHandler, process.isDetachDefault, true, project)
        return process
    }

    protected fun createRunParameters(state: CommandLineState): RunParameters {
        val bundle = configuration.getProductBundle(state.environment)
        val installer = createInstaller(bundle)
        return TrivialRunParameters(XcodeLLDBDriverConfiguration(null), installer, arch)
    }

    protected abstract fun createInstaller(bundle: File): AppleInstaller
    protected abstract fun createDebugProcess(parameters: RunParameters, session: XDebugSession, state: CommandLineState): CidrDebugProcess
}

class ApplePhysicalDeviceLauncher(
    configuration: MobileRunConfiguration,
    arch: ArchitectureType,
    private val device: ApplePhysicalDevice,
    private val raw: AMDevice
) : AppleLauncher(configuration, arch) {

    override fun createInstaller(bundle: File): AppleInstaller =
        AppleInstaller(project, bundle, device)

    override fun createProcess(state: CommandLineState): ProcessHandler =
        throw IllegalStateException("should start debug process instead")

    override fun createDebugProcess(parameters: RunParameters, session: XDebugSession, state: CommandLineState): CidrDebugProcess =
        IPhoneDebugProcess(parameters, raw, session, state.consoleBuilder, true)
}

class AppleSimulatorLauncher(
    configuration: MobileRunConfiguration,
    arch: ArchitectureType,
    private val device: AppleSimulator
) : AppleLauncher(configuration, arch) {

    override fun createInstaller(bundle: File): AppleInstaller =
        AppleInstaller(project, bundle, device)

    override fun createProcess(state: CommandLineState): ProcessHandler {
        configureConsole(state)

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