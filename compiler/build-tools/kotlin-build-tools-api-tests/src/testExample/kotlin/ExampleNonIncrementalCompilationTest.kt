/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.expectFailWithError
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.project
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.writeText

class ExampleNonIncrementalCompilationTest : BaseCompilationTest() {
    @DisplayName("Sample non-incremental compilation test with two modules")
    @DefaultStrategyAgnosticCompilationTest
    fun myTest(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module1 = module("jvm-module-1")
            val module2 = module("jvm-module-2", listOf(module1))

            // this is not the scenario DSL, so the modules are not built at this moment

            // you should handle the right order of compilation between modules yourself
            module1.compile { module ->
                assertOutputs(module, "FooKt.class", "Bar.class", "BazKt.class")
            }
            module2.compile { module ->
                assertOutputs(module, "AKt.class", "BKt.class")
            }
        }
    }

    @DisplayName("Sample non-incremental compilation test with a single module and a compilation error")
    @DefaultStrategyAgnosticCompilationTest
    fun failedCompilationTest(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module1 = module("jvm-module-1")

            module1.sourcesDirectory.resolve("bar.kt").writeText("aaaa")

            module1.compile {
                expectFail()
                assertLogContainsPatterns(LogLevel.ERROR, ".*bar\\.kt:1:1 Syntax error: Expecting a top level declaration.*".toRegex())

                // equals to

                expectFailWithError(".*bar\\.kt:1:1 Syntax error: Expecting a top level declaration.*".toRegex())
            }
        }
    }
}