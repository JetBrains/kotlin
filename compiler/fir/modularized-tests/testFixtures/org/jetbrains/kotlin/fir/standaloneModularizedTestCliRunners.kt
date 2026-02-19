/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

object StandaloneFulPipelineTestCliRunner {
    @JvmStatic
    fun main(args: Array<String>) {
        with (FullPipelineModularizedTestPure(modularizedTestConfigFromArgsOrSystemProperties(args))) {
            setUp()
            testTotalKotlin()
            tearDown()
        }
    }
}

object StandaloneResolveModularizedTestCliRunner {
    @JvmStatic
    fun main(args: Array<String>) {
        with (FirResolveModularizedTotalKotlinTestPure(modularizedTestConfigFromArgsOrSystemProperties(args))) {
            setUp()
            testTotalKotlin()
            tearDown()
        }
    }
}
