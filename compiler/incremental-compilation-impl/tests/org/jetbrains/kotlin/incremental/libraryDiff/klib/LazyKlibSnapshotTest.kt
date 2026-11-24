/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.build.report.DoNothingBuildReporter
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler
import org.jetbrains.kotlin.incremental.ClasspathSnapshotFiles
import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import org.jetbrains.kotlin.incremental.LookupStorage
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.NonRecoverableCompilationTransaction
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotBuildReporter
import org.jetbrains.kotlin.incremental.snapshots.LazySnapshotLoadingMetrics.OnClasspathDiffComputation
import org.jetbrains.kotlin.incremental.storage.ListExternalizer
import org.jetbrains.kotlin.incremental.storage.saveToFile
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertSame

class LazyKlibSnapshotTest : KlibSnapshotTestCommon() {

    private val firstKlib by lazy { snapshotKlib(File(klibTestDataDir, "KlibSnapshotShrinkerTest/src")) }
    private val secondKlib by lazy { snapshotKlib(File(klibTestDataDir, "KlibEntrySnapshotterTest/testContentHash/src/current")) }

    @Test
    fun testCurrentSnapshotOfAllKlibs() {
        val lazyKlibSnapshot = lazyKlibSnapshot(currentEntrySnapshots = listOf(firstKlib, secondKlib))

        assertEquals(
            (firstKlib.klibFileSnapshots + secondKlib.klibFileSnapshots).associate { it.sourceFilePath to it.contentHash },
            lazyKlibSnapshot.getCurrentKlibSnapshot(OnClasspathDiffComputation).associate { it.sourceFilePath to it.contentHash },
        )
    }

    @Test
    fun testDuplicateSourceFilesAcrossKlibs() {
        val lazyKlibSnapshot = lazyKlibSnapshot(currentEntrySnapshots = listOf(firstKlib, firstKlib))

        assertEquals(
            (firstKlib.klibFileSnapshots + firstKlib.klibFileSnapshots).map { it.sourceFilePath },
            lazyKlibSnapshot.getCurrentKlibSnapshot(OnClasspathDiffComputation).map { it.sourceFilePath },
        )
    }

    @Test
    fun testSavedShrunkSnapshot() {
        val shrunkSnapshot = firstKlib.klibFileSnapshots.filter { it.sourceFilePath == "com/example/Base.kt" }
        val lazyKlibSnapshot = lazyKlibSnapshot(shrunkPreviousSnapshot = shrunkSnapshot)

        assertEquals(
            listOf("com/example/Base.kt"),
            lazyKlibSnapshot.getSavedShrunkKlibSnapshotAgainstPreviousLookups(OnClasspathDiffComputation).map { it.sourceFilePath },
        )
    }

    @Test
    fun testSnapshotsAreLoadedOnce() {
        val currentSnapshotFile = File(tmpDir, "current.bin").also { KlibEntrySnapshotExternalizer.saveToFile(it, firstKlib) }
        val shrunkPreviousSnapshotFile =
            File(tmpDir, "shrunk.bin").also { ListExternalizer(KlibSnapshotExternalizer).saveToFile(it, emptyList()) }
        val lazyKlibSnapshot = LazyKlibSnapshot(listOf(currentSnapshotFile), shrunkPreviousSnapshotFile, reporter)

        val current = lazyKlibSnapshot.getCurrentKlibSnapshot(OnClasspathDiffComputation)
        val savedShrunk = lazyKlibSnapshot.getSavedShrunkKlibSnapshotAgainstPreviousLookups(OnClasspathDiffComputation)
        currentSnapshotFile.delete()
        shrunkPreviousSnapshotFile.delete()

        assertSame(current, lazyKlibSnapshot.getCurrentKlibSnapshot(OnClasspathDiffComputation))
        assertSame(savedShrunk, lazyKlibSnapshot.getSavedShrunkKlibSnapshotAgainstPreviousLookups(OnClasspathDiffComputation))
    }

    @Test
    fun testShrinkAndSaveKlibSnapshot() {
        val currentSnapshotFile = File(tmpDir, "current.bin").also { KlibEntrySnapshotExternalizer.saveToFile(it, firstKlib) }
        val classpathChanges = ToBeComputedByIncrementalCompiler(ClasspathSnapshotFiles(listOf(currentSnapshotFile), tmpDir))
        val shrunkSnapshotFile = classpathChanges.classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile

        val icContext = IncrementalCompilationContext(
            storeFullFqNamesInLookupCache = true,
            transaction = NonRecoverableCompilationTransaction(),
        )
        LookupStorage(File(tmpDir, "lookups"), icContext).use { lookupStorage ->
            val lookups = MultiMap.create<LookupSymbol, String>().apply { putValue(LookupSymbol("Sub", "com.example"), "/src/Usage.kt") }
            lookupStorage.addAll(lookups, setOf("/src/Usage.kt"))

            shrinkAndSaveKlibSnapshot(
                classpathChanges, lookupStorage, LazyKlibSnapshot(listOf(currentSnapshotFile), shrunkSnapshotFile, reporter), reporter
            )
        }

        assertEquals(
            setOf("com/example/Sub.kt", "com/example/Base.kt"),
            LazyKlibSnapshot(emptyList(), shrunkSnapshotFile, reporter)
                .getSavedShrunkKlibSnapshotAgainstPreviousLookups(OnClasspathDiffComputation).mapTo(mutableSetOf()) { it.sourceFilePath },
        )
    }

    private val reporter = ClasspathSnapshotBuildReporter(DoNothingBuildReporter)

    private fun lazyKlibSnapshot(
        currentEntrySnapshots: List<KlibEntrySnapshot> = emptyList(),
        shrunkPreviousSnapshot: List<KlibFileSnapshot> = emptyList(),
    ): LazyKlibSnapshot {
        val currentSnapshotFiles = currentEntrySnapshots.mapIndexed { index, entrySnapshot ->
            File(tmpDir, "current-$index.bin").also { KlibEntrySnapshotExternalizer.saveToFile(it, entrySnapshot) }
        }
        val shrunkPreviousSnapshotFile =
            File(tmpDir, "shrunk-previous.bin").also { ListExternalizer(KlibSnapshotExternalizer).saveToFile(it, shrunkPreviousSnapshot) }

        return LazyKlibSnapshot(currentSnapshotFiles, shrunkPreviousSnapshotFile, reporter)
    }
}
