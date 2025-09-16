/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.File
import org.junit.jupiter.api.io.TempDir


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

class FullPipelineModularizedTest : AbstractModularizedJUnit5Test<FullPipelineModularizedTestPure>(
    FullPipelineModularizedTestPure(modularizedTestConfigFromSystemProperties())
) {
    fun testTotalKotlin() = test.testTotalKotlin()
}

class FE1FullPipelineModularizedTest : AbstractModularizedJUnit5Test<FE1FullPipelineModularizedTestPure>(
    FE1FullPipelineModularizedTestPure(modularizedTestConfigFromSystemProperties())
) {
    fun testTotalKotlin() = test.testTotalKotlin()
}

class FirResolveModularizedTotalKotlinTest : AbstractModularizedJUnit5Test<FirResolveModularizedTotalKotlinTestPure>(
    FirResolveModularizedTotalKotlinTestPure(modularizedTestConfigFromSystemProperties())
) {
    fun testTotalKotlin() = test.testTotalKotlin()
}

class NonFirResolveModularizedTotalKotlinTest : AbstractModularizedJUnit5Test<NonFirResolveModularizedTotalKotlinTestPure>(
    NonFirResolveModularizedTotalKotlinTestPure(modularizedTestConfigFromSystemProperties())
) {
    fun testTotalKotlin() = test.testTotalKotlin()
}

// base for generated tests
@Suppress("unused")
abstract class AbstractIsolatedFulPipelineTestRunner {

    @TempDir
    lateinit var tempPath: File

    fun runTest(modelPath: String) {
        val config = modularizedTestConfigFromSingleModelFile(File(modelPath))
        val test = AbstractIsolatedFullPipelineModularizedTest(config)
        test.runSingleModelCompilation(modelPath, tempPath)
    }
}