/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf.profilers

import org.jetbrains.kotlin.idea.perf.profilers.async.AsyncProfilerHandler
import org.jetbrains.kotlin.idea.perf.profilers.yk.YKProfilerHandler
import org.jetbrains.kotlin.idea.perf.util.logMessage

interface ProfilerHandler {
    fun startProfiling(activityName: String, options: List<String> = emptyList())

    fun stopProfiling(snapshotsPath: String, activityName: String, options: List<String> = emptyList())

    companion object {
        private var instance: ProfilerHandler? = null

        @Synchronized
        fun getInstance(): ProfilerHandler {
            return instance ?: run {
                val handler =
                    doOrLog("asyncProfiler not found") { AsyncProfilerHandler() }
                        ?: doOrLog("yourKit not found") { YKProfilerHandler() } ?: DummyProfilerHandler
                instance = handler
                return handler
            }
        }
    }

}

object DummyProfilerHandler : ProfilerHandler {
    override fun startProfiling(activityName: String, options: List<String>) {}

    override fun stopProfiling(snapshotsPath: String, activityName: String, options: List<String>) {}
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