/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.*
import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.Module
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.ProjectSpec
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.*
import org.jetbrains.kotlin.buildtools.api.tests.compilation.util.moduleWithFir
import org.jetbrains.kotlin.buildtools.api.v2.enums.KotlinVersion
import org.junit.jupiter.api.DisplayName
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.assertThrows
import java.util.UUID


@DisplayName("Single module IC scenarios for FIR runner")
class SingleModuleFirRunnerIncrementalTest : BaseCompilationTest() {

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Adding and removing the class")
    @TestMetadata("jvm-module-1")
    fun testScenario1(projectSpec: ProjectSpec) {
        scenario(projectSpec) {
            val module1 = moduleWithFir("jvm-module-1")

            val randomString = UUID.randomUUID().toString()
            module1.createFile(
                "foobar.kt",
                //language=kt
                """
                fun foobar() {
                    println("$randomString")
                }
                """.trimIndent()
            )

            module1.compile { module, scenarioModule ->
                assertCompiledSources(module, "foobar.kt")
                assertAddedOutputs(module, scenarioModule, "FoobarKt.class") // specify only the difference
            }

            module1.deleteFile(
                "foobar.kt",
            )

            module1.compile { module, scenarioModule ->
                assertNoCompiledSources(module)
                assertRemovedOutputs(module, scenarioModule, "FoobarKt.class") // specify only the difference
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Throws an exception on using LV 1.9")
    @TestMetadata("jvm-module-1")
    fun testScenario2(projectSpec: ProjectSpec) {
        scenario(projectSpec) {
            assertThrows<IllegalStateException>(
                message = "Compilation does not fail on LV 1.9"
            ) {
                // Throws on initial compilation
                moduleWithFir(
                    moduleName = "jvm-module-1",
                    overrides = Module.Overrides(languageVersion = KotlinVersion.V1_9),
                )
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Throws an exception on missing -Xuse-fir-ic")
    @TestMetadata("jvm-module-1")
    fun testScenario3(projectSpec: ProjectSpec) {
        scenario(projectSpec) {
            assertThrows<IllegalStateException>(
                message = "Compilation does not fail on missing -Xuse-fir-ic"
            ) {
                // Throws on initial compilation
                module(
                    moduleName = "jvm-module-1",
                    overrides = Module.Overrides(useFirRunner = true),
                )
            }
        }
    }
}
