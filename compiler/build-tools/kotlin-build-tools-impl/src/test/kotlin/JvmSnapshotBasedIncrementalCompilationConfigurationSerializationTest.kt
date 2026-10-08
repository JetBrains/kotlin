/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.serializability.JvmSnapshotBasedIncrementalCompilationConfigurationImplSerializer
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("DEPRECATION_ERROR")
class JvmSnapshotBasedIncrementalCompilationConfigurationSerializationTest {

    private val json = Json { prettyPrint = true }

    @Test
    fun testDefaultValuesRoundtrip() {
        val original = JvmSnapshotBasedIncrementalCompilationConfigurationImpl(
            workingDirectory = Path("/tmp/workDir"),
            sourcesChanges = SourcesChanges.ToBeCalculated,
            dependenciesSnapshotFiles = listOf(Path("/tmp/dep1.snapshot"), Path("/tmp/dep2.snapshot")),
            shrunkClasspathSnapshot = Path("/tmp/shrunk.snapshot"),
        )

        val serialized = json.encodeToString(JvmSnapshotBasedIncrementalCompilationConfigurationImplSerializer, original)
        assertFalse(serialized.contains("options"), "Serialized string must not contain 'options'")

        val deserialized = json.decodeFromString(JvmSnapshotBasedIncrementalCompilationConfigurationImplSerializer, serialized)

        assertEquals(original.workingDirectory, deserialized.workingDirectory)
        assertTrue(deserialized.sourcesChanges is SourcesChanges.ToBeCalculated)
        assertEquals(original.dependenciesSnapshotFiles, deserialized.dependenciesSnapshotFiles)
        assertEquals(original.shrunkClasspathSnapshot, deserialized.shrunkClasspathSnapshot)
        assertEquals(original.preciseJavaTracking, deserialized.preciseJavaTracking)
        assertEquals(original.assuredNoClasspathSnapshotChanges, deserialized.assuredNoClasspathSnapshotChanges)
        assertEquals(original.useFirRunner, deserialized.useFirRunner)
        assertEquals(original.rootProjectDir, deserialized.rootProjectDir)
        assertEquals(original.moduleBuildDir, deserialized.moduleBuildDir)
        assertEquals(original.backupClasses, deserialized.backupClasses)
        assertEquals(original.keepIcCachesInMemory, deserialized.keepIcCachesInMemory)
        assertEquals(original.forceRecompilation, deserialized.forceRecompilation)
        assertEquals(original.outputDirs, deserialized.outputDirs)
        assertEquals(original.unsafeIncrementalCompilationForMultiplatform, deserialized.unsafeIncrementalCompilationForMultiplatform)
        assertEquals(original.monotonousIncrementalCompileSetExpansion, deserialized.monotonousIncrementalCompileSetExpansion)
        assertEquals(original.trackConfigurationInputs, deserialized.trackConfigurationInputs)
    }

    @Test
    fun testCustomValuesRoundtrip() {
        val original = JvmSnapshotBasedIncrementalCompilationConfigurationImpl(
            workingDirectory = Path("/tmp/workDir"),
            sourcesChanges = SourcesChanges.Known(
                modifiedFiles = listOf(File("/tmp/Modified.kt")),
                removedFiles = listOf(File("/tmp/Removed.kt")),
            ),
            dependenciesSnapshotFiles = listOf(Path("/tmp/dep.snapshot")),
            shrunkClasspathSnapshot = Path("/tmp/shrunk.snapshot"),
        ).apply {
            preciseJavaTracking = true
            assuredNoClasspathSnapshotChanges = true
            useFirRunner = true
            rootProjectDir = Path("/tmp/root")
            moduleBuildDir = Path("/tmp/build")
            backupClasses = true
            keepIcCachesInMemory = true
            forceRecompilation = true
            outputDirs = setOf(Path("/tmp/out1"), Path("/tmp/out2"))
            unsafeIncrementalCompilationForMultiplatform = true
            monotonousIncrementalCompileSetExpansion = false
            trackConfigurationInputs = true
        }

        val serialized = json.encodeToString(JvmSnapshotBasedIncrementalCompilationConfigurationImplSerializer, original)
        assertFalse(serialized.contains("options"), "Serialized string must not contain 'options'")

        val deserialized = json.decodeFromString(JvmSnapshotBasedIncrementalCompilationConfigurationImplSerializer, serialized)

        assertEquals(original.workingDirectory, deserialized.workingDirectory)
        val deserializedChanges = deserialized.sourcesChanges as SourcesChanges.Known
        assertEquals(listOf(File("/tmp/Modified.kt").absolutePath), deserializedChanges.modifiedFiles.map { it.absolutePath })
        assertEquals(listOf(File("/tmp/Removed.kt").absolutePath), deserializedChanges.removedFiles.map { it.absolutePath })
        assertEquals(original.dependenciesSnapshotFiles, deserialized.dependenciesSnapshotFiles)
        assertEquals(original.shrunkClasspathSnapshot, deserialized.shrunkClasspathSnapshot)
        assertEquals(true, deserialized.preciseJavaTracking)
        assertEquals(true, deserialized.assuredNoClasspathSnapshotChanges)
        assertEquals(true, deserialized.useFirRunner)
        assertEquals(Path("/tmp/root"), deserialized.rootProjectDir)
        assertEquals(Path("/tmp/build"), deserialized.moduleBuildDir)
        assertEquals(true, deserialized.backupClasses)
        assertEquals(true, deserialized.keepIcCachesInMemory)
        assertEquals(true, deserialized.forceRecompilation)
        assertEquals(setOf(Path("/tmp/out1"), Path("/tmp/out2")), deserialized.outputDirs)
        assertEquals(true, deserialized.unsafeIncrementalCompilationForMultiplatform)
        assertEquals(false, deserialized.monotonousIncrementalCompileSetExpansion)
        assertEquals(true, deserialized.trackConfigurationInputs)
    }

    @Test
    fun testUnknownSourcesChangesRoundtrip() {
        val original = JvmSnapshotBasedIncrementalCompilationConfigurationImpl(
            workingDirectory = Path("/tmp/workDir"),
            sourcesChanges = SourcesChanges.Unknown,
            dependenciesSnapshotFiles = emptyList(),
            shrunkClasspathSnapshot = Path("/tmp/shrunk.snapshot"),
        )

        val serialized = json.encodeToString(JvmSnapshotBasedIncrementalCompilationConfigurationImplSerializer, original)
        val deserialized = json.decodeFromString(JvmSnapshotBasedIncrementalCompilationConfigurationImplSerializer, serialized)
        assertTrue(deserialized.sourcesChanges is SourcesChanges.Unknown)
    }
}
