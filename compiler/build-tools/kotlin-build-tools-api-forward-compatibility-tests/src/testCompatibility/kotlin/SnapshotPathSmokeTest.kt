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
import org.jetbrains.kotlin.buildtools.tests.compilation.model.project
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

/**
 * These tests verify the behavior documented in KT-75837:
 * - The deprecated 4-parameter `snapshotBasedIcConfigurationBuilder` accepts a custom snapshot path,
 *   but internally only the parent directory is used - the filename is always "shrunk-classpath-snapshot.bin"
 * - The new 3-parameter version auto-generates the path under workingDirectory
 */
class SnapshotPathSmokeTest : BaseCompilationTest() {
    @DisplayName("New 3-parameter builder creates snapshot with auto-generated path under workingDirectory")
    @DefaultStrategyAgnosticCompilationTest
    @TestMetadata("jvm-module-1")
    fun testNewBuilderAutoGeneratesSnapshotPath(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module1 = module("jvm-module-1")
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
