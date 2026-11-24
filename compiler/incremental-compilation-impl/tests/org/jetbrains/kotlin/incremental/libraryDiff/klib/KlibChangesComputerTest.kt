/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import org.jetbrains.kotlin.build.report.DoNothingBuildReporter
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity.CLASS_LEVEL
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity.CLASS_MEMBER_LEVEL
import org.jetbrains.kotlin.incremental.ChangesEither
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotBuildReporter
import org.jetbrains.kotlin.incremental.classpathDiff.ProgramSymbolSet
import org.jetbrains.kotlin.incremental.classpathDiff.toChangesEither
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.fail

/**
 * The testData sources are copied from `KotlinOnlyClasspathChangesComputerTest`. Differences from its expectations:
 *   - No `<SAM-CONSTRUCTOR>` lookups: on the JVM they come from `DirtyData`, which the klib diff does not use.
 *   - Coarse-grained snapshots cover a whole source file, so all classes and top-level members of a changed file are reported
 *     (e.g. `Outer` together with `Outer.Companion`).
 */
class KlibChangesComputerTest : KlibSnapshotTestCommon() {

    private val testDataDir = File(klibTestDataDir, "KlibChangesComputerTest")

    @Test
    fun testAbiVersusNonAbiChanges() {
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "publicPropertyChangedType", scope = "com.example.SomeClass"),
                LookupSymbol(name = "publicFunctionChangedSignature", scope = "com.example.SomeClass"),
            ),
            fqNames = setOf("com.example.SomeClass")
        ).assertEquals(computeChanges("testAbiVersusNonAbiChanges"))
    }

    @Test
    fun testModifiedAddedRemovedElements() {
        Changes(
            lookupSymbols = setOf(
                // ModifiedClassUnchangedMembers
                LookupSymbol(name = "ModifiedClassUnchangedMembers", scope = "com.example"),

                // ModifiedClassChangedMembers
                LookupSymbol(name = "modifiedProperty", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "addedProperty", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "removedProperty", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "modifiedFunction", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "addedFunction", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "removedFunction", scope = "com.example.ModifiedClassChangedMembers"),

                // AddedClass
                LookupSymbol(name = "AddedClass", scope = "com.example"),

                // RemovedClass
                LookupSymbol(name = "RemovedClass", scope = "com.example"),
            ),
            fqNames = setOf(
                "com.example.ModifiedClassUnchangedMembers",
                "com.example.ModifiedClassChangedMembers",
                "com.example.AddedClass",
                "com.example.RemovedClass",
            )
        ).assertEquals(computeChanges("testModifiedAddedRemovedElements"))
    }

    @Test
    fun testModifiedAddedRemovedElements_ClassLevelSnapshot() {
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "ModifiedClassUnchangedMembers", scope = "com.example"),
                LookupSymbol(name = "ModifiedClassChangedMembers", scope = "com.example"),
                LookupSymbol(name = "AddedClass", scope = "com.example"),
                LookupSymbol(name = "RemovedClass", scope = "com.example"),
            ),
            fqNames = setOf(
                "com.example.ModifiedClassUnchangedMembers",
                "com.example.ModifiedClassChangedMembers",
                "com.example.AddedClass",
                "com.example.RemovedClass",
            )
        ).assertEquals(computeChanges("testModifiedAddedRemovedElements", CLASS_LEVEL))
    }

    @Test
    fun testMixedGranularities() {
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "CoarseGrainedFirstBuild_CoarseGrainedSecondBuild_Class", scope = "com.example"),
                LookupSymbol(name = "CoarseGrainedFirstBuild_FineGrainedSecondBuild_Class", scope = "com.example"),
                LookupSymbol(name = "FineGrainedFirstBuild_CoarseGrainedSecondBuild_Class", scope = "com.example"),
                LookupSymbol(name = "modifiedProperty", scope = "com.example.FineGrainedFirstBuild_FineGrainedSecondBuild_Class"),
                LookupSymbol(name = "modifiedFunction", scope = "com.example.FineGrainedFirstBuild_FineGrainedSecondBuild_Class"),
            ),
            fqNames = setOf(
                "com.example.CoarseGrainedFirstBuild_CoarseGrainedSecondBuild_Class",
                "com.example.CoarseGrainedFirstBuild_FineGrainedSecondBuild_Class",
                "com.example.FineGrainedFirstBuild_CoarseGrainedSecondBuild_Class",
                "com.example.FineGrainedFirstBuild_FineGrainedSecondBuild_Class",
            )
        ).assertEquals(computeChanges(snapshotWithMixedGranularities("current"), snapshotWithMixedGranularities("previous")))
    }

    @Test
    fun testTopLevelMembers() {
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "modifiedTopLevelProperty", scope = "com.example"),
                LookupSymbol(name = "addedTopLevelProperty", scope = "com.example"),
                LookupSymbol(name = "removedTopLevelProperty", scope = "com.example"),
                LookupSymbol(name = "movedTopLevelProperty", scope = "com.example"),
                LookupSymbol(name = "modifiedTopLevelFunction", scope = "com.example"),
                LookupSymbol(name = "addedTopLevelFunction", scope = "com.example"),
                LookupSymbol(name = "removedTopLevelFunction", scope = "com.example"),
                LookupSymbol(name = "movedTopLevelFunction", scope = "com.example"),
            ),
            fqNames = setOf("com.example")
        ).assertEquals(computeChanges("testTopLevelMembers"))
    }

    /** A change of a class member must not be reported as a change of the top-level members of the same file, and vice versa. */
    @Test
    fun testClassAndTopLevelMembersInOneFile() {
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "changedMember", scope = "com.example.ChangedClass"),
                LookupSymbol(name = "changedTopLevelProperty", scope = "com.example"),
            ),
            fqNames = setOf("com.example.ChangedClass", "com.example")
        ).assertEquals(computeChanges("testClassAndTopLevelMembersInOneFile"))
    }

    @Test
    fun testClassAndTopLevelMembersInOneFile_ClassLevelSnapshot() {
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "ChangedClass", scope = "com.example"),
                LookupSymbol(name = "UnchangedClass", scope = "com.example"),
                LookupSymbol(name = "changedTopLevelProperty", scope = "com.example"),
                LookupSymbol(name = "unchangedTopLevelProperty", scope = "com.example"),
            ),
            fqNames = setOf("com.example.ChangedClass", "com.example.UnchangedClass", "com.example")
        ).assertEquals(computeChanges("testClassAndTopLevelMembersInOneFile", CLASS_LEVEL))
    }

    /** Same-named files of one package in two klibs are matched by content hash, not by package and source file alone. */
    @Test
    fun testDuplicateSourceFiles() {
        val previous = snapshot("testTopLevelMembers", "previous", CLASS_MEMBER_LEVEL)
        val current = snapshot("testTopLevelMembers", "current", CLASS_MEMBER_LEVEL)
        val unchangedKlib = snapshot("testTopLevelMembers", "previous", CLASS_MEMBER_LEVEL)

        Changes(lookupSymbols = emptySet(), fqNames = emptySet())
            .assertEquals(computeChanges(current = unchangedKlib + previous, previous = previous + unchangedKlib))
        computeChanges("testTopLevelMembers")
            .assertEquals(computeChanges(current = unchangedKlib + current, previous = previous + unchangedKlib))
    }

    /** Like the compiler, the first klib's copy of a duplicate class is compared, not the copies of later klibs. */
    @Test
    fun testDuplicateClasses() {
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "changedFunction", scope = "com.example.DuplicateClass"),
                LookupSymbol(name = "otherKlibFunction", scope = "com.example"),
            ),
            fqNames = setOf("com.example.DuplicateClass", "com.example"),
        ).assertEquals(
            computeChanges(
                current = snapshot("testDuplicateClasses", "current", CLASS_MEMBER_LEVEL) +
                        snapshot("testDuplicateClasses", "otherKlibCurrent", CLASS_MEMBER_LEVEL),
                previous = snapshot("testDuplicateClasses", "previous", CLASS_MEMBER_LEVEL) +
                        snapshot("testDuplicateClasses", "otherKlibPrevious", CLASS_MEMBER_LEVEL),
            )
        )
    }

    @Test
    fun testImpactComputation_SupertypesInheritors() {
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "changedProperty", scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = "changedProperty", scope = "com.example.SubClassOfChangedSuperClass"),
                LookupSymbol(name = "changedProperty", scope = "com.example.SubSubClassOfChangedSuperClass"),
                LookupSymbol(name = "changedFunction", scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = "changedFunction", scope = "com.example.SubClassOfChangedSuperClass"),
                LookupSymbol(name = "changedFunction", scope = "com.example.SubSubClassOfChangedSuperClass"),
            ),
            fqNames = setOf(
                "com.example.ChangedSuperClass",
                "com.example.SubClassOfChangedSuperClass",
                "com.example.SubSubClassOfChangedSuperClass",
            )
        ).assertEquals(computeChanges("testImpactComputation_SupertypesInheritors"))
    }

    @Test
    fun testGranularityTransition_CoarseToFineToFine() {
        val build1 = snapshot("testGranularityTransition", "previous", CLASS_LEVEL)
        val build2 = snapshot("testGranularityTransition", "current", CLASS_MEMBER_LEVEL)
        coarseGrainedOuterChanges.assertEquals(computeChanges(build2, build1))

        val build3 = snapshot("testGranularityTransition", "previous", CLASS_MEMBER_LEVEL)
        Changes(
            lookupSymbols = setOf(LookupSymbol(name = "companionFunction", scope = "com.example.Outer.Companion")),
            fqNames = setOf("com.example.Outer.Companion")
        ).assertEquals(computeChanges(build3, build2))
    }

    @Test
    fun testGranularityTransition_FineToCoarseToCoarseToFine() {
        val build1 = snapshot("testGranularityTransition", "previous", CLASS_MEMBER_LEVEL)
        val build2 = snapshot("testGranularityTransition", "current", CLASS_LEVEL)
        coarseGrainedOuterChanges.assertEquals(computeChanges(build2, build1))

        val build3 = snapshot("testGranularityTransition", "previous", CLASS_LEVEL)
        coarseGrainedOuterChanges.assertEquals(computeChanges(build3, build2))

        val build4 = snapshot("testGranularityTransition", "current", CLASS_MEMBER_LEVEL)
        coarseGrainedOuterChanges.assertEquals(computeChanges(build4, build3))
    }

    private val coarseGrainedOuterChanges = Changes(
        lookupSymbols = setOf(
            LookupSymbol(name = "Outer", scope = "com.example"),
            LookupSymbol(name = "Companion", scope = "com.example.Outer"),
        ),
        fqNames = setOf("com.example.Outer", "com.example.Outer.Companion")
    )

    private fun snapshot(testName: String, version: String, granularity: ClassSnapshotGranularity): List<KlibFileSnapshot> =
        snapshotKlib(File(testDataDir, "$testName/src/$version"), granularity).klibFileSnapshots

    private fun computeChanges(testName: String, granularity: ClassSnapshotGranularity = CLASS_MEMBER_LEVEL): Changes =
        computeChanges(snapshot(testName, "current", granularity), snapshot(testName, "previous", granularity))

    /**
     * Granularity is set per klib, so take each source file's snapshot from the klib snapshotted with the granularity encoded in the
     * file name (`<previous build granularity>FirstBuild_<current build granularity>SecondBuild_Class`).
     */
    private fun snapshotWithMixedGranularities(version: String): List<KlibFileSnapshot> {
        val coarse = snapshot("testMixedClassSnapshotGranularities", version, CLASS_LEVEL).associateBy { it.sourceFilePath }
        val fineMarker = if (version == "previous") "FineGrainedFirstBuild" else "FineGrainedSecondBuild"
        return snapshot("testMixedClassSnapshotGranularities", version, CLASS_MEMBER_LEVEL)
            .map { if (fineMarker in it.sourceFilePath) it else coarse.getValue(it.sourceFilePath) }
    }

    private fun computeChanges(current: List<KlibFileSnapshot>, previous: List<KlibFileSnapshot>): Changes =
        KlibChangesComputer.computeChangedAndImpactedSet(current, previous, ClasspathSnapshotBuildReporter(DoNothingBuildReporter))
            .toChanges()
}

private data class Changes(val lookupSymbols: Set<LookupSymbol>, val fqNames: Set<String>)

private fun ProgramSymbolSet.toChanges(): Changes {
    val changes: ChangesEither.Known = toChangesEither()
    return Changes(changes.lookupSymbols.toSet(), changes.fqNames.map { it.asString() }.toSet())
}

private fun Changes.assertEquals(actual: Changes) {
    listOfNotNull(
        compare(expected = this.lookupSymbols, actual = actual.lookupSymbols),
        compare(expected = this.fqNames, actual = actual.fqNames)
    ).also {
        if (it.isNotEmpty()) {
            fail(it.joinToString("\n"))
        }
    }
}

private fun compare(expected: Set<*>, actual: Set<*>): String? {
    return if (expected != actual) {
        "Two sets differ:\n" +
                "Elements in expected set but not in actual set: ${expected - actual}\n" +
                "Elements in actual set but not in expected set: ${actual - expected}"
    } else null
}
