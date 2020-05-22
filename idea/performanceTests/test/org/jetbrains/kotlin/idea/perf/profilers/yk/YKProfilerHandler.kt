/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf.profilers.yk

import org.jetbrains.kotlin.idea.perf.profilers.ProfilerHandler
import org.jetbrains.kotlin.idea.perf.profilers.doOrThrow
import org.jetbrains.kotlin.idea.perf.util.logMessage
import java.lang.management.ManagementFactory
import java.lang.reflect.Method
import java.nio.file.Files
import java.nio.file.Paths

/**
 * To use YKProfilerHandler:
 * - ${YOURKIT_PROFILER_HOME}/lib/yjp-controller-api-redist.jar has to be in a classpath
 * - add agentpath vm paramter like `-agentpath:${YOURKIT_PROFILER_HOME}/Resources/bin/mac/libyjpagent.dylib`
 */
class YKProfilerHandler : ProfilerHandler {

    private var controller: Any

    init {
        check(ManagementFactory.getRuntimeMXBean().inputArguments.any { it.contains("yjpagent") }) {
            "vm parameter -agentpath:\$YOURKIT_PROFILER_HOME/bin/../libyjpagent is not specified"
        }

        controller = doOrThrow("Unable to create com.yourkit.api.Controller instance") {
            ykLibClass.getConstructor().newInstance()
        }
    }

    override fun startProfiling(activityName: String, options: List<String>) {
        startCPUSamplingMethod.invoke(controller, null)
    }

    override fun stopProfiling(snapshotsPath: String, activityName: String, options: List<String>) {
        val dumpPath = captureSnapshotMethod.invoke(controller, SNAPSHOT_WITHOUT_HEAP) as String
        stopCPUProfilingMethod.invoke(controller)
        val path = Paths.get(dumpPath)
        val target = path.parent.resolve(snapshotsPath)
        logMessage { "dump is moved to $target" }
        Files.move(path, target)
    }

    companion object {
        const val SNAPSHOT_WITHOUT_HEAP = 0L

        private val ykLibClass: Class<*> = doOrThrow("yjp-controller-api-redist.jar is not in a classpath") {
            Class.forName("com.yourkit.api.Controller")
        }
        private val startCPUSamplingMethod: Method = doOrThrow("com.yourkit.api.Controller#startCPUSampling(String) not found") {
            ykLibClass.getMethod(
                "startCPUSampling",
                String::class.java
            )
        }
        private val captureSnapshotMethod: Method = doOrThrow("com.yourkit.api.Controller#captureSnapshot(long) not found") {
            ykLibClass.getMethod(
                "captureSnapshot",
                SNAPSHOT_WITHOUT_HEAP::class.java
            )
        }

        private val stopCPUProfilingMethod: Method = doOrThrow("com.yourkit.api.Controller#stopCPUProfiling() not found") {
            ykLibClass.getMethod("stopCPUProfiling")
        }
    }
}