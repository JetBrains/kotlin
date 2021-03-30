/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import org.jetbrains.kotlin.idea.perf.profilers.ProfilerConfig

class PerfTestBuilder<SV, TV> {
    private lateinit var stats: Stats
    private lateinit var name: String
    private var warmUpIterations: Int = 5
    private var iterations: Int = 20
    private var setUp: (TestData<SV, TV>) -> Unit = { }
    private lateinit var test: (TestData<SV, TV>) -> Unit
    private var tearDown: (TestData<SV, TV>) -> Unit = { }
    private var checkStability: Boolean = true
    internal var profilerConfig: ProfilerConfig = ProfilerConfig()
    private var stopAtException: Boolean = false

    internal fun run() {
        stats.perfTest(
            testName = name,
            warmUpIterations = warmUpIterations,
            iterations = iterations,
            setUp = setUp,
            test = test,
            tearDown = tearDown,
            checkStability = checkStability,
            stopAtException = stopAtException,
        )
    }

    fun stats(stats: Stats) {
        this.stats = stats
    }

    fun name(name: String) {
        this.name = name
    }

    fun warmUpIterations(warmUpIterations: Int) {
        this.warmUpIterations = warmUpIterations
    }

    fun iterations(iterations: Int) {
        this.iterations = iterations
    }

    fun setUp(setUp: (TestData<SV, TV>) -> Unit) {
        this.setUp = setUp
    }

    fun test(test: (TestData<SV, TV>) -> Unit) {
        this.test = test
    }

    fun tearDown(tearDown: (TestData<SV, TV>) -> Unit) {
        this.tearDown = tearDown
    }

    fun profilerConfig(profilerConfig: ProfilerConfig) {
        this.profilerConfig = profilerConfig
    }

    fun checkStability(checkStability: Boolean) {
        this.checkStability = checkStability
    }

    fun stopAtException(stopAtException: Boolean) {
        this.stopAtException = stopAtException
    }
}

fun <SV, TV> performanceTest(initializer: PerfTestBuilder<SV, TV>.() -> Unit) {
    PerfTestBuilder<SV, TV>().apply(initializer).run()
}