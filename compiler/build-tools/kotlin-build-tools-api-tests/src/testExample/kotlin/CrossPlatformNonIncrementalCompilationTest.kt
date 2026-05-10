/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.expectFailWithError
import org.jetbrains.kotlin.buildtools.tests.compilation.model.*
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.junit.jupiter.api.DisplayName
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
            module1.sourcesDirectory.resolve("bar.kt").writeText("aaaa")
            module1.compile {
                expectFail()
                assertLogContainsPatterns(LogLevel.ERROR, ".*bar\\.kt:1:1 Syntax error: Expecting a top level declaration.*".toRegex())

                // equals to
                expectFailWithError(".*bar\\.kt:1:1 Syntax error: Expecting a top level declaration.*".toRegex())
            }
        }
    }

    @DisplayName("Simple compilation succeeds on all platforms")
    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    fun successfulCompilationAllPlatforms(project: ProjectCreator) {
        project {
            val module1 = module("jvm-module-1")
            // default assertion expects COMPILATION_SUCCESS
            module1.compile {}
            if (module1 is LinkableModule<*, *>) {
                module1.link {
                    assertOutputs("${module1.moduleName}.js")
                }
            }
        }
    }

}
