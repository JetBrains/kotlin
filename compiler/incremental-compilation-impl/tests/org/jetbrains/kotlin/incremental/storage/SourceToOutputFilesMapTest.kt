/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertFailsWith

class SourceToOutputFilesMapTest {

    @field:TempDir
    lateinit var tmpDir: File

    private lateinit var srcDir: File
    private lateinit var classesDir: File

    private lateinit var stofMap: SourceToOutputFilesMap

    private lateinit var fooDotKt: File
    private lateinit var fooDotClass: File

    @BeforeEach
    fun setUp() {
        val workingDir = tmpDir

        srcDir = workingDir.resolve("src")
        classesDir = workingDir.resolve("classes")

        stofMap = SourceToOutputFilesMap(
            storageFile = workingDir.resolve("stof.tab"),
            icContext = IncrementalCompilationContext(
                pathConverterForSourceFiles = RelocatableFileToPathConverter(srcDir),
                pathConverterForOutputFiles = RelocatableFileToPathConverter(classesDir),
            )
        )

        fooDotKt = srcDir.resolve("Foo.kt")
        fooDotClass = classesDir.resolve("Foo.class")
    }

    @AfterEach
    fun tearDown() {
        stofMap.close()
    }

    @Test
    fun testNoSetGetReturnsNull() {
        assertNull(stofMap[fooDotKt])
    }

    @Test
    fun testSetOneGetReturnsOne() {
        stofMap[fooDotKt] = setOf(fooDotClass)

        assertEquals(setOf(fooDotClass), stofMap[fooDotKt])
    }

    @Test
    fun testSetDupeGetReturnsUnique() {
        stofMap.append(fooDotKt, fooDotClass)
        stofMap.append(fooDotKt, fooDotClass)

        assertEquals(setOf(fooDotClass), stofMap[fooDotKt])
    }

    @Test
    fun testSetOverwriteGetReturnsNew() {
        val fooKtDotClass = classesDir.resolve("FooKt.class")
        stofMap[fooDotKt] = setOf(fooDotClass)
        stofMap[fooDotKt] = setOf(fooKtDotClass)

        assertEquals(setOf(fooKtDotClass), stofMap[fooDotKt])
    }

    @Test
    fun testSetRelativePathFails() {
        assertFailsWith<IllegalStateException> {
            stofMap[fooDotKt] = setOf(File("relativePath"))
        }
        assertFailsWith<IllegalStateException> {
            stofMap[File("relativePath")] = setOf(fooDotClass)
        }
    }

    @Test
    fun testGetRelativePathFails() {
        stofMap[fooDotKt] = setOf(fooDotClass)

        assertFailsWith<IllegalStateException> {
            stofMap[File("relativePath")]
        }
    }

    @Test
    fun testGetAndRemove() {
        stofMap[fooDotKt] = setOf(fooDotClass)

        assertEquals(setOf(fooDotClass), stofMap.getAndRemove(fooDotKt))
        assertNull(stofMap[fooDotKt])
    }

}
