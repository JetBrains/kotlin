/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class JavaSupertypeTrackerTest {
    @TempDir
    lateinit var workingDir: File

    private lateinit var cache: IncrementalJvmCache

    private val base = FqName("test.Base")
    private val derived = FqName("test.Derived")
    private val derivedFromDerived = FqName("test.DerivedFromDerived")
    private val otherBase = FqName("test.OtherBase")

    @BeforeEach
    fun setUp() {
        cache = IncrementalJvmCache(workingDir, IncrementalCompilationContext(), targetOutputDir = null)
    }

    @AfterEach
    fun tearDown() {
        cache.close()
    }

    @Test
    fun reportedJavaHierarchyIsRecordedInJavaMapsOnly() {
        updateCache(derived to listOf(base), derivedFromDerived to listOf(derived), base to emptyList())

        assertEquals(setOf(derived), cache.getJavaSubtypesOf(base).toSet())
        assertEquals(setOf(derivedFromDerived), cache.getJavaSubtypesOf(derived).toSet())
        assertEquals(setOf(derived), cache.getJavaSupertypesOf(derivedFromDerived).toSet())

        assertEquals(emptySet<FqName>(), cache.getSubtypesOf(base).toSet())
        assertEquals(emptySet<FqName>(), cache.getSupertypesOf(derived).toSet())
    }

    @Test
    fun reReportedSupertypesReplacePreviousOnes() {
        updateCache(derived to listOf(base))
        updateCache(derived to listOf(otherBase))

        assertEquals(emptySet<FqName>(), cache.getJavaSubtypesOf(base).toSet())
        assertEquals(setOf(derived), cache.getJavaSubtypesOf(otherBase).toSet())
        assertEquals(setOf(otherBase), cache.getJavaSupertypesOf(derived).toSet())
    }

    private fun updateCache(vararg reports: Pair<FqName, List<FqName>>) {
        val tracker = JavaSupertypeTrackerImpl()
        reports.forEach { [classFqName, supertypeFqNames] -> tracker.report(classFqName, supertypeFqNames) }
        updateIncrementalCache(emptyList(), cache, ChangesCollector(), javaChangesTracker = null, jvmMetadataTracker = null, tracker)
    }
}
