/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf.profilers

interface PhaseProfiler {
    fun start()

    fun stop()
}

object DummyPhaseProfiler : PhaseProfiler {
    override fun start() {}
    override fun stop() {}
}

class ActualPhaseProfiler(
    private val profilerHandler: ProfilerHandler
) : PhaseProfiler {
    private var attempt: Int = 1

    override fun start() {
        profilerHandler.startProfiling()
    }

    override fun stop() {
        profilerHandler.stopProfiling(attempt)
        attempt++
    }
}

data class ProfilerConfig(var enabled: Boolean = false, var path: String = "", var name: String = "", var tracing: Boolean = false, var options: List<String> = emptyList(), var warmup: Boolean = false)
