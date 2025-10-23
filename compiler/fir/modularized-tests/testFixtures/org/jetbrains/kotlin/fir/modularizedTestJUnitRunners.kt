/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.test.util.JUnit4Assertions.assertEquals
import org.junit.jupiter.api.AfterEach
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
        val config = modularizedTestConfigFromSingleModelFile(File(modelPath))
        val test = AbstractIsolatedFullPipelineModularizedTest(config)
        val (result, messageCollector) = test.runSingleModelCompilation(modelPath, tempPath) { args ->
            args.languageVersion = LANGUAGE_VERSION_K2
            configureCompatibleApiVersion(args)
        }
        assertEquals(ExitCode.OK, result) { messageCollector.toString() }
    }
}