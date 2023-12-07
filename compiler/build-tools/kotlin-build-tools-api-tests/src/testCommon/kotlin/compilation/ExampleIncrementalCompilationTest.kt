/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.project
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.readText
import kotlin.io.path.writeText

@Disabled("Example tests for evaluation purposes of the DSL")
class ExampleIncrementalCompilationTest : BaseCompilationTest() {
    @DisplayName("Sample IC test with a single module")
    @DefaultStrategyAgnosticCompilationTest
    fun singleModuleTest(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project {
            val module1 = module("jvm-module-1")

            module1.compileIncrementally(strategyConfig, SourcesChanges.Unknown)

            val fooKt = module1.sourcesDirectory.resolve("foo.kt")
            fooKt.writeText(fooKt.readText().replace("foo()", "foo(i: Int = 1)"))

            module1.compileIncrementally(
                strategyConfig,
                SourcesChanges.Known(modifiedFiles = listOf(fooKt.toFile()), removedFiles = emptyList()),
            ) {
                assertCompiledSources("foo.kt", "bar.kt")
                assertLogContainsPatterns(LogLevel.DEBUG, ".*Incremental compilation completed".toRegex())
            }
        }
    }

    @DisplayName("Sample IC test with 2 modules and custom compilation options")
    @DefaultStrategyAgnosticCompilationTest
    fun twoModulesTest(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project {
            val module1 = module("jvm-module-1")
            val module2 = module("jvm-module-2", listOf(module1))

            module1.compileIncrementally(strategyConfig, SourcesChanges.Unknown)
            module2.compileIncrementally(strategyConfig, SourcesChanges.Unknown)

            val barKt = module1.sourcesDirectory.resolve("bar.kt")
            barKt.writeText(barKt.readText().replace("bar()", "bar(i: Int = 1)"))

            module1.compileIncrementally(
                strategyConfig,
                SourcesChanges.Known(modifiedFiles = listOf(barKt.toFile()), removedFiles = emptyList()),
                incrementalCompilationConfigAction = {
                    it.keepIncrementalCompilationCachesInMemory(false)
                },
            )

            module2.compileIncrementally(
                strategyConfig,
                SourcesChanges.Known(modifiedFiles = emptyList(), removedFiles = emptyList())
            ) {
                assertCompiledSources("b.kt")
            }
        }
    }
}