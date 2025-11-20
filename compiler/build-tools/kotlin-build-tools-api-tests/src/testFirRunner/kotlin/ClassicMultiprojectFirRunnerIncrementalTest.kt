/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.buildtools.api.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.ScenarioModule
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.buildtools.api.tests.compilation.util.moduleWithFir
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName


@DisplayName("Multi module IC scenarios for FIR runner")
class ClassicMultiprojectFirRunnerIncrementalTest : BaseCompilationTest() {

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("testAbiChangeInLib_changeMethodSignature")
    @TestMetadata("incrementalMultiproject")
    fun testAbiChangeInLib_changeMethodSignature(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val lib = moduleWithFir("incrementalMultiproject/lib")
            val app = moduleWithFir("incrementalMultiproject/app", listOf(lib))
            val external = moduleWithFir("incrementalMultiproject/external")

            changeMethodSignatureInLib(lib)

            lib.compile {
                assertCompiledSources(setOf(
                    "src/main/kotlin/bar/A.kt",
                    "src/main/kotlin/bar/B.kt",
                    "src/main/kotlin/bar/barUseA.kt",
                ))
            }
            app.compile {
                assertCompiledSources(setOf(
                    "src/main/kotlin/foo/AA.kt",
                    "src/main/kotlin/foo/AAA.kt",
                    "src/main/kotlin/foo/BB.kt",
                    "src/main/kotlin/foo/fooUseA.kt",
                ))
            }
        }
    }

    private fun changeMethodSignatureInLib(lib: ScenarioModule) {
        lib.changeFile("src/main/kotlin/bar/A.kt") {
            it.replace("fun a() {}", "fun a(): Int = 1")
        }
    }
}
