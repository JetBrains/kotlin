/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.cli.common.profiling.AsyncProfilerHelper
import java.io.File

private val ASYNC_PROFILER_LIB = System.getProperty("fir.bench.use.async.profiler.lib")
private val ASYNC_PROFILER_START_CMD = System.getProperty("fir.bench.use.async.profiler.cmd.start")
private val ASYNC_PROFILER_STOP_CMD = System.getProperty("fir.bench.use.async.profiler.cmd.stop")
private val PROFILER_SNAPSHOT_DIR = System.getProperty("fir.bench.snapshot.dir") ?: "tmp/snapshots"

class AsyncProfilerControl {
    private val asyncProfiler = if (ASYNC_PROFILER_LIB != null) {
        try {
            AsyncProfilerHelper.getInstance(ASYNC_PROFILER_LIB)
        } catch (e: ExceptionInInitializerError) {
            if (e.cause is ClassNotFoundException) {
                throw IllegalStateException("Async-profiler initialization error, make sure async-profiler.jar is on classpath", e.cause)
            }
            throw e
        }
    } else {
        null
    }

    private fun executeAsyncProfilerCommand(command: String?, pass: Int, reportDateStr: String) {
        if (asyncProfiler != null) {
            require(command != null)
            fun String.replaceParams(): String =
                this.replace("\$REPORT_DATE", reportDateStr)
                    .replace("\$PASS", pass.toString())

            val snapshotDir = File(PROFILER_SNAPSHOT_DIR.replaceParams()).also { it.mkdirs() }
            val expandedCommand = command
                .replace("\$SNAPSHOT_DIR", snapshotDir.toString())
                .replaceParams()
            val result = asyncProfiler.execute(expandedCommand)
            println("PROFILER: $result")
        }
    }


    fun beforePass(pass: Int, reportDateStr: String) {
        executeAsyncProfilerCommand(ASYNC_PROFILER_START_CMD, pass, reportDateStr)
    }

    fun afterPass(pass: Int, reportDateStr: String) {
        executeAsyncProfilerCommand(ASYNC_PROFILER_STOP_CMD, pass, reportDateStr)
    }
}