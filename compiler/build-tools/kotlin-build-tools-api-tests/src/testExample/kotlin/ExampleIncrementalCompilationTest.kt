/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_EXPLICIT_BACKING_FIELDS
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration.Companion.KEEP_IC_CACHES_IN_MEMORY
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogDoesNotContainPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.project
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Avoid using this DSL unless you face a scenario that can't be solved within the scenario DSL
 */
class ExampleIncrementalCompilationTest : BaseCompilationTest() {
    @DisplayName("Sample IC test with a single module")
    @DefaultStrategyAgnosticCompilationTest
    @TestMetadata("jvm-module-1")
    fun testSingleModule(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module1 = module("jvm-module-1")

            // this is not the scenario DSL, so the module is not built at this moment

            module1.compileIncrementally(SourcesChanges.Unknown)

            val fooKt = module1.sourcesDirectory.resolve("foo.kt")
            fooKt.writeText(fooKt.readText().replace("foo()", "foo(i: Int = 1)"))

            module1.compileIncrementally(
                SourcesChanges.Known(modifiedFiles = listOf(fooKt.toFile()), removedFiles = emptyList()),
            ) {
                assertCompiledSources("foo.kt", "bar.kt")
                assertLogContainsPatterns(LogLevel.DEBUG, ".*Incremental compilation completed".toRegex())
            }
        }
    }

    @DisplayName("Sample IC test with 2 modules and custom compilation options")
    @DefaultStrategyAgnosticCompilationTest
    @TestMetadata("jvm-module-1")
    fun testTwoModules(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module1 = module("jvm-module-1")
            val module2 = module("jvm-module-2", listOf(module1))

            // this is not the scenario DSL, so the modules are not built at this moment

            // you should handle the right order of compilation between modules yourself
            module1.compileIncrementally(SourcesChanges.Unknown)
            module2.compileIncrementally(SourcesChanges.Unknown)

            val barKt = module1.sourcesDirectory.resolve("bar.kt")
            barKt.writeText(barKt.readText().replace("bar()", "bar(i: Int = 1)"))

            module1.compileIncrementally(
                SourcesChanges.Known(modifiedFiles = listOf(barKt.toFile()), removedFiles = emptyList()),
                icOptionsConfigAction = {
                    it[KEEP_IC_CACHES_IN_MEMORY] = false
                }
            )

            module2.compileIncrementally(
                SourcesChanges.Known(modifiedFiles = emptyList(), removedFiles = emptyList())
            ) {
                assertCompiledSources("b.kt")
            }
        }
    }

    @DisplayName("Sample IC test with 2 modules and custom compilation options")
    @DefaultStrategyAgnosticCompilationTest
    @TestMetadata("jvm-module-1")
    fun testExplicitBackingFieldsIncremental(strategyConfig: CompilerExecutionStrategyConfiguration) {
        if (strategyConfig.first::class.simpleName == "KotlinToolchainsV1Adapter") {
            // `X_EXPLICIT_BACKING_FIELDS` is not supported.
            return
        }

        project(strategyConfig) {
            val module = module("explicit-backing-fields-incremental-1") {
                @OptIn(ExperimentalCompilerArgument::class)
                it.compilerArguments[X_EXPLICIT_BACKING_FIELDS] = true
            }

            val main = module.sourcesDirectory.resolve("Main.kt")
            val mainContents = main.readText()

            // First time, `main` won't compile because of two `OPT_IN_USAGE_ERROR`s
            main.deleteIfExists()
            module.compileIncrementally(SourcesChanges.Unknown)
            main.writeText(mainContents)

            module.compileIncrementally(
                SourcesChanges.Known(modifiedFiles = listOf(main.toFile()), removedFiles = emptyList()),
            ) {
                expectFail()
                assertCompiledSources("Main.kt")
                assertLogContainsPatterns(LogLevel.DEBUG, ".*Incremental compilation completed".toRegex())
                assertLogDoesNotContainPatterns(LogLevel.ERROR, ".*Main\\.kt:8:19 This declaration needs opt-in\\. Its usage must be marked with '@Experimental' or '@OptIn\\(Experimental::class\\)'.*".toRegex())
                assertLogContainsPatterns(LogLevel.ERROR, ".*Main\\.kt:14:19 This declaration needs opt-in\\. Its usage must be marked with '@Experimental' or '@OptIn\\(Experimental::class\\)'.*".toRegex())
            }
        }
    }
}