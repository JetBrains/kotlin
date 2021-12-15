/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.incremental.ChangesEither
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.ClassFileUtil.snapshot
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.CompileUtil.compileAll
import org.jetbrains.kotlin.resolve.sam.SAM_LOOKUP_NAME
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import kotlin.test.fail

abstract class ClasspathChangesComputerTest : ClasspathSnapshotTestCommon() {

    // TODO Add more test cases:
    //   - inline functions

    companion object {
        val testDataDir =
            File("compiler/incremental-compilation-impl/testData/org/jetbrains/kotlin/incremental/classpathDiff/ClasspathChangesComputerTest")
    }

    @Test
    abstract fun testAbiVersusNonAbiChanges()

    @Test
    abstract fun testModifiedAddedRemovedElements()

    @Test
    abstract fun testImpactAnalysis()
}

class KotlinOnlyClasspathChangesComputerTest : ClasspathChangesComputerTest() {

    @Test
    override fun testAbiVersusNonAbiChanges() {
        val changes = computeClasspathChanges(File(testDataDir, "testAbiVersusNonAbiChanges/src/kotlin"), tmpDir)
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "publicPropertyChangedType", scope = "com.example.SomeClass"),
                LookupSymbol(name = "publicFunctionChangedSignature", scope = "com.example.SomeClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SomeClass"),
            ),
            fqNames = setOf("com.example.SomeClass")
        ).assertEquals(changes)
    }

    @Test
    override fun testModifiedAddedRemovedElements() {
        val changes = computeClasspathChanges(File(testDataDir, "testModifiedAddedRemovedElements/src/kotlin"), tmpDir)
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
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.ModifiedClassChangedMembers"),

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
        ).assertEquals(changes)
    }

    @Test
    override fun testImpactAnalysis() {
        val changes = computeClasspathChanges(File(testDataDir, "testImpactAnalysis_KotlinOnly/src"), tmpDir)
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "changedProperty", scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = "changedProperty", scope = "com.example.SubClassOfChangedSuperClass"),
                LookupSymbol(name = "changedProperty", scope = "com.example.SubSubClassOfChangedSuperClass"),
                LookupSymbol(name = "changedFunction", scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = "changedFunction", scope = "com.example.SubClassOfChangedSuperClass"),
                LookupSymbol(name = "changedFunction", scope = "com.example.SubSubClassOfChangedSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SubClassOfChangedSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SubSubClassOfChangedSuperClass")
            ),
            fqNames = setOf(
                "com.example.ChangedSuperClass",
                "com.example.SubClassOfChangedSuperClass",
                "com.example.SubSubClassOfChangedSuperClass"
            )
        ).assertEquals(changes)
    }

    @Test
    fun testTopLevelMembers() {
        val changes = computeClasspathChanges(File(testDataDir, "testTopLevelMembers_KotlinOnly/src"), tmpDir)
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
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example")
            ),
            fqNames = setOf("com.example")
        ).assertEquals(changes)
    }
}

@RunWith(Parameterized::class)
class JavaOnlyClasspathChangesComputerTest(private val protoBased: Boolean) : ClasspathChangesComputerTest() {

    companion object {
        @Parameterized.Parameters(name = "protoBased={0}")
        @JvmStatic
        fun parameters() = listOf(true, false)
    }

    @Test
    override fun testAbiVersusNonAbiChanges() {
        val changes = computeClasspathChanges(File(testDataDir, "testAbiVersusNonAbiChanges/src/java"), tmpDir, protoBased)
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "publicFieldChangedType", scope = "com.example.SomeClass"),
                LookupSymbol(name = "publicMethodChangedSignature", scope = "com.example.SomeClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SomeClass")
            ),
            fqNames = setOf("com.example.SomeClass")
        ).assertEquals(changes)
    }

    @Test
    override fun testModifiedAddedRemovedElements() {
        val changes = computeClasspathChanges(File(testDataDir, "testModifiedAddedRemovedElements/src/java"), tmpDir, protoBased)
        Changes(
            lookupSymbols = setOf(
                // ModifiedClassUnchangedMembers
                LookupSymbol(name = "ModifiedClassUnchangedMembers", scope = "com.example"),

                // ModifiedClassChangedMembers
                LookupSymbol(name = "modifiedField", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "addedField", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "removedField", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "modifiedMethod", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "addedMethod", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "removedMethod", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.ModifiedClassChangedMembers"),

                // AddedClass
                LookupSymbol(name = "AddedClass", scope = "com.example"),

                // RemovedClass
                LookupSymbol(name = "RemovedClass", scope = "com.example")
            ),
            fqNames = setOf(
                "com.example.ModifiedClassUnchangedMembers",
                "com.example.ModifiedClassChangedMembers",
                "com.example.AddedClass",
                "com.example.RemovedClass"
            )
        ).assertEquals(changes)
    }

    @Test
    override fun testImpactAnalysis() {
        val changes = computeClasspathChanges(File(testDataDir, "testImpactAnalysis_JavaOnly/src"), tmpDir, protoBased)
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "changedField", scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = "changedField", scope = "com.example.SubClassOfChangedSuperClass"),
                LookupSymbol(name = "changedField", scope = "com.example.SubSubClassOfChangedSuperClass"),
                LookupSymbol(name = "changedMethod", scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = "changedMethod", scope = "com.example.SubClassOfChangedSuperClass"),
                LookupSymbol(name = "changedMethod", scope = "com.example.SubSubClassOfChangedSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SubClassOfChangedSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SubSubClassOfChangedSuperClass")
            ),
            fqNames = setOf(
                "com.example.ChangedSuperClass",
                "com.example.SubClassOfChangedSuperClass",
                "com.example.SubSubClassOfChangedSuperClass"
            )
        ).assertEquals(changes)
    }
}

class KotlinAndJavaClasspathChangesComputerTest : ClasspathSnapshotTestCommon() {

    @Test
    fun testImpactAnalysis() {
        val changes =
            computeClasspathChanges(File(ClasspathChangesComputerTest.testDataDir, "testImpactAnalysis_KotlinAndJava/src"), tmpDir)
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "changedProperty", scope = "com.example.ChangedKotlinSuperClass"),
                LookupSymbol(name = "changedProperty", scope = "com.example.KotlinSubClassOfChangedKotlinSuperClass"),
                LookupSymbol(name = "changedProperty", scope = "com.example.JavaSubClassOfChangedKotlinSuperClass"),
                LookupSymbol(name = "changedFunction", scope = "com.example.ChangedKotlinSuperClass"),
                LookupSymbol(name = "changedFunction", scope = "com.example.KotlinSubClassOfChangedKotlinSuperClass"),
                LookupSymbol(name = "changedFunction", scope = "com.example.JavaSubClassOfChangedKotlinSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.ChangedKotlinSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.KotlinSubClassOfChangedKotlinSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.JavaSubClassOfChangedKotlinSuperClass"),
                LookupSymbol(name = "changedField", scope = "com.example.ChangedJavaSuperClass"),
                LookupSymbol(name = "changedField", scope = "com.example.KotlinSubClassOfChangedJavaSuperClass"),
                LookupSymbol(name = "changedField", scope = "com.example.JavaSubClassOfChangedJavaSuperClass"),
                LookupSymbol(name = "changedMethod", scope = "com.example.ChangedJavaSuperClass"),
                LookupSymbol(name = "changedMethod", scope = "com.example.KotlinSubClassOfChangedJavaSuperClass"),
                LookupSymbol(name = "changedMethod", scope = "com.example.JavaSubClassOfChangedJavaSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.ChangedJavaSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.KotlinSubClassOfChangedJavaSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.JavaSubClassOfChangedJavaSuperClass")
            ),
            fqNames = setOf(
                "com.example.ChangedKotlinSuperClass",
                "com.example.KotlinSubClassOfChangedKotlinSuperClass",
                "com.example.JavaSubClassOfChangedKotlinSuperClass",
                "com.example.ChangedJavaSuperClass",
                "com.example.KotlinSubClassOfChangedJavaSuperClass",
                "com.example.JavaSubClassOfChangedJavaSuperClass"
            )
        ).assertEquals(changes)
    }
}

private fun computeClasspathChanges(classpathSourceDir: File, tmpDir: TemporaryFolder, protoBased: Boolean = false): Changes {
    val currentSnapshot = snapshotClasspath(File(classpathSourceDir, "current-classpath"), tmpDir, protoBased)
    val previousSnapshot = snapshotClasspath(File(classpathSourceDir, "previous-classpath"), tmpDir, protoBased)
    return computeClasspathChanges(currentSnapshot, previousSnapshot)
}

private fun snapshotClasspath(classpathSourceDir: File, tmpDir: TemporaryFolder, protoBased: Boolean): ClasspathSnapshot {
    val classpath = mutableListOf<File>()
    val classpathEntrySnapshots = classpathSourceDir.listFiles()!!.sortedBy { it.name }.map { classpathEntrySourceDir ->
        val classFiles = compileAll(classpathEntrySourceDir, classpath, tmpDir)
        classpath.addAll(listOfNotNull(classFiles.firstOrNull()?.classRoot))

        val relativePaths = classFiles.map { it.unixStyleRelativePath }
        val classSnapshots = classFiles.snapshot(protoBased).map { it.withHash }
        ClasspathEntrySnapshot(
            classSnapshots = relativePaths.zip(classSnapshots).toMap(LinkedHashMap())
        )
    }
    return ClasspathSnapshot(classpathEntrySnapshots)
}

private fun computeClasspathChanges(
    currentClasspathSnapshot: ClasspathSnapshot,
    previousClasspathSnapshot: ClasspathSnapshot
): Changes {
    return ClasspathChangesComputer.computeChangedAndImpactedSet(
        currentClasspathSnapshot.removeDuplicateAndInaccessibleClasses(),
        previousClasspathSnapshot.removeDuplicateAndInaccessibleClasses(),
        DoNothingBuildMetricsReporter
    ).normalize()
}

/** Adapted version of [ChangesEither.Known] for readability in this test. */
private data class Changes(val lookupSymbols: Set<LookupSymbol>, val fqNames: Set<String>)

private fun ChangeSet.normalize(): Changes {
    val changes: ChangesEither.Known = getChanges()
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
