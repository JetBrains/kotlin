/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests

import org.jetbrains.kotlin.buildtools.api.CompilationService
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalCompilationApproachParameters
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.File

class JvmCompilationConfigurationTest {
    private val compilationService = CompilationService.loadImplementation(CompilationServiceInitializationTest::class.java.classLoader)

    @Test
    fun testDefaultNonIncrementalSettings() {
        val config = compilationService.makeJvmCompilationConfiguration()
        assertEquals(emptySet<String>(), config.kotlinScriptFilenameExtensions)
    }

    @Test
    fun testNonIncrementalSettingsModification() {
        val config = compilationService.makeJvmCompilationConfiguration()
            .useKotlinScriptFilenameExtensions(listOf("kt", "kts", "my.kts", "kt"))
        assertEquals(setOf("kt", "kts", "my.kts"), config.kotlinScriptFilenameExtensions)
    }

    @Test
    fun testDefaultClasspathSnapshotSettings() {
        val config = compilationService.makeJvmCompilationConfiguration()
        val icConfig = config.makeClasspathSnapshotBasedIncrementalCompilationConfiguration()
        val parameters = ClasspathSnapshotBasedIncrementalCompilationApproachParameters(emptyList(), File("someFile"))
        config.useIncrementalCompilation(File("someDir"), SourcesChanges.Unknown, parameters, icConfig)
        assertEquals(emptySet<String>(), config.kotlinScriptFilenameExtensions)
        assertNull(icConfig.rootProjectDir)
        assertNull(icConfig.buildDir)
        if (buildToolsVersion < KotlinToolingVersion(2, 0, 0, "Beta2")) {
            // the option had incorrect default value
            assertEquals(emptySet<File>(), icConfig.outputDirs)
        } else {
            assertNull(icConfig.outputDirs)
        }
        assertEquals(false, icConfig.forcedNonIncrementalMode)
        assertEquals(false, icConfig.assuredNoClasspathSnapshotsChanges)
        // implementation-specific defaults
        assertEquals(true, icConfig.preciseJavaTrackingEnabled)
        assertEquals(false, icConfig.preciseCompilationResultsBackupEnabled)
        assertEquals(false, icConfig.incrementalCompilationCachesKeptInMemory)
    }

    @Test
    fun testClasspathSnapshotSettingsModification() {
        val config = compilationService.makeJvmCompilationConfiguration()
        val icConfig = config.makeClasspathSnapshotBasedIncrementalCompilationConfiguration()
            .useOutputDirs(listOf(File("outputDir")))
            .usePreciseJavaTracking(false)
            .usePreciseCompilationResultsBackup(true)
            .keepIncrementalCompilationCachesInMemory(true)
            .setRootProjectDir(File("rootProjectDir"))
            .setBuildDir(File("buildDir"))
            .assureNoClasspathSnapshotsChanges()
            .forceNonIncrementalMode()
        val parameters = ClasspathSnapshotBasedIncrementalCompilationApproachParameters(emptyList(), File("someFile"))
        config.useIncrementalCompilation(File("someDir"), SourcesChanges.Unknown, parameters, icConfig)
        assertEquals(File("rootProjectDir"), icConfig.rootProjectDir)
        assertEquals(File("buildDir"), icConfig.buildDir)
        assertEquals(setOf(File("outputDir")), icConfig.outputDirs)
        assertEquals(true, icConfig.forcedNonIncrementalMode)
        assertEquals(true, icConfig.assuredNoClasspathSnapshotsChanges)
        assertEquals(false, icConfig.preciseJavaTrackingEnabled)
        assertEquals(true, icConfig.preciseCompilationResultsBackupEnabled)
        assertEquals(true, icConfig.incrementalCompilationCachesKeptInMemory)
    }
}