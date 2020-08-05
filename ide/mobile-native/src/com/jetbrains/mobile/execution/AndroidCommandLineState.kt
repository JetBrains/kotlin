package com.jetbrains.mobile.execution

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.DefaultDebugEnvironment
import com.intellij.debugger.engine.JavaDebugProcess
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.configurations.RemoteState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.impl.XDebugSessionImpl
import java.io.OutputStream

abstract class AndroidCommandLineState(
    configuration: MobileRunConfigurationBase,
    protected val device: AndroidDevice,
    environment: ExecutionEnvironment
) : CommandLineState(environment) {
    protected val project = configuration.project
    protected val apk = configuration.getProductBundle(device)

    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
        val isDebug = environment.executor.id == DefaultDebugExecutor.EXECUTOR_ID
        if (!isDebug) return super.execute(executor, runner)

        val runConsole = consoleBuilder.console
        val debugConsole = consoleBuilder.console
        val handler = startDebugProcess()
        runConsole.attachToProcess(handler)
        debugConsole.attachToProcess(handler)

        val wrapper = DebugProcessHandlerWrapper(handler)

        handler.debuggerPort.thenAccept { debugPort ->
            ProcessIOExecutorService.INSTANCE.execute {
                try {
                    val connection = RemoteConnection(true, "localhost", debugPort.toString(), false)

                    val result = DefaultExecutionResult(debugConsole, handler, *createActions(debugConsole, handler, executor))

                    val debugState = object : RemoteState {
                        override fun getRemoteConnection(): RemoteConnection = connection
                        override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult? = result
                    }
                    wrapper.detachProcess() // to reuse already existing run tab

                    runInEdt {
                        val debugEnvironment = DefaultDebugEnvironment(environment, debugState, connection, false)
                        val debuggerSession = DebuggerManagerEx.getInstanceEx(project).attachVirtualMachine(debugEnvironment)!!
                        val session = XDebuggerManager.getInstance(project).startSession(environment, object : XDebugProcessStarter() {
                            override fun start(session: XDebugSession): XDebugProcess =
                                JavaDebugProcess.create(session, debuggerSession)
                        })
                        (session as XDebugSessionImpl).showSessionTab()
                        session.debugProcess.processHandler.startNotify()
                    }
                } catch (e: Throwable) {
                    log.error(e)
                }
            }
        }

        return DefaultExecutionResult(runConsole, wrapper)
    }

    protected abstract fun startDebugProcess(): AndroidProcessHandler

    private class DebugProcessHandlerWrapper(val wrapped: AndroidProcessHandler) : ProcessHandler() {
        override fun getProcessInput(): OutputStream? = wrapped.processInput
        override fun detachIsDefault(): Boolean = wrapped.detachIsDefault()

        override fun destroyProcessImpl() {}
        override fun detachProcessImpl() {
            notifyProcessDetached()
        }
    }

    companion object {
        private val log = logger<AndroidCommandLineState>()
    }
}

open class AndroidAppCommandLineState(
    configuration: MobileRunConfigurationBase,
    device: AndroidDevice,
    environment: ExecutionEnvironment
) : AndroidCommandLineState(configuration, device, environment) {

    override fun startProcess(): AndroidProcessHandler =
        device.installAndLaunch(apk, project)

    override fun startDebugProcess(): AndroidProcessHandler =
        device.installAndLaunch(apk, project, waitForDebugger = true)
}
