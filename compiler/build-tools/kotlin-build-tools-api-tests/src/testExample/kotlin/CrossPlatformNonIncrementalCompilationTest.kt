/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.expectFailWithError
import org.jetbrains.kotlin.buildtools.tests.compilation.model.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText

/**
 * Tests that verify compilation behavior across both JVM and JS platforms.
 * Each test runs the same logic using both [jvmProject] (JVM) and [jsProject] (JS).
 */
class CrossPlatformNonIncrementalCompilationTest : BaseCompilationTest() {
    @DisplayName("Compilation with syntax error fails on all platforms")
    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    fun failedCompilationAllPlatforms(project: ProjectCreator) {
        project {
            val module1 = module("jvm-module-1")
            verifySyntaxErrorCompilationFails(module1)
        }
    }

    @DisplayName("Simple compilation succeeds on all platforms")
    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    fun successfulCompilationAllPlatforms(project: ProjectCreator) {
        project {
            val module1 = module("jvm-module-1")
            verifySuccessfulCompilation(module1)
            if (module1 is LinkableModule<*, *>) {
                module1.link {
                    assertOutputs("${module1.moduleName}.js")
                }
            }
        }
    }

    companion object {
        private fun <O : BaseCompilationOperation, B : BaseCompilationOperation.Builder, IC : BaseIncrementalCompilationConfiguration.Builder> verifySyntaxErrorCompilationFails(
            module: Module<O, B, IC>,
        ) {
            module.sourcesDirectory.resolve("bar.kt").writeText("aaaa")

            module.compile {
                expectFail()
                assertLogContainsPatterns(LogLevel.ERROR, ".*bar\\.kt:1:1 Syntax error: Expecting a top level declaration.*".toRegex())

                // equals to
                expectFailWithError(".*bar\\.kt:1:1 Syntax error: Expecting a top level declaration.*".toRegex())
            }
        }

        private fun <O : BaseCompilationOperation, B : BaseCompilationOperation.Builder, IC : BaseIncrementalCompilationConfiguration.Builder> verifySuccessfulCompilation(
            module: Module<O, B, IC>,
        ) {
            // default assertion expects COMPILATION_SUCCESS
            module.compile {
            }
        }
    }
}
