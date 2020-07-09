package org.jetbrains.konan.gradle.execution

import com.intellij.execution.ExecutionException
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.filters.TextConsoleBuilder
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.util.ProgramParametersConfigurator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.xdebugger.XDebugSession
import com.jetbrains.cidr.cpp.CPPLog
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionGDBDriverConfiguration
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionLLDBDriverConfiguration
import com.jetbrains.cidr.cpp.toolchains.CPPEnvironment
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains
import com.jetbrains.cidr.execution.*
import com.jetbrains.cidr.execution.debugger.CidrCustomDebuggerProvider
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.lang.toolchains.CidrToolEnvironment
import com.jetbrains.cidr.toolchains.EnvironmentProblems
import com.jetbrains.konan.debugger.KonanLocalDebugProcess
import java.io.File

class CLionGradleKonanLauncher(
        private val myEnvironment: ExecutionEnvironment,
        private val myConfiguration: GradleKonanAppRunConfiguration
) : GradleKonanLauncher() {

    private val myExtensionsManager: CidrRunConfigurationExtensionsManager = CidrRunConfigurationExtensionsManager.getInstance()

    private val projectBaseDir: File?
        get() = project.basePath?.let { File(it) }

    private val buildAndRunConfigurations: GradleKonanAppRunConfiguration.BuildAndRunConfigurations
        @Throws(ExecutionException::class)
        get() {
            val target = myEnvironment.executionTarget
            if (target !is GradleKonanBuildProfileExecutionTarget) {
                throw ExecutionException(ProgramRunnerUtil.getCannotRunOnErrorMessage(myConfiguration, target))
            }

            val buildConfigurationProblems = BuildConfigurationProblems()
            return myConfiguration.getBuildAndRunConfigurations(target,
                                                                buildConfigurationProblems, true) ?: throw ExecutionException(
                buildConfigurationProblems.text)
        }

    override fun getProject(): Project {
        return myConfiguration.project
    }

    @Throws(ExecutionException::class)
    public override fun createProcess(state: CommandLineState): ProcessHandler {
        val usePty = usePty()
        val buildAndRunConfigurations = buildAndRunConfigurations
        val environment = getRunEnvironment(buildAndRunConfigurations)
        val context = ConfigurationExtensionContext()
        val cl = createCommandLine(buildAndRunConfigurations, environment, usePty)
        val runnerSettings = myEnvironment.runnerSettings
        val runner = myEnvironment.runner
        val runnerId = runner.runnerId

        myExtensionsManager.patchCommandLine(myConfiguration, runnerSettings, environment, cl, runnerId, context)
        state.consoleBuilder = createConsoleBuilder(environment, projectBaseDir)
        myExtensionsManager.patchCommandLineState(myConfiguration, runnerSettings, environment, projectBaseDir, state, runnerId, context)
        val handler = createProcessHandler(environment, cl, usePty)
        configProcessHandler(handler, false, true, project)
        myExtensionsManager.attachExtensionsToProcess(myConfiguration,
                                                      handler,
                                                      environment,
                                                      runnerSettings,
                                                      runnerId,
                                                      context)
        return handler
    }

    @Throws(ExecutionException::class)
    private fun createProcessHandler(environment: CPPEnvironment, cl: GeneralCommandLine, usePty: Boolean): ProcessHandler {
        val overrideWinPtyWidth = SystemInfo.isWindows && usePty && environment.useWindowsConsole()

        var oldCols: String? = null
        if (overrideWinPtyWidth) {
            oldCols = System.getProperty(WIN_PTY_COLS)
            System.setProperty(WIN_PTY_COLS, WIN_PTY_CONSOLE_WIDTH.toString())
        }
        val processHandler: ProcessHandler
        try {
            processHandler = environment.hostMachine.createProcess(cl, true, usePty)
        }
        finally {
            if (overrideWinPtyWidth) {
                if (oldCols != null) {
                    System.setProperty(WIN_PTY_COLS, oldCols)
                }
                else {
                    System.clearProperty(WIN_PTY_COLS)
                }
            }
        }
        return processHandler
    }

    private fun createConsoleBuilder(
            environment: CidrToolEnvironment,
            projectBaseDir: File?
    ): TextConsoleBuilder {
        return CidrConsoleBuilder(myConfiguration.project, environment, projectBaseDir)
    }

    private fun usePty(): Boolean {
        val application = ApplicationManager.getApplication()
        return PtyCommandLine.isEnabled() || application.isInternal || application.isUnitTestMode
    }

    @Throws(ExecutionException::class)
    public override fun createDebugProcess(state: CommandLineState, session: XDebugSession): CidrDebugProcess {
        val buildAndRunConfigurations = buildAndRunConfigurations
        val environment = getRunEnvironment(buildAndRunConfigurations)
        val context = ConfigurationExtensionContext()
        val runnerSettings = myEnvironment.runnerSettings
        val runner = myEnvironment.runner
        val runnerId = runner.runnerId

        val cl = createCommandLine(buildAndRunConfigurations, environment, false)
        environment.convertPathVariableToEnv(cl)
        myExtensionsManager.patchCommandLine(myConfiguration, runnerSettings, environment, cl, runnerId, context)
        val projectBaseDir = projectBaseDir
        state.consoleBuilder = createConsoleBuilder(environment, projectBaseDir)
        myExtensionsManager.patchCommandLineState(myConfiguration, runnerSettings, environment, projectBaseDir, state, runnerId, context)
        val backendFilterProvider = CidrDebugConsoleFilterProvider(environment, projectBaseDir)
        val parameters = getDebugParameters(environment, cl)
        val process = KonanLocalDebugProcess(parameters, session, state.consoleBuilder, backendFilterProvider, true)
        configProcessHandler(process.processHandler, process.isDetachDefault, true, project)
        myExtensionsManager.attachExtensionsToProcess(myConfiguration,
                                                      process.processHandler,
                                                      environment,
                                                      runnerSettings,
                                                      runnerId,
                                                      context)
        return process
    }

    @Throws(ExecutionException::class)
    private fun getDebugParameters(
            environment: CPPEnvironment,
            cl: GeneralCommandLine
    ): RunParameters {
        val installer = TrivialInstaller(cl)

        for (debuggerProvider in CidrCustomDebuggerProvider.EP_NAME.extensionList) {
            debuggerProvider.debuggerConfigurations.firstOrNull()?.let { config ->
                return TrivialRunParameters(config, installer)
            }
        }

        environment.toolSet.isDebugSupportDisabled?.let { debugSupportDisabled ->
            throw ExecutionException(debugSupportDisabled)
        }

        val config = if (environment.toolchain.debuggerKind.isLLDB())
            CLionLLDBDriverConfiguration(project, environment.toolchain)
        else
            CLionGDBDriverConfiguration(project, environment.toolchain)

        return TrivialRunParameters(config, installer)
    }

    @Throws(ExecutionException::class)
    private fun createCommandLine(
            buildAndRunConfigurations: GradleKonanAppRunConfiguration.BuildAndRunConfigurations,
            environment: CPPEnvironment,
            usePty: Boolean
    ): GeneralCommandLine {
        val runFile = buildAndRunConfigurations.runFile
        CPPLog.LOG.assertTrue(runFile != null)
        if (!runFile!!.exists()) throw ExecutionException("File not found: $runFile")

        return ApplicationManager.getApplication().runReadAction(ThrowableComputable<GeneralCommandLine, ExecutionException> {
            val cl: GeneralCommandLine
            if (usePty) {
                cl = PtyCommandLine()
                cl.withUseCygwinLaunch(environment.isCygwin)
            }
            else {
                cl = GeneralCommandLine()
            }
            cl.exePath = runFile.path

            val configurator = CidrCommandLineConfigurator(myConfiguration.project, getParameters(runFile.parent))
            configurator.configureCommandLine(cl)

            environment.prepare(cl, CidrToolEnvironment.PrepareFor.RUN)

            cl
        })
    }

    @Throws(ExecutionException::class)
    private fun getRunEnvironment(
            @Suppress("UNUSED_PARAMETER") buildAndRunConfigurations: GradleKonanAppRunConfiguration.BuildAndRunConfigurations
    ): CPPEnvironment {
        val environmentProblems = EnvironmentProblems()
        val environment = CPPToolchains.createCPPEnvironment(project, projectBaseDir, null, environmentProblems, false, null)
        if (environment == null) {
            environmentProblems.throwAsExecutionException()
        }
        assert(environment != null)
        return environment!!
    }

    private fun getParameters(defaultWorkingDir: String): CidrProgramParameters {
        val configurator = object : ProgramParametersConfigurator() {
            override fun getDefaultWorkingDir(project: Project): String {
                return defaultWorkingDir
            }
        }

        return CidrProgramParameters().apply { configurator.configureConfiguration(this, myConfiguration) }
    }

    companion object {
        private const val WIN_PTY_COLS = "win.pty.cols"
        private const val WIN_PTY_CONSOLE_WIDTH = 120
    }
}
