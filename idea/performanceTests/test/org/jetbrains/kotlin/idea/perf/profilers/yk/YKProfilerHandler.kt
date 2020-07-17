/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf.profilers.yk

import org.jetbrains.kotlin.idea.perf.profilers.ProfilerConfig
import org.jetbrains.kotlin.idea.perf.profilers.ProfilerHandler
import org.jetbrains.kotlin.idea.perf.profilers.ProfilerHandler.Companion.determinePhasePath
import org.jetbrains.kotlin.idea.perf.profilers.doOrThrow
import org.jetbrains.kotlin.idea.perf.util.logMessage
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.management.ManagementFactory
import java.lang.reflect.Method
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * To use YKProfilerHandler:
 * - ${YOURKIT_PROFILER_HOME}/lib/yjp-controller-api-redist.jar has to be in a classpath
 * - add agentpath vm paramter like `-agentpath:${YOURKIT_PROFILER_HOME}/Resources/bin/mac/libyjpagent.dylib`
 */
class YKProfilerHandler(val profilerConfig: ProfilerConfig) : ProfilerHandler {

    private var controller: Any
    lateinit var phasePath: Path

    init {
        check(ManagementFactory.getRuntimeMXBean().inputArguments.any { it.contains("yjpagent") }) {
            "vm parameter -agentpath:\$YOURKIT_PROFILER_HOME/bin/../libyjpagent is not specified"
        }

        controller = doOrThrow("Unable to create com.yourkit.api.Controller instance") {
            ykLibClass.getConstructor().newInstance()
        }
    }

    override fun startProfiling() {
        try {
            if (profilerConfig.tracing) {
                startTracingMethod.invoke(controller, null)
            } else {
                startSamplingMethod.invoke(controller, null)
            }
        } catch (e: Exception) {
            val stringWriter = StringWriter().also { e.printStackTrace(PrintWriter(it)) }
            logMessage { "exception while starting profile ${e.localizedMessage} $stringWriter" }
        }
    }

    override fun stopProfiling(attempt: Int) {
        val pathToSnapshot = captureSnapshotMethod.invoke(controller, SNAPSHOT_WITHOUT_HEAP) as String
        stopCpuProfilingMethod.invoke(controller)
        val dumpPath = Paths.get(pathToSnapshot)
        if (!this::phasePath.isInitialized)
            phasePath = determinePhasePath(dumpPath, profilerConfig)
        val targetFile = phasePath.resolve("$attempt-${profilerConfig.name}.snapshot")
        logMessage { "dump is moved to $targetFile" }
        Files.move(dumpPath, targetFile)
    }

    companion object {
        const val SNAPSHOT_WITHOUT_HEAP = 0L

        private val ykLibClass: Class<*> = doOrThrow("yjp-controller-api-redist.jar is not in a classpath") {
            Class.forName("com.yourkit.api.Controller")
        }
        private val startSamplingMethod: Method = doOrThrow("com.yourkit.api.Controller#startSampling(String) not found") {
            ykLibClass.getMethod(
                "startSampling",
                String::class.java
            )
        }
        private val startTracingMethod: Method = doOrThrow("com.yourkit.api.Controller#startTracing(String) not found") {
            ykLibClass.getMethod(
                "startTracing",
                String::class.java
            )
        }
        private val captureSnapshotMethod: Method = doOrThrow("com.yourkit.api.Controller#captureSnapshot(long) not found") {
            ykLibClass.getMethod(
                "captureSnapshot",
                SNAPSHOT_WITHOUT_HEAP::class.java
            )
        }

        private val capturePerformanceSnapshotMethod: Method =
            doOrThrow("com.yourkit.api.Controller#capturePerformanceSnapshot() not found") {
                ykLibClass.getMethod("capturePerformanceSnapshot")
            }

        private val stopCpuProfilingMethod: Method = doOrThrow("com.yourkit.api.Controller#stopCPUProfiling() not found") {
            ykLibClass.getMethod("stopCpuProfiling")
        }
    }
}