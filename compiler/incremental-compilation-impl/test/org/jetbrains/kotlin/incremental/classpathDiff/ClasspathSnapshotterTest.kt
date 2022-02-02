/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.ClassFileUtil.readBytes
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.SourceFile.JavaSourceFile
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.SourceFile.KotlinSourceFile
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.TestSourceFile
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File

private val testDataDir =
    File("compiler/incremental-compilation-impl/testData/org/jetbrains/kotlin/incremental/classpathDiff/ClasspathSnapshotterTest")

class KotlinOnlyClasspathSnapshotterTest : ClasspathSnapshotTestCommon() {

    @Suppress("SameParameterValue")
    private fun getSourceFile(testName: String, relativePath: String) = TestSourceFile(
        KotlinSourceFile(
            baseDir = File("$testDataDir/kotlin/$testName/src"), relativePath = relativePath,
            preCompiledClassFile = ClassFile(File("$testDataDir/kotlin/$testName/classes"), relativePath.replace(".kt", ".class"))
        ), tmpDir
    )

    @Test
    fun testSimpleClass() {
        val sourceFile = getSourceFile("testSimpleClass", "com/example/SimpleClass.kt")
        val actualSnapshot = sourceFile.compileAndSnapshot().toGson()
        val expectedSnapshot = sourceFile.getExpectedSnapshotFile().readText()

        assertEquals(expectedSnapshot, actualSnapshot)

        // Check that the snapshot contains ABI info
        actualSnapshot.assertContains("publicProperty", "publicFunction")

        // Private properties and functions' names/signatures are currently part of the snapshot. We will fix this later.
        actualSnapshot.assertContains("privateProperty", "privateFunction")

        // Check that the snapshot does not contain non-ABI info
        actualSnapshot.assertDoesNotContain(
            "publicProperty's value",
            "privateProperty's value",
            "publicFunction's body",
            "privateFunction's body"
        )
    }

    @Test
    fun testPackageFacadeClasses() {
        val classpathSnapshot = snapshotClasspath(File("$testDataDir/kotlin/testPackageFacadeClasses/src"), tmpDir)
        val classSnapshots = classpathSnapshot.classpathEntrySnapshots.single().classSnapshots
        val fileFacadeSnapshot = classSnapshots["com/example/FileFacadeKt.class"]!!.toGson()
        val multifileClassSnapshot = classSnapshots["com/example/MultifileClass.class"]!!.toGson()
        val multifileClassPart1Snapshot = classSnapshots["com/example/MultifileClass__MultifileClass1Kt.class"]!!.toGson()
        val multifileClassPart2Snapshot = classSnapshots["com/example/MultifileClass__MultifileClass2Kt.class"]!!.toGson()

        // Check that the snapshots contain ABI info
        fileFacadeSnapshot.assertContains("propertyInFileFacade", "functionInFileFacade")
        multifileClassPart1Snapshot.assertContains("propertyInMultifileClass1", "functionInMultifileClass1")
        multifileClassPart2Snapshot.assertContains("propertyInMultifileClass2", "functionInMultifileClass2")

        // Check that the snapshots do not contain non-ABI info
        fileFacadeSnapshot.assertDoesNotContain("propertyInFileFacade's value", "functionInFileFacade's body")
        multifileClassPart1Snapshot.assertDoesNotContain("propertyInMultifileClass1's value", "functionInMultifileClass1's body")
        multifileClassPart2Snapshot.assertDoesNotContain("propertyInMultifileClass2's value", "functionInMultifileClass2's body")

        // Classes with MULTIFILE_CLASS kind have no proto data
        multifileClassSnapshot.assertDoesNotContain(
            "propertyInMultifileClass1",
            "functionInMultifileClass1",
            "propertyInMultifileClass2",
            "functionInMultifileClass2"
        )
    }
}

class JavaOnlyClasspathSnapshotterTest : ClasspathSnapshotTestCommon() {

    @Suppress("SameParameterValue")
    private fun getSourceFile(testName: String, relativePath: String) = TestSourceFile(
        JavaSourceFile(baseDir = File("$testDataDir/java/$testName/src"), relativePath = relativePath), tmpDir
    )

    private fun TestSourceFile.compileAndSnapshotWithDebugInfo(): ClassSnapshot {
        val classFile = compileSingle()
        return ClassSnapshotter.snapshot(
            listOf(ClassFileWithContents(classFile, classFile.readBytes())),
            includeDebugInfoInJavaSnapshot = true
        ).single()
    }

    @Test
    fun testSimpleClass() {
        val sourceFile = getSourceFile("testSimpleClass", "com/example/SimpleClass.java")
        val actualSnapshot = sourceFile.compileAndSnapshotWithDebugInfo().toGson()
        val expectedSnapshot = sourceFile.getExpectedSnapshotFile().readText()

        assertEquals(expectedSnapshot, actualSnapshot)

        // Check that the snapshot contains ABI info
        actualSnapshot.assertContains("publicField", "publicMethod")

        // Check that the snapshot does not contain non-ABI info
        actualSnapshot.assertDoesNotContain(
            "privateField",
            "privateMethod",
            "publicField's value",
            "privateField's value",
            "publicMethod's body",
            "privateMethod's body"
        )
    }
}

private fun TestSourceFile.getExpectedSnapshotFile(): File {
    val relativePath = sourceFile.unixStyleRelativePath.substringBeforeLast(".") + ".json"
    return sourceFile.baseDir.resolve("../expected-snapshot/$relativePath")
}

private fun String.assertContains(vararg elements: String) {
    elements.forEach {
        assertTrue(contains(it))
    }
}

private fun String.assertDoesNotContain(vararg elements: String) {
    elements.forEach {
        assertFalse(contains(it))
    }
}
