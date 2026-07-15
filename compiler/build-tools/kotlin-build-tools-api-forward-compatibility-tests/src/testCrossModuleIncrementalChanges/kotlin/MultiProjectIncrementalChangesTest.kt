/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests.compilation

import org.jetbrains.kotlin.buildtools.forward.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.util.compile
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

/**
 * Build-system-agnostic part of the former KGP `IncrementalCompilationMultiProjectIT`: cross-module Kotlin IC in a
 * two-module `lib` (depended on by) `app` project. Verifies which sources are recompiled when a declaration in
 * `lib` changes. The Gradle-specific scenarios (`:lib:clean`, dependency/classpath edits, build-dir remapping,
 * missing-IC-state and cache-corruption fallback, Groovy interop, task up-to-date wiring) stay in the integration
 * test. The ABI-method-signature case already lives in [ClassicMultiprojectFirRunnerIncrementalTest].
 */
class MultiProjectIncrementalChangesTest : BaseCompilationTest() {
    private val aKt = "src/main/kotlin/bar/A.kt"
    private val barDummyKt = "src/main/kotlin/bar/BarDummy.kt"

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Adding a new method to a lib class recompiles its lib and app dependents")
    @TestMetadata("incrementalMultiproject")
    fun testAbiChangeInLib_addNewMethod(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val lib = module("incrementalMultiproject/lib")
            val app = module("incrementalMultiproject/app", dependencies = listOf(lib))

            lib.changeFile(aKt) { it.replace("fun a() {}", "fun a() {}\nfun newA() {}") }

            lib.compile(expectedDirtySet = setOf("src/main/kotlin/bar/A.kt", "src/main/kotlin/bar/B.kt"))
            app.compile(
                expectedDirtySet = setOf(
                    "src/main/kotlin/foo/AA.kt",
                    "src/main/kotlin/foo/AAA.kt",
                    "src/main/kotlin/foo/BB.kt",
                )
            )
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("A non-ABI change to a lib method body recompiles lib only, not app")
    @TestMetadata("incrementalMultiproject")
    fun testNonAbiChangeInLib_changeMethodBody(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val lib = module("incrementalMultiproject/lib")
            val app = module("incrementalMultiproject/app", dependencies = listOf(lib))

            lib.changeFile(aKt) { it.replace("fun a() {}", "fun a() { println() }") }

            lib.compile(expectedDirtySet = setOf("src/main/kotlin/bar/A.kt"))
            app.compile(expectedDirtySet = setOf())
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changing an isolated lib class recompiles only that class")
    @TestMetadata("incrementalMultiproject")
    fun testChangeIsolatedClassInLib(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val lib = module("incrementalMultiproject/lib")
            val app = module("incrementalMultiproject/app", dependencies = listOf(lib))

            lib.changeFile(barDummyKt) { "$it { fun m() = 42}" }

            lib.compile(expectedDirtySet = setOf("src/main/kotlin/bar/BarDummy.kt"))
            app.compile(expectedDirtySet = setOf())
        }
    }
}
