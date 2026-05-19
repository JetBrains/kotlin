/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertClassDeclarationsContain
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

@DisplayName("Annotation changes in incremental compilation")
class AnnotationChangesTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-14440: Annotation list changes should be detected and used in incremental compilation (same module)")
    @TestMetadata("ic-scenarios/annotations")
    fun testAnnotationListChangeRecompilesUsagesSameModule(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val mod = module("ic-scenarios/annotations/lib")

            mod.replaceFileWithVersion("A.kt", "add-my-annotation")
            mod.compile {
                assertCompiledSources("A.kt", "B.kt")
            }

            mod.replaceFileWithVersion("A.kt", "add-my-annotation-and-deprecation")
            mod.compile {
                assertCompiledSources("A.kt", "B.kt")
                assertLogContainsPatterns(LogLevel.WARN, ".*B\\.kt:6:13 'fun foo\\(\\): Unit' is deprecated. Deprecated.".toRegex())
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-14440: Annotation list changes should be detected and used in incremental compilation (cross module)")
    @TestMetadata("ic-scenarios/annotations")
    fun testAnnotationListChangeRecompilesUsagesDifferentModules(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val lib = module("ic-scenarios/annotations/lib")
            val app = module("ic-scenarios/annotations/app", listOf(lib))

            lib.replaceFileWithVersion("A.kt", "add-my-annotation")
            lib.compile()
            app.compile {
                assertCompiledSources("C.kt")
            }

            lib.replaceFileWithVersion("A.kt", "add-my-annotation-and-deprecation")
            lib.compile()
            app.compile {
                assertCompiledSources("C.kt")
                assertLogContainsPatterns(LogLevel.WARN, ".*C\\.kt:6:13 'fun foo\\(\\): Unit' is deprecated. Deprecated.".toRegex())
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changes to annotation parameters should be detected and used in incremental compilation (same module)")
    @TestMetadata("ic-scenarios/annotations")
    fun testAnnotationParameterChangeRecompilesUsagesSameModule(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val mod = module("ic-scenarios/annotations/lib")

            mod.replaceFileWithVersion("A.kt", "add-deprecation")
            mod.compile {
                assertCompiledSources("A.kt", "B.kt")
                assertLogContainsPatterns(LogLevel.WARN, ".*B\\.kt:6:13 'fun foo\\(\\): Unit' is deprecated. Deprecated.".toRegex())
            }

            mod.replaceFileWithVersion("A.kt", "add-deprecation-error")
            mod.compile {
                expectFail()
                assertCompiledSources("A.kt", "B.kt")
                assertLogContainsPatterns(LogLevel.ERROR, ".*B\\.kt:6:13 'fun foo\\(\\): Unit' is deprecated. Deprecated.".toRegex())
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changes to annotation parameters should be detected and used in incremental compilation (cross module)")
    @TestMetadata("ic-scenarios/annotations")
    fun testAnnotationParameterChangeRecompilesUsagesDifferentModules(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val lib = module("ic-scenarios/annotations/lib")
            val app = module("ic-scenarios/annotations/app", listOf(lib))

            lib.replaceFileWithVersion("A.kt", "add-deprecation")
            lib.compile()
            app.compile {
                assertCompiledSources("C.kt")
                assertLogContainsPatterns(LogLevel.WARN, ".*C\\.kt:6:13 'fun foo\\(\\): Unit' is deprecated. Deprecated.".toRegex())
            }

            lib.replaceFileWithVersion("A.kt", "add-deprecation-error")
            lib.deleteFile("B.kt")
            lib.compile()
            app.compile {
                expectFail()
                assertCompiledSources("C.kt")
                assertLogContainsPatterns(LogLevel.ERROR, ".*C\\.kt:6:13 'fun foo\\(\\): Unit' is deprecated. Deprecated.".toRegex())
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-55293: Changing annotation argument on interface should lead to recompilation of implementation that propagate the annotation through bridges")
    @TestMetadata("ic-scenarios/annotations")
    fun testAnnotationParameterChangeRecompilesBridges(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val lib = module("ic-scenarios/annotations/lib")

            lib.replaceFileWithVersion("MyInterface.kt", "add-deprecation-error")
            lib.compile {
                assertCompiledSources("MyInterface.kt", "MyClass.kt")
                assertClassDeclarationsContain(
                    classFqn = "MyClass",
                    setOf(
                        "public void someMethod();", // bridge method
                    )
                )
            }
        }
    }
}
