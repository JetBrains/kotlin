/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File

abstract class AbstractModularizedJUnit5Test<T : AbstractModularizedTest>(protected val test: T) {
    @BeforeEach
    fun setUp() {
        test.setUp()
    }

    @AfterEach
    fun tearDown() {
        test.tearDown()
    }
}

// base for generated tests
abstract class AbstractIsolatedFulPipelineTestRunner {

    @TempDir
    lateinit var tempPath: File

    fun runTest(modelPath: String) {
        // A benchmark run may deliberately compile only a deterministic subset of the modules, see
        // `fir.bench.sample.*`: at parallelism 1 the full suite takes about an hour
        if (!ModularizedTestInstrumentation.isSelected(modelPath)) {
            ModularizedTestInstrumentation.noteSkippedBySampling()
            assumeTrue(false, "The module is excluded from this run by the fir.bench.sample.* configuration")
            return
        }
        val config = modularizedTestConfigFromSingleModelFile(File(modelPath))
        val test = AbstractIsolatedFullPipelineModularizedTest(config)
        // Every iteration is recorded separately: the first one is cold, the rest are warm, which separates the
        // file cache and JIT effects from the steady state
        var lastResult = ExitCode.OK
        var lastMessages = ""
        for (iteration in 0 until ModularizedTestInstrumentation.compileRepeat) {
            val performanceManager = ModularizedTestInstrumentation.createPerformanceManager()
            val measurement = ModularizedTestInstrumentation.start(modelPath, iteration)
            val [result, messageCollector] = test.runSingleModelCompilation(modelPath, tempPath, performanceManager) { args ->
                args.languageVersion = LANGUAGE_VERSION_K2
                configureCompatibleApiVersion(args)
                if (performanceManager != null) {
                    args.detailedPerf = ModularizedTestInstrumentation.detailedPerf
                }
            }
            ModularizedTestInstrumentation.finish(measurement, performanceManager, result)
            if (result != ExitCode.OK) {
                // A module that does not compile fails in every iteration; do not waste the time on the rest
                lastResult = result
                lastMessages = messageCollector.toString()
                break
            }
        }
        assertEquals(ExitCode.OK, lastResult) { lastMessages }
    }
}
