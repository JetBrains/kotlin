/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.util.JUnit4Assertions.assertEquals
import org.jetbrains.kotlin.util.PerformanceManagerImpl
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.io.path.createTempDirectory

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
        val config = modularizedTestConfigFromSingleModelFile(File(modelPath))
        val test = AbstractIsolatedFullPipelineModularizedTest(config)

        // Measurement harness (opt-in via `-Pfir.force.repeat`): compile the same model several
        // times in one JVM so JIT warms up and per-iteration numbers are steady, and capture the
        // analysis (frontend) phase and the Java-class-finder side stats for each iteration.
        val repeat = System.getProperty("fir.force.repeat")?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val timingFile = System.getProperty("fir.force.timing.file")?.let(::File)

        if (repeat == 1 && timingFile == null) {
            val compilation = test.runSingleModelCompilation(modelPath, tempPath) { args ->
                args.languageVersion = LANGUAGE_VERSION_K2
                configureCompatibleApiVersion(args)
            }
            assertEquals(ExitCode.OK, compilation.first) { compilation.second.toString() }
            return
        }

        // Keep the compiler environment alive across iterations, matching the multi-module FP test.
        CompilerSystemProperties.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY.value = "true"
        val moduleData = loadModuleDumpFile(File(modelPath), config).single()
        val javaDirect = System.getProperty("fir.force.javaDirect") ?: "(model default)"

        for (i in 0 until repeat) {
            val outputDir = createTempDirectory(tempPath.toPath(), "compile-output").toFile()
            val manager = PerformanceManagerImpl(JvmPlatforms.defaultJvmPlatform, "modularized bench")
            val messageCollector = MessageCollectorImpl()
            val result = test.processModule(moduleData, outputDir, messageCollector, manager) { args ->
                args.languageVersion = LANGUAGE_VERSION_K2
                configureCompatibleApiVersion(args)
            }
            val stats = manager.unitStats
            val analysis = stats.analysisStats
            val findJava = stats.findJavaClassStats
            val line = "javaDirect=%s iter=%d exit=%s hasErrors=%b files=%d lines=%d analysisWallMs=%d analysisCpuMs=%d totalWallMs=%d findJavaCount=%d findJavaMs=%d".format(
                javaDirect, i, result, stats.hasErrors, stats.filesCount, stats.linesCount,
                analysis?.millis ?: -1,
                analysis?.let { java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(it.cpuNanos) } ?: -1,
                stats.getTotalTime().millis,
                findJava?.count ?: -1,
                findJava?.time?.millis ?: -1,
            )
            println("[BENCH] $line")
            timingFile?.appendText(line + "\n")
            // In measurement mode we keep iterating for warm-up even if the model does not compile
            // cleanly on this machine (e.g. missing classpath outputs); the frontend still runs and
            // its timings are what we compare.
        }
    }
}
