/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.Companion.INCREMENTAL_COMPILATION
import org.jetbrains.kotlin.buildtools.api.jvm.operations.snapshotBasedIcConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

/**
 * These tests verify snapshot path handling, introduced in KT-75837 and finalized in KT-83937:
 * - The deprecated 4-parameter `snapshotBasedIcConfigurationBuilder` (now a `DeprecationLevel.ERROR`) still accepts
 *   a custom snapshot path, but the path is ignored entirely - the snapshot is always stored as
 *   "shrunk-classpath-snapshot.bin" under workingDirectory
 * - The new 3-parameter version stores the snapshot at that same location
 */
class SnapshotPathSmokeTest : BaseCompilationTest() {
    @DisplayName("Deprecated 4-parameter builder ignores the custom snapshot path and uses workingDirectory")
    @DefaultStrategyAgnosticCompilationTest
    @TestMetadata("basic-multimodule-project/module-1")
    fun testDeprecatedBuilderIgnoresCustomSnapshotPath(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module1 = module("basic-multimodule-project/module-1")
            val customSnapshotDir = module1.buildDirectory.resolve("my-custom-snapshot")
            customSnapshotDir.createDirectories()
            val customSnapshotFile = customSnapshotDir.resolve("my-custom-snapshot-name.bin")

            module1.compile(
                compilationConfigAction = { compilationOperation ->
                    @Suppress("DEPRECATION", "DEPRECATION_ERROR")
                    val icConfig = compilationOperation.snapshotBasedIcConfiguration(
                        workingDirectory = module1.icCachesDir,
                        sourcesChanges = SourcesChanges.Unknown,
                        dependenciesSnapshotFiles = emptyList(),
                        shrunkClasspathSnapshot = customSnapshotFile,
                    ) {}
                    compilationOperation[INCREMENTAL_COMPILATION] =
                        icConfig
                }
            )

            val expectedSnapshotFile = module1.icCachesDir.resolve("shrunk-classpath-snapshot.bin")
            assertTrue(expectedSnapshotFile.exists()) {
                "Expected snapshot file at $expectedSnapshotFile to exist. " +
                        "The snapshot is always stored as 'shrunk-classpath-snapshot.bin' under workingDirectory."
            }
            assertFalse(customSnapshotFile.exists()) {
                "Custom snapshot path $customSnapshotFile should not exist - the configured path is ignored."
            }
        }
    }

    @DisplayName("New 3-parameter builder creates snapshot with auto-generated path under workingDirectory")
    @DefaultStrategyAgnosticCompilationTest
    @TestMetadata("basic-multimodule-project/module-1")
    fun testNewBuilderAutoGeneratesSnapshotPath(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module1 = module("basic-multimodule-project/module-1")
            val icWorkDir = module1.buildDirectory.resolve("ic-work")

            module1.compile(
                compilationConfigAction = { compilationOperation ->
                    val icConfig = compilationOperation.snapshotBasedIcConfiguration(
                        workingDirectory = icWorkDir,
                        sourcesChanges = SourcesChanges.Unknown,
                        dependenciesSnapshotFiles = emptyList(),
                    ) {}
                    compilationOperation[INCREMENTAL_COMPILATION] =
                        icConfig
                }
            )

            val expectedSnapshotFile = icWorkDir.resolve("shrunk-classpath-snapshot.bin")
            assertTrue(expectedSnapshotFile.exists()) {
                "Expected snapshot file at $expectedSnapshotFile to exist under workingDirectory."
            }
        }
    }
}
