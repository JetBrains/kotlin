/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase


abstract class AbstractModularizedJUnit4Test<T : AbstractModularizedTest>(protected val test: T) : KtUsefulTestCase() {
    override fun setUp() {
        super.setUp()
        test.setUp()
    }

    override fun tearDown() {
        super.tearDown()
        test.tearDown()
    }
}

class FullPipelineModularizedTest : AbstractModularizedJUnit4Test<FullPipelineModularizedTestPure>(
    FullPipelineModularizedTestPure(modularizedTestConfigFromSystemProperties())
) {
    fun testTotalKotlin() = test.testTotalKotlin()
}

class FE1FullPipelineModularizedTest : AbstractModularizedJUnit4Test<FE1FullPipelineModularizedTestPure>(
    FE1FullPipelineModularizedTestPure(modularizedTestConfigFromSystemProperties())
) {
    fun testTotalKotlin() = test.testTotalKotlin()
}

class FirResolveModularizedTotalKotlinTest : AbstractModularizedJUnit4Test<FirResolveModularizedTotalKotlinTestPure>(
    FirResolveModularizedTotalKotlinTestPure(modularizedTestConfigFromSystemProperties())
) {
    fun testTotalKotlin() = test.testTotalKotlin()
}

class NonFirResolveModularizedTotalKotlinTest : AbstractModularizedJUnit4Test<NonFirResolveModularizedTotalKotlinTestPure>(
    NonFirResolveModularizedTotalKotlinTestPure(modularizedTestConfigFromSystemProperties())
) {
    fun testTotalKotlin() = test.testTotalKotlin()
}
