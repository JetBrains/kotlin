/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests

import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.project
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

class NonIncrementalCompilationSmokeTest : BaseCompilationTest() {
    @DisplayName("Non-incremental compilation produces only expected outputs in multi-module setup")
    @DefaultStrategyAgnosticCompilationTest
    @TestMetadata("jvm-module-1")
    fun multiModule(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module1 = module("jvm-module-1")
            val module2 = module("jvm-module-2", listOf(module1))

            module1.compile { module ->
                assertOutputs(module, "FooKt.class", "Bar.class", "BazKt.class")
            }
            module2.compile { module ->
                assertOutputs(module, "AKt.class", "BKt.class")
            }
        }
    }
}