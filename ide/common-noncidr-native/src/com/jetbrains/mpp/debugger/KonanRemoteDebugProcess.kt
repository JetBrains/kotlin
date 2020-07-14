/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan.debugger

import com.intellij.execution.filters.DefaultConsoleFiltersProvider
import com.intellij.execution.filters.TextConsoleBuilder
import com.intellij.xdebugger.XDebugSession
import com.jetbrains.cidr.execution.RunParameters
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriver
import com.jetbrains.konan.KonanLog
import com.jetbrains.mpp.runconfig.AttachmentStrategy
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class KonanRemoteDebugProcess(
    parameters: RunParameters,
    session: XDebugSession,
    consoleBuilder: TextConsoleBuilder,
    private val executableFile: File,
    private val attachmentStrategy: AttachmentStrategy
) : KonanLocalDebugProcess(parameters, session, consoleBuilder, DefaultConsoleFiltersProvider()) {

    private fun executeCommand(command: String): List<String> {
        val result = mutableListOf<String>()
        try {
            val p = Runtime.getRuntime().exec(command)
            val input = BufferedReader(InputStreamReader(p.inputStream))
            input.lines().forEach { line ->
                if (line.isNotEmpty()) {
                    result.add(line)
                }
            }

            input.close()
        } catch (e: Exception) {
            KonanLog.LOG.warn("'$command' failed")
        }

        return result
    }

    private fun ensureProcessReady(isReady: () -> Boolean) {
        for (timing in listOf(1_000L, 2_000L, 4_000L)) {
            if (isReady()) {
                break
            }
            Thread.sleep(timing)
        }
    }

    private fun loadTargetByName(driver: LLDBDriver, name: String): DebuggerDriver.Inferior {
        val shortName = name.substring(name.lastIndexOf('/') + 1)
        ensureProcessReady { executeCommand("pgrep $shortName").isNotEmpty() }
        return driver.loadForAttach(name, false)
    }

    private fun loadTargetByPort(driver: LLDBDriver, port: Int): DebuggerDriver.Inferior {
        val url = "connect://127.0.0.1:$port"
        ensureProcessReady { executeCommand("netstat -an").find { it.contains("LISTEN") && it.contains("$port") } != null }
        return driver.loadForAttachDebugServer(url, executableFile, null, emptyList(), null)
    }

    override fun doLoadTarget(driver: DebuggerDriver) = when (attachmentStrategy) {
        is AttachmentStrategy.ByName -> loadTargetByName(driver as LLDBDriver, executableFile.absolutePath)
        is AttachmentStrategy.ByPort -> loadTargetByPort(driver as LLDBDriver, attachmentStrategy.port)
    }
}