/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf.profilers

import org.jetbrains.kotlin.idea.perf.profilers.async.AsyncProfilerHandler
import org.jetbrains.kotlin.idea.perf.profilers.yk.YKProfilerHandler
import org.jetbrains.kotlin.idea.perf.util.logMessage
import java.nio.file.Path

interface ProfilerHandler {
    fun startProfiling()

    fun stopProfiling(attempt: Int)

    companion object {
        private var instance: ProfilerHandler? = null

        @Synchronized
        fun getInstance(profilerConfig: ProfilerConfig): ProfilerHandler {
            return instance ?: run {
                val handler =
                    doOrLog("asyncProfiler not found") { AsyncProfilerHandler(profilerConfig) }
                        ?: doOrLog("yourKit not found") { YKProfilerHandler(profilerConfig) } ?: DummyProfilerHandler
                instance = handler
                return handler
            }
        }

        fun determinePhasePath(dumpPath: Path, profilerConfig: ProfilerConfig): Path {
            val activityPath = dumpPath.parent.resolve(profilerConfig.path)
            val runNumber =
                (activityPath.toFile().listFiles()?.maxBy { it.name.toIntOrNull() ?: 0 }?.name?.toIntOrNull()
                    ?: 0) + 1
            val runPath = activityPath.resolve("$runNumber")
            runPath.toFile().mkdirs()
            logMessage { "profiler's run path found $runPath" }
            return runPath
        }
    }
}

object DummyProfilerHandler : ProfilerHandler {
    override fun startProfiling() {}

    override fun stopProfiling(attempt: Int) {}
}

internal fun <T> doOrLog(message: String, block: () -> T?): T? {
    return try {
        block()
    } catch (e: Throwable) {
        logMessage { message }
        null
    }
}

internal fun <T> doOrThrow(message: String, block: () -> T): T {
    return try {
        block()
    } catch (e: Throwable) {
        throw Exception(message)
    } ?: throw Exception(message)
}