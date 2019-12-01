/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

class PerfTestBuilder<SV, TV> {
    private lateinit var stats: Stats
    private lateinit var name: String
    private var warmUpIterations: Int = 5
    private var iterations: Int = 20
    private var setUp: (TestData<SV, TV>) -> Unit = { }
    private lateinit var test: (TestData<SV, TV>) -> Unit
    private var tearDown: (TestData<SV, TV>) -> Unit = { }
    private var profileEnabled: Boolean = false

    fun run() {
        stats.perfTest(
            testName = name,
            warmUpIterations = warmUpIterations,
            iterations = iterations,
            setUp = setUp,
            test = test,
            tearDown = tearDown,
            profileEnabled = profileEnabled
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

    fun profileEnabled(profileEnabled: Boolean) {
        this.profileEnabled = profileEnabled
    }
}

fun <SV, TV> performanceTest(initializer: PerfTestBuilder<SV, TV>.() -> Unit) {
    PerfTestBuilder<SV, TV>().apply(initializer).run()
}