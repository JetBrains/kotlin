/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated

@Isolated
class FullPipelineModularizedTest : AbstractModularizedJUnit5Test<FullPipelineModularizedTestPure>(
    FullPipelineModularizedTestPure(modularizedTestConfigFromSystemProperties())
) {
    @Test
    fun testTotalKotlin() = test.testTotalKotlinIfConfigured()
}

@Isolated
class FE1FullPipelineModularizedTest : AbstractModularizedJUnit5Test<FE1FullPipelineModularizedTestPure>(
    FE1FullPipelineModularizedTestPure(modularizedTestConfigFromSystemProperties())
) {
    @Test
    fun testTotalKotlin() = test.testTotalKotlinIfConfigured()
}

@Isolated
class FirResolveModularizedTotalKotlinTest : AbstractModularizedJUnit5Test<FirResolveModularizedTotalKotlinTestPure>(
    @Test
    FirResolveModularizedTotalKotlinTestPure(modularizedTestConfigFromSystemProperties())
) {
    fun testTotalKotlin() = test.testTotalKotlinIfConfigured()
}

@Isolated
class NonFirResolveModularizedTotalKotlinTest : AbstractModularizedJUnit5Test<NonFirResolveModularizedTotalKotlinTestPure>(
    NonFirResolveModularizedTotalKotlinTestPure(modularizedTestConfigFromSystemProperties())
) {
    @Test
    fun testTotalKotlin() = test.testTotalKotlinIfConfigured()
}

private fun AbstractModularizedTest.testTotalKotlinIfConfigured() {
    if (config.jpsDir == null) {
        Assumptions.abort<Unit>("Skipping modularized test: assuming it is not configured properly")
    }
    testTotalKotlin()
}

