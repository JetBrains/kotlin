package com.jetbrains.mobile.execution

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.jetbrains.cidr.execution.Installer
import com.jetbrains.cidr.execution.RunParameters
import com.jetbrains.cidr.execution.TrivialRunParameters
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.execution.debugger.IPhoneDebugProcess
import com.jetbrains.cidr.execution.debugger.IPhoneSimulatorDebugProcess
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration
import com.jetbrains.cidr.execution.debugger.backend.XcodeLLDBDriverConfiguration
import com.jetbrains.cidr.execution.simulatorSupport.SimulatorProcessHandler
import com.jetbrains.cidr.execution.CidrLauncher
import com.jetbrains.konan.debugger.addKotlinHandler
import java.io.File

open class AppleLauncher<T : MobileRunConfiguration>(
    val configuration: T,
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

    override fun createProcess(state: CommandLineState): ProcessHandler {
        if (device is ApplePhysicalDevice) throw IllegalStateException("should start debug process instead")

        val parameters = createRunParameters(state)
        val handler = SimulatorProcessHandler(parameters, null, device.id, false, false, true)
        configProcessHandler(handler, false, true, project)
        handler.startNotify()
        return handler
    }

    private fun createRunParameters(state: CommandLineState): RunParameters {
        val bundle = configuration.getProductBundle(device)
        return TrivialRunParameters(createDebuggerDriverConfiguration(), createInstaller(bundle), device.arch.type)
    }

    protected open fun createDebuggerDriverConfiguration(): DebuggerDriverConfiguration = XcodeLLDBDriverConfiguration(null)

    protected open fun createInstaller(bundle: File): Installer = AppleInstaller(configuration, device, environment, bundle)

    protected open fun configureDebugProcess(process: CidrDebugProcess) {}

    private fun createDebugProcess(parameters: RunParameters, session: XDebugSession, state: CommandLineState): CidrDebugProcess {
        val process = when (device) {
            is ApplePhysicalDevice -> object : IPhoneDebugProcess(parameters, device.raw, session, state.consoleBuilder, true) {
                override fun getBreakpointHandlers(): Array<XBreakpointHandler<*>> =
                    addKotlinHandler(super.getBreakpointHandlers(), session.project)
            }

            is AppleSimulator -> object : IPhoneSimulatorDebugProcess(parameters, session, state.consoleBuilder, false) {
                override fun createSimulatorProcessHandler(params: RunParameters, allowConcurrentSessions: Boolean) =
                    SimulatorProcessHandler(params, null, device.id, true, allowConcurrentSessions, true)

                override fun getBreakpointHandlers(): Array<XBreakpointHandler<*>> =
                    addKotlinHandler(super.getBreakpointHandlers(), session.project)
            }
        }

        configureDebugProcess(process)

        return process
    }
}