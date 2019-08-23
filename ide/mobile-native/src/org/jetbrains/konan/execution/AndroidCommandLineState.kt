/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.DefaultDebugUIEnvironment
import com.intellij.debugger.engine.JavaDebugProcess
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.configurations.RemoteState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.impl.XDebugSessionImpl
import com.jetbrains.cidr.execution.CidrCommandLineState
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.execution.testing.CidrLauncher
import java.io.OutputStream

class AndroidCommandLineState(
    private val configuration: MobileRunConfiguration,
    environment: ExecutionEnvironment
) : CidrCommandLineState(environment, FakeLauncher()) {
    private val project get() = configuration.project
    private val device = environment.executionTarget as AndroidDevice
    private val apk = configuration.getProductBundle(environment)

    override fun startProcess(): ProcessHandler {
        return device.installAndLaunch(apk, project)
    }

    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
        val isDebug = environment.executor.id == DefaultDebugExecutor.EXECUTOR_ID
        if (!isDebug) return super.execute(executor, runner)

        val consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project)
        val runConsole = consoleBuilder.console
        val debugConsole = consoleBuilder.console
        val handler = device.installAndLaunch(apk, project, waitForDebugger = true)
        runConsole.attachToProcess(handler)
        debugConsole.attachToProcess(handler)

        val wrapper = DebugProcessHandlerWrapper(handler)

        ApplicationManager.getApplication().executeOnPooledThread {
            while (handler.debuggerPort == null) Thread.sleep(100)
            val debugPort = handler.debuggerPort!!.toString()
            val connection = RemoteConnection(true, "localhost", debugPort, false)

            val result = DefaultExecutionResult(debugConsole, handler, *createActions(debugConsole, handler, executor))

            val debugState = object : RemoteState {
                override fun getRemoteConnection(): RemoteConnection = connection
                override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult? = result
            }
            wrapper.detachProcess() // to reuse already existing run tab

            runInEdt {
                val debugEnv = DefaultDebugUIEnvironment(environment, debugState, connection, false)
                val debuggerSession = DebuggerManagerEx.getInstanceEx(project).attachVirtualMachine(debugEnv.environment)!!
                val session = XDebuggerManager.getInstance(project).startSession(environment, object : XDebugProcessStarter() {
                    override fun start(session: XDebugSession): XDebugProcess =
                        JavaDebugProcess.create(session, debuggerSession)
                })
                (session as XDebugSessionImpl).showSessionTab()
                session.debugProcess.processHandler.startNotify()
            }
        }

        return DefaultExecutionResult(runConsole, wrapper)
    }

    companion object {
        private val log = logger<AndroidCommandLineState>()
    }
}

private class DebugProcessHandlerWrapper(val wrapped: AndroidProcessHandler) : ProcessHandler() {
    override fun getProcessInput(): OutputStream? = wrapped.processInput
    override fun detachIsDefault(): Boolean = wrapped.detachIsDefault()

    override fun destroyProcessImpl() {}
    override fun detachProcessImpl() {
        notifyProcessDetached()
    }
}

private class FakeLauncher : CidrLauncher() {
    private fun error(): Nothing = throw IllegalStateException("this function should never be called")

    override fun getProject(): Project = error()
    override fun createProcess(state: CommandLineState): ProcessHandler = error()
    override fun createDebugProcess(state: CommandLineState, session: XDebugSession): CidrDebugProcess = error()
}
