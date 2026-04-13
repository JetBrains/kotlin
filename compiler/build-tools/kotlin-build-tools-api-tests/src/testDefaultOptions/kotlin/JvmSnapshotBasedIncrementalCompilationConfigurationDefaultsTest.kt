/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.defaults

import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class JvmSnapshotBasedIncrementalCompilationConfigurationDefaultsTest {
    @Test
    fun testDefaultOptions() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        val icConfiguration = kotlinToolchains.jvm.jvmCompilationOperationBuilder(emptyList(), Path("."))
            .snapshotBasedIcConfigurationBuilder(Path("."), SourcesChanges.Unknown, emptyList()).build()
        testDefaults(icConfiguration)
    }

    @Suppress("DEPRECATION")
    private fun testDefaults(icConfiguration: JvmSnapshotBasedIncrementalCompilationConfiguration) {
        assertEquals(null, icConfiguration[JvmSnapshotBasedIncrementalCompilationConfiguration.ROOT_PROJECT_DIR])
        assertEquals(null, icConfiguration[BaseIncrementalCompilationConfiguration.ROOT_PROJECT_DIR])
        assertEquals(null, icConfiguration[JvmSnapshotBasedIncrementalCompilationConfiguration.MODULE_BUILD_DIR])
        assertEquals(null, icConfiguration[BaseIncrementalCompilationConfiguration.MODULE_BUILD_DIR])
        assertEquals(false, icConfiguration[JvmSnapshotBasedIncrementalCompilationConfiguration.PRECISE_JAVA_TRACKING])
        assertEquals(false, icConfiguration[JvmSnapshotBasedIncrementalCompilationConfiguration.BACKUP_CLASSES])
        assertEquals(false, icConfiguration[BaseIncrementalCompilationConfiguration.BACKUP_CLASSES])
        assertEquals(false, icConfiguration[JvmSnapshotBasedIncrementalCompilationConfiguration.KEEP_IC_CACHES_IN_MEMORY])
        assertEquals(false, icConfiguration[BaseIncrementalCompilationConfiguration.KEEP_IC_CACHES_IN_MEMORY])
        assertEquals(false, icConfiguration[JvmSnapshotBasedIncrementalCompilationConfiguration.FORCE_RECOMPILATION])
        assertEquals(false, icConfiguration[BaseIncrementalCompilationConfiguration.FORCE_RECOMPILATION])
        assertEquals(null, icConfiguration[JvmSnapshotBasedIncrementalCompilationConfiguration.OUTPUT_DIRS])
        assertEquals(null, icConfiguration[BaseIncrementalCompilationConfiguration.OUTPUT_DIRS])
        assertEquals(false, icConfiguration[JvmSnapshotBasedIncrementalCompilationConfiguration.ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES])
        @OptIn(ExperimentalCompilerArgument::class)
        assertEquals(false, icConfiguration[JvmSnapshotBasedIncrementalCompilationConfiguration.USE_FIR_RUNNER])
        @OptIn(ExperimentalCompilerArgument::class)
        assertEquals(
            false,
            icConfiguration[BaseIncrementalCompilationConfiguration.UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM]
        )
        @OptIn(ExperimentalCompilerArgument::class)
        assertEquals(
            false,
            icConfiguration[JvmSnapshotBasedIncrementalCompilationConfiguration.UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM]
        )
        @OptIn(ExperimentalCompilerArgument::class)
        assertEquals(
            true,
            icConfiguration[BaseIncrementalCompilationConfiguration.MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION]
        )
        @OptIn(ExperimentalCompilerArgument::class)
        assertEquals(
            true,
            icConfiguration[JvmSnapshotBasedIncrementalCompilationConfiguration.MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION]
        )
        assertEquals(false, icConfiguration[BaseIncrementalCompilationConfiguration.TRACK_CONFIGURATION_INPUTS])
    }
}
