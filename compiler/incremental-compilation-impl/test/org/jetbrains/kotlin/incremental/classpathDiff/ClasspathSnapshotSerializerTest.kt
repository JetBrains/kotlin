/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.SourceFile.JavaSourceFile
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.SourceFile.KotlinSourceFile
import org.jetbrains.kotlin.incremental.storage.fromByteArray
import org.jetbrains.kotlin.incremental.storage.toByteArray
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

abstract class ClasspathSnapshotSerializerTest : ClasspathSnapshotTestCommon() {

    companion object {
        val testDataDir =
            File("compiler/incremental-compilation-impl/testData/org/jetbrains/kotlin/incremental/classpathDiff/ClasspathSnapshotterTest")
    }

    protected abstract val sourceFile: TestSourceFile

    @Test
    open fun `test ClassSnapshotExternalizer`() {
        val originalSnapshot = sourceFile.compileAndSnapshot()
        val serializedSnapshot = ClassSnapshotExternalizer.toByteArray(originalSnapshot)
        val deserializedSnapshot = ClassSnapshotExternalizer.fromByteArray(serializedSnapshot)

        assertEquals(originalSnapshot.toGson(), deserializedSnapshot.toGson())
    }
}

class KotlinClassesClasspathSnapshotSerializerTest : ClasspathSnapshotSerializerTest() {

    override val sourceFile = TestSourceFile(
        KotlinSourceFile(
            baseDir = File(testDataDir, "kotlin/testSimpleClass/src"), relativePath = "com/example/SimpleClass.kt",
            preCompiledClassFile = ClassFile(File(testDataDir, "kotlin/testSimpleClass/classes"), "com/example/SimpleClass.class")
        ), tmpDir
    )
}

class JavaClassesClasspathSnapshotSerializerTest : ClasspathSnapshotSerializerTest() {

    override val sourceFile = TestSourceFile(
        JavaSourceFile(
            baseDir = File(testDataDir, "java/testSimpleClass/src"), relativePath = "com/example/SimpleClass.java",
        ), tmpDir
    )
}
