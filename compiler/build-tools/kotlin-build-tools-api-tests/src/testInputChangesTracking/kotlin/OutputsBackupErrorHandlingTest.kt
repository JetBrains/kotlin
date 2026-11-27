/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration.Companion.BACKUP_CLASSES
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration.Companion.KEEP_IC_CACHES_IN_MEMORY
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ModuleContext
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.*
import org.jetbrains.kotlin.buildtools.tests.compilation.util.execute
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.exists

class OutputsBackupErrorHandlingTest : BaseCompilationTest() {

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Externally tracked: with backup options, outputs are preserved after compilation error")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testExternallyTrackedWithBackupOptionsOutputsPreserved(kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy) {
        testWithBackupOptionsOutputsPreserved(kotlinToolchains, executionPolicy) {
            module("basic-multimodule-project/module-1", icOptionsConfigAction = { it[BACKUP_CLASSES] = true; it[KEEP_IC_CACHES_IN_MEMORY] = true })
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Externally tracked: without backup options, outputs are lost after compilation error")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testExternallyTrackedWithoutBackupOptionsOutputsLost(kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy) {
        testWithoutBackupOptionsOutputsLost(kotlinToolchains, executionPolicy) { module("basic-multimodule-project/module-1") }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Internally tracked: with backup options, outputs are preserved after compilation error")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testInternallyTrackedWithBackupOptionsOutputsPreserved(kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy) {
        testWithBackupOptionsOutputsPreserved(kotlinToolchains, executionPolicy) {
            trackedModule("basic-multimodule-project/module-1", icOptionsConfigAction = { it[BACKUP_CLASSES] = true; it[KEEP_IC_CACHES_IN_MEMORY] = true })
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Internally tracked: without backup options, outputs are lost after compilation error")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testInternallyTrackedWithoutBackupOptionsOutputsLost(kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy) {
        testWithoutBackupOptionsOutputsLost(kotlinToolchains, executionPolicy) { trackedModule("basic-multimodule-project/module-1") }
    }

    private fun testWithBackupOptionsOutputsPreserved(
        kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy,
        createModule: Scenario<*, *>.() -> ScenarioModule,
    ) {
        jvmScenario(kotlinToolchains, executionPolicy) {
            val module = createModule()

            module.createFile("extra.kt", "fun extra() = foo()")
            module.compile {
                assertCompiledSources("extra.kt")
                assertAddedOutputs("ExtraKt.class")
            }

            module.changeFile("extra.kt") { "fun extra() = doesNotExist()" }
            module.compile {
                expectFail()
                assertNoOutputSetChanges()
                assertOutputFileExists("ExtraKt.class")
                assertOutputFileExists("META-INF/basic-multimodule-project_module-1.kotlin_module")
            }
        }
    }

    private fun testWithoutBackupOptionsOutputsLost(
        kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy,
        createModule: Scenario<*, *>.() -> ScenarioModule,
    ) {
        jvmScenario(kotlinToolchains, executionPolicy) {
            val module = createModule()

            module.createFile("extra.kt", "fun extra() = foo()")
            module.compile {
                assertCompiledSources("extra.kt")
                assertAddedOutputs("ExtraKt.class")
            }

            module.changeFile("extra.kt") { "fun extra() = doesNotExist()" }
            module.compile {
                expectFail()
                assertOutputFileDoesNotExist("ExtraKt.class")
                assertOutputFileDoesNotExist("META-INF/basic-multimodule-project/module-1.kotlin_module")
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Externally tracked: BACKUP_CLASSES only, outputs preserved but caches invalid - recompiles on revert")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testExternallyTrackedBackupClassesOnlyOutputsPreservedButCachesInvalid(kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy) {
        testBackupClassesOnlyOutputsPreservedButCachesInvalid(kotlinToolchains, executionPolicy) {
            module("basic-multimodule-project/module-1", icOptionsConfigAction = { it[BACKUP_CLASSES] = true })
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Internally tracked: BACKUP_CLASSES only, outputs preserved but caches invalid - recompiles on revert")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testInternallyTrackedBackupClassesOnlyOutputsPreservedButCachesInvalid(kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy) {
        testBackupClassesOnlyOutputsPreservedButCachesInvalid(kotlinToolchains, executionPolicy) {
            trackedModule("basic-multimodule-project/module-1", icOptionsConfigAction = { it[BACKUP_CLASSES] = true })
        }
    }

    private fun testBackupClassesOnlyOutputsPreservedButCachesInvalid(
        kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy,
        createModule: Scenario<*, *>.() -> ScenarioModule,
    ) {
        jvmScenario(kotlinToolchains, executionPolicy) {
            val module = createModule()

            module.createFile("extra.kt", "fun extra() = foo()")
            module.compile {
                assertCompiledSources("extra.kt")
                assertAddedOutputs("ExtraKt.class")
            }

            module.changeFile("extra.kt") { "fun extra() = doesNotExist()" }
            module.compile {
                expectFail()
                // Outputs are restored by BACKUP_CLASSES
                assertOutputFileExists("ExtraKt.class")
            }

            // Without KEEP_IC_CACHES_IN_MEMORY, the IC caches were written to disk during the failed build and not rolled back.
            // The caches are now inconsistent with the restored outputs, causing compilation to fail even though the source code is valid.
            module.changeFile("extra.kt") { "fun extra() = foo()" }
            module.compile {
                expectFail()
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Externally tracked: with backup, compiled code is correct after error and fix")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testExternallyTrackedCorrectnessAfterRecovery(kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy) {
        testCorrectnessAfterRecovery(kotlinToolchains, executionPolicy) {
            module("basic-multimodule-project/module-1", icOptionsConfigAction = { it[BACKUP_CLASSES] = true; it[KEEP_IC_CACHES_IN_MEMORY] = true })
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Internally tracked: with backup, compiled code is correct after error and fix")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testInternallyTrackedCorrectnessAfterRecovery(kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy) {
        testCorrectnessAfterRecovery(kotlinToolchains, executionPolicy) {
            trackedModule("basic-multimodule-project/module-1", icOptionsConfigAction = { it[BACKUP_CLASSES] = true; it[KEEP_IC_CACHES_IN_MEMORY] = true })
        }
    }

    private fun testCorrectnessAfterRecovery(
        kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy,
        createModule: Scenario<*, *>.() -> ScenarioModule,
    ) {
        jvmScenario(kotlinToolchains, executionPolicy) {
            val module = createModule()

            module.createFile("extra.kt", "fun main() { println(\"v1\") }")
            module.compile {
                assertCompiledSources("extra.kt")
                assertAddedOutputs("ExtraKt.class")
            }
            module.execute(mainClass = "ExtraKt", exactOutput = "v1")

            module.changeFile("extra.kt") { "fun main() { doesNotExist() }" }
            module.compile { expectFail() }

            module.changeFile("extra.kt") { "fun main() { println(\"v2\") }" }
            module.compile {
                assertCompiledSources("extra.kt")
                assertNoOutputSetChanges()
            }
            module.execute(mainClass = "ExtraKt", exactOutput = "v2")
        }
    }

    companion object {
        context(module: ModuleContext)
        fun assertOutputFileExists(relativePath: String) {
            assertTrue(module.outputDirectory.resolve(relativePath).exists()) {
                "$relativePath should exist in output directory after failed compilation with backup"
            }
        }

        context(module: ModuleContext)
        fun assertOutputFileDoesNotExist(relativePath: String) {
            assertFalse(module.outputDirectory.resolve(relativePath).exists()) {
                "$relativePath should not exist in output directory after failed compilation without backup"
            }
        }
    }
}
