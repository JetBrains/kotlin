/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.ClassFileUtil.readBytes
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.SourceFile.JavaSourceFile
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.SourceFile.KotlinSourceFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.io.File

abstract class ClasspathSnapshotterTest : ClasspathSnapshotTestCommon() {

    companion object {
        val testDataDir =
            File("compiler/incremental-compilation-impl/testData/org/jetbrains/kotlin/incremental/classpathDiff/ClasspathSnapshotterTest")
    }

    protected abstract val sourceFile: TestSourceFile
    protected abstract val sourceFileWithAbiChange: TestSourceFile
    protected abstract val sourceFileWithNonAbiChange: TestSourceFile

    private val expectedSnapshotFile: File
        get() = sourceFile.asFile().let {
            val srcDir = File(it.path.substringBeforeLast("src") + "src")
            val relativePath = it.relativeTo(srcDir)
            testDataDir.resolve("expected-snapshot").resolve(relativePath.parent).resolve(relativePath.nameWithoutExtension + ".json")
        }

    @Test
    fun `test ClassSnapshotter's result against expected snapshot`() {
        val classSnapshot = sourceFile.compileSingle().let {
            ClassSnapshotter.snapshot(listOf(ClassFileWithContents(it, it.readBytes())), includeDebugInfoInJavaSnapshot = true)
        }.single()

        assertEquals(expectedSnapshotFile.readText(), classSnapshot.toGson())
    }

    @Test
    fun `test ClassSnapshotter extracts ABI info from a class`() {
        // After an ABI change, the snapshot must change
        assertNotEquals(sourceFile.compileAndSnapshot().toGson(), sourceFileWithAbiChange.compileAndSnapshot().toGson())
    }

    @Test
    fun `test ClassSnapshotter does not extract non-ABI info from a class`() {
        // After a non-ABI change, the snapshot must not change
        assertEquals(sourceFile.compileAndSnapshot().toGson(), sourceFileWithNonAbiChange.compileAndSnapshot().toGson())
    }
}

class KotlinOnlyClasspathSnapshotterTest : ClasspathSnapshotterTest() {

    override val sourceFile = TestSourceFile(
        KotlinSourceFile(
            baseDir = File(testDataDir, "src/kotlin"), relativePath = "com/example/SimpleClass.kt",
            preCompiledClassFile = ClassFile(File(testDataDir, "classes/kotlin"), "com/example/SimpleClass.class")
        ), tmpDir
    )

    override val sourceFileWithAbiChange = TestSourceFile(
        KotlinSourceFile(
            baseDir = File(testDataDir, "src-changed/kotlin/abi-change"), relativePath = "com/example/SimpleClass.kt",
            preCompiledClassFile = ClassFile(File(testDataDir, "classes-changed/kotlin/abi-change"), "com/example/SimpleClass.class")
        ), tmpDir
    )

    override val sourceFileWithNonAbiChange = TestSourceFile(
        KotlinSourceFile(
            baseDir = File(testDataDir, "src-changed/kotlin/non-abi-change"), relativePath = "com/example/SimpleClass.kt",
            preCompiledClassFile = ClassFile(File(testDataDir, "classes-changed/kotlin/non-abi-change"), "com/example/SimpleClass.class")
        ), tmpDir
    )
}

class JavaOnlyClasspathSnapshotterTest : ClasspathSnapshotterTest() {

    override val sourceFile = TestSourceFile(
        JavaSourceFile(
            baseDir = File(testDataDir, "src/java"), relativePath = "com/example/SimpleClass.java",
        ), tmpDir
    )

    override val sourceFileWithAbiChange = TestSourceFile(
        JavaSourceFile(
            baseDir = File(testDataDir, "src-changed/java/abi-change"), relativePath = "com/example/SimpleClass.java",
        ), tmpDir
    )

    override val sourceFileWithNonAbiChange = TestSourceFile(
        JavaSourceFile(
            baseDir = File(testDataDir, "src-changed/java/non-abi-change"), relativePath = "com/example/SimpleClass.java",
        ), tmpDir
    )
}
