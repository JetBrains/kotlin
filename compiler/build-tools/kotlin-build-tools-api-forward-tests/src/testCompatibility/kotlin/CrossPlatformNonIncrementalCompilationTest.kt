/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests

import org.jetbrains.kotlin.buildtools.forward.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.assertions.assertOutputsContains
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.assertions.expectFailWithError
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.*
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
            val module1 = module("basic-multimodule-project/module-1")
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
            val module1 = module("basic-multimodule-project/module-1")
            // default assertion expects COMPILATION_SUCCESS
            module1.compile {}
            if (module1 is LinkableModule<*, *>) {
                module1.link {
                    assertOutputsContains(module1.expectedOutputFileName)
                }
            }
        }
    }

    @DisplayName("A missing classpath dependency fails the compilation on all platforms")
    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    fun missingClasspathDependencyFailsCompilationAllPlatforms(project: ProjectCreator) {
        project {
            // module-2 references `Bar` from module-1; without the dependency the reference is unresolved.
            val consumer = module("basic-multimodule-project/module-2")
            consumer.compile {
                expectFail()
                assertLogContainsPatterns(LogLevel.ERROR, ".*[Uu]nresolved reference.*Bar.*".toRegex())
            }
        }
    }
}
