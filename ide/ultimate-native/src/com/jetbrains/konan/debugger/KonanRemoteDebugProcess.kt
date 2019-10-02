/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan.debugger

import com.intellij.execution.filters.DefaultConsoleFiltersProvider
import com.intellij.execution.filters.TextConsoleBuilder
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.frame.XExecutionStack
import com.jetbrains.cidr.execution.RunParameters
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess
import com.jetbrains.cidr.execution.debugger.CidrSuspensionCause
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver
import com.jetbrains.cidr.execution.debugger.backend.LLFrame
import com.jetbrains.cidr.execution.debugger.backend.LLThread
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriver
import java.io.File

class KonanRemoteDebugProcess(
    parameters: RunParameters,
    session: XDebugSession,
    consoleBuilder: TextConsoleBuilder,
    private val executableFile: File,
    private val port: Int
) : KonanLocalDebugProcess(parameters, session, consoleBuilder, DefaultConsoleFiltersProvider()) {

    override fun doLoadTarget(driver: DebuggerDriver): DebuggerDriver.Inferior {
        val url = "connect://127.0.0.1:$port"
        return (driver as LLDBDriver).loadForAttachDebugServer(url, executableFile, null, emptyList(), null)
    }
}