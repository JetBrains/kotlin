/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.project
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName

@Disabled("Example tests for evaluation purposes of the DSL")
class ExampleCompatibilityCompilationTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Sample compatibility compilation test that is run as part of each test suit")
    @TestMetadata("jvm-module-1")
    fun testCompatibilityCompilation(strategyConfig: CompilerExecutionStrategyConfiguration) {
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