/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.DefaultDebugEnvironment
import com.intellij.debugger.engine.JavaDebugProcess
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.jetbrains.cidr.execution.CidrCommandLineState
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.execution.testing.CidrLauncher

class AndroidCommandLineState(
    private val configuration: MobileRunConfiguration,
    environment: ExecutionEnvironment
) : CidrCommandLineState(environment, FakeLauncher()) {
    private val device = environment.executionTarget as AndroidDevice
    private val apk = configuration.getProductBundle(environment)

    override fun startProcess(): ProcessHandler {
        return device.installAndLaunch(apk, configuration.project)
    }

    override fun startDebugProcess(session: XDebugSession): XDebugProcess {
        val console = TextConsoleBuilderFactory.getInstance().createBuilder(configuration.project).console
        val handler = device.installAndLaunch(apk, configuration.project, waitForDebugger = true)
        console.attachToProcess(handler)

        while (handler.debuggerPort == null) Thread.sleep(100) // FIXME

        val debugPort = handler.debuggerPort!!.toString()
        val connection = RemoteConnection(true, "localhost", debugPort, false)

        val debugEnvironment = DefaultDebugEnvironment(
            environment,
            { _, _ -> DefaultExecutionResult(console, handler) },
            connection, false
        )
        val debuggerSession = DebuggerManagerEx.getInstanceEx(configuration.project).attachVirtualMachine(debugEnvironment)!!
        return JavaDebugProcess.create(session, debuggerSession)
    }
}

private class FakeLauncher : CidrLauncher() {
    private fun error(): Nothing = throw IllegalStateException("this function should never be called")

    override fun getProject(): Project = error()
    override fun createProcess(state: CommandLineState): ProcessHandler = error()
    override fun createDebugProcess(state: CommandLineState, session: XDebugSession): CidrDebugProcess = error()
}
