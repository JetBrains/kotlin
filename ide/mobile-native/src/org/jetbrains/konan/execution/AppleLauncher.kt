/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.xdebugger.XDebugSession
import com.jetbrains.cidr.ArchitectureType
import com.jetbrains.cidr.execution.*
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.execution.debugger.IPhoneDebugProcess
import com.jetbrains.cidr.execution.debugger.IPhoneSimulatorDebugProcess
import com.jetbrains.cidr.execution.debugger.backend.XcodeLLDBDriverConfiguration
import com.jetbrains.cidr.execution.deviceSupport.AMDevice
import com.jetbrains.cidr.execution.deviceSupport.AMDeviceUtil
import com.jetbrains.cidr.execution.testing.CidrLauncher
import java.io.File

class AppleLauncher(
    private val configuration: MobileRunConfiguration,
    private val arch: ArchitectureType,
    private val raw: AMDevice?
) : CidrLauncher() {
    override fun getProject(): Project = configuration.project

    override fun createProcess(state: CommandLineState): ProcessHandler {
        log.assertTrue(raw == null, "should start debug process instead")
        configureConsole(state)

        TODO("not implemented")
    }

    override fun createDebugProcess(state: CommandLineState, session: XDebugSession): CidrDebugProcess {
        configureConsole(state)

        val bundle = configuration.getProductBundle(state.environment)
        val installer = AppleInstaller(project, bundle, raw)
        val parameters = TrivialRunParameters(
            XcodeLLDBDriverConfiguration(null), installer, arch
        )
        val process: CidrDebugProcess =
            if (raw != null) {
                IPhoneDebugProcess(parameters, raw, session, state.consoleBuilder, true)
            } else {
                object : IPhoneSimulatorDebugProcess(parameters, session, state.consoleBuilder, true) {
                    override fun createSimulatorProcessHandler(
                        params: RunParameters,
                        allowConcurrentSessions: Boolean
                    ): ProcessHandlerWithPID {
                        TODO("not implemented")
                    }
                }
            }
        configProcessHandler(process.processHandler, process.isDetachDefault, true, project)
        return process
    }

    private fun configureConsole(state: CommandLineState) {
        state.consoleBuilder = CidrConsoleBuilder(project, null, project.basePath?.let { File(it) })
    }
}

private class AppleInstaller(
    private val project: Project,
    private val bundle: File,
    private val raw: AMDevice?
) : Installer {
    private var alreadyInstalled: GeneralCommandLine? = null

    override fun install(): GeneralCommandLine {
        alreadyInstalled?.let { return it }

        val commandLine = GeneralCommandLine()
        if (raw != null) {
            AMDeviceUtil.installApplicationInBackgroundAndAcquireDebugInfo(
                raw,
                true,
                bundle,
                project,
                commandLine
            )
            commandLine.exePath += "/" + FileUtil.getNameWithoutExtension(bundle)
        } else {
            commandLine.exePath = bundle.absolutePath
        }

        val params = SimpleProgramParameters().also {
            it.workingDirectory = File(commandLine.exePath).parentFile.parent
            it.isPassParentEnvs = false
        }
        CidrCommandLineConfigurator(project, params).configureCommandLine(commandLine)

        alreadyInstalled = commandLine
        return commandLine
    }

    override fun getExecutableFile(): File = bundle
    override fun getAppWorkingDir(): File? = null
}

private val log = logger<AppleLauncher>()