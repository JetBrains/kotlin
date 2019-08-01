/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan


import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.execution.filters.DefaultConsoleFiltersProvider
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.util.ProgramParametersConfigurator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.impl.settings.XDebuggerSettingManagerImpl
import com.jetbrains.cidr.execution.RunParameters
import com.jetbrains.cidr.execution.TrivialInstaller
import com.jetbrains.cidr.execution.TrivialRunParameters
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.system.LocalHost
import com.jetbrains.konan.debugger.KonanLLDBDriverConfiguration
import com.jetbrains.konan.debugger.KonanLocalDebugProcess
import java.io.File


class IdeaKonanLauncher(
    val configuration: IdeaKonanRunConfiguration,
    private val runFile: File
) {
    @Throws(ExecutionException::class)
    fun startProcess(state: CommandLineState) = createProcess(state)

    private fun usePty(): Boolean {
        val application = ApplicationManager.getApplication()
        return PtyCommandLine.isEnabled() || application.isInternal || application.isUnitTestMode
    }

    @Throws(ExecutionException::class)
    private fun createCommandLine(usePty: Boolean): GeneralCommandLine {
        return ApplicationManager.getApplication().runReadAction(ThrowableComputable<GeneralCommandLine, ExecutionException> {
            val cl: GeneralCommandLine = if (usePty) PtyCommandLine() else GeneralCommandLine()
            cl.exePath = runFile.toString()
            val configurator = KonanCommandLineConfigurator(getParameters(runFile.parent.toString()))
            configurator.configureCommandLine(cl)
            cl
        })
    }

    @Throws(ExecutionException::class)
    fun createProcess(state: CommandLineState): ProcessHandler {
        val usePty = usePty()
        val cl = createCommandLine(usePty)
        return LocalHost.INSTANCE.createProcess(cl, true, usePty)
    }

    @Throws(ExecutionException::class)
    fun startDebugProcess(state: CommandLineState, session: XDebugSession): CidrDebugProcess {
        val result = createDebugProcess(state, session)
        result.start()
        return result
    }

    @Throws(ExecutionException::class)
    private fun getDebugParameters(cl: GeneralCommandLine): RunParameters {
        val installer = TrivialInstaller(cl)
        val workspace = IdeaKonanWorkspace.getInstance(configuration.project)
        return TrivialRunParameters(KonanLLDBDriverConfiguration(workspace.konanHome!!), installer)
    }

    @Throws(ExecutionException::class)
    fun createDebugProcess(state: CommandLineState, session: XDebugSession): CidrDebugProcess {
        val cl = createCommandLine(false)
        val parameters = getDebugParameters(cl)
        return KonanLocalDebugProcess(
            parameters,
            session,
            state.consoleBuilder,
            DefaultConsoleFiltersProvider()
        )
    }

    private fun getParameters(defaultWorkingDir: String): SimpleProgramParameters {
        val params = SimpleProgramParameters()
        val configurator = object : ProgramParametersConfigurator() {
            override fun getDefaultWorkingDir(project: Project): String {
                return defaultWorkingDir
            }
        }
        configurator.configureConfiguration(params, configuration)
        return params
    }
}