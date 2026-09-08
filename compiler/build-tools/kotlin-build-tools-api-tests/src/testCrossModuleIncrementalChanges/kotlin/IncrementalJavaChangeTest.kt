/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration.Companion.PRECISE_JAVA_TRACKING
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertNoCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

/**
 * Build-system-agnostic part of the former KGP `IncrementalJavaChangeIT`: cross-module incremental compilation
 * with Java source and ABI changes in a two-module `lib` (depended on by) `app` project.
 *
 * Precise Java tracking is only implemented for K1 (see KT-57147), so with the default K2 frontend the
 * `PRECISE_JAVA_TRACKING` option has no observable effect. Therefore only the non-precise behavior is covered here,
 * with the option set explicitly to make that expectation obvious.
 */
@DisplayName("Incremental compilation with Java source and ABI changes")
class IncrementalJavaChangeTest : BaseCompilationTest() {
    private val javaClassFile = "src/main/java/bar/JavaClass.java"
    private val trackedJavaClassFile = "src/main/java/bar/TrackedJavaClass.java"

    // shared between tests, so that modules with the same configuration are compiled only once, see `Scenario.module`
    private val disablePreciseJavaTracking: (JvmSnapshotBasedIncrementalCompilationConfiguration.Builder) -> Unit = {
        it[PRECISE_JAVA_TRACKING] = false
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Lib: method signature ABI change in Java class recompiles dependent Kotlin files in app")
    @TestMetadata("incrementalMultiprojectJava")
    fun testAbiChangeInLib_changeMethodSignature(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val lib = module("incrementalMultiprojectJava/lib", compileJavaSources = true, icOptionsConfigAction = disablePreciseJavaTracking)
            val app = module("incrementalMultiprojectJava/app", dependencies = listOf(lib), icOptionsConfigAction = disablePreciseJavaTracking)

            lib.changeFile(javaClassFile) { it.replace("String getString", "Object getString") }

            lib.compile()
            app.compile {
                assertCompiledSources(
                    "src/main/kotlin/foo/JavaClassChild.kt",
                    "src/main/kotlin/foo/useJavaClass.kt",
                )
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Lib: method body non-ABI change in Java class does not recompile Kotlin files in app")
    @TestMetadata("incrementalMultiprojectJava")
    fun testNonAbiChangeInLib_changeMethodBody(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val lib = module("incrementalMultiprojectJava/lib", compileJavaSources = true, icOptionsConfigAction = disablePreciseJavaTracking)
            val app = module("incrementalMultiprojectJava/app", dependencies = listOf(lib), icOptionsConfigAction = disablePreciseJavaTracking)

            lib.changeFile(javaClassFile) { it.replace("Hello, World!", "Hello, World!!!!") }

            lib.compile()
            app.compile {
                assertNoCompiledSources()
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Lib: tracked method signature ABI change with disabled precise Java tracking recompiles same module usages")
    @TestMetadata("incrementalMultiprojectJava")
    fun testAbiChangeInLib_changeMethodSignature_tracked_disablePreciseJavaTracking(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val lib = module("incrementalMultiprojectJava/lib", compileJavaSources = true, icOptionsConfigAction = disablePreciseJavaTracking)
            val app = module("incrementalMultiprojectJava/app", dependencies = listOf(lib), icOptionsConfigAction = disablePreciseJavaTracking)

            lib.changeFile(trackedJavaClassFile) { it.replace("String getString", "Object getString") }

            lib.compile {
                assertCompiledSources("src/main/kotlin/bar/useTrackedJavaClassSameModule.kt")
            }
            app.compile {
                assertCompiledSources(
                    "src/main/kotlin/foo/TrackedJavaClassChild.kt",
                    "src/main/kotlin/foo/useTrackedJavaClass.kt",
                )
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Lib: tracked method body non-ABI change with disabled precise Java tracking recompiles same module usages")
    @TestMetadata("incrementalMultiprojectJava")
    fun testNonAbiChangeInLib_changeMethodBody_tracked_disablePreciseJavaTracking(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val lib = module("incrementalMultiprojectJava/lib", compileJavaSources = true, icOptionsConfigAction = disablePreciseJavaTracking)
            val app = module("incrementalMultiprojectJava/app", dependencies = listOf(lib), icOptionsConfigAction = disablePreciseJavaTracking)

            lib.changeFile(trackedJavaClassFile) { it.replace("Hello, World!", "Hello, World!!!!") }

            lib.compile {
                assertCompiledSources("src/main/kotlin/bar/useTrackedJavaClassSameModule.kt")
            }
            app.compile {
                assertNoCompiledSources()
            }
        }
    }
}
