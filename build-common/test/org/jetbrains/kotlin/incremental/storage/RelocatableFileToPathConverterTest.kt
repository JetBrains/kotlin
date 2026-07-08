/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files

class RelocatableFileToPathConverterTest {

    @field:TempDir
    lateinit var tmpDir: File

    private lateinit var baseDir: File
    private lateinit var pathConverter: RelocatableFileToPathConverter

    @BeforeEach
    fun setUp() {
        baseDir = Files.createTempDirectory(tmpDir.toPath(), "baseDir").toFile()
        pathConverter = RelocatableFileToPathConverter(baseDir)
    }

    @Test
    fun testToPath() {
        assertEquals("com/example/Foo.kt", pathConverter.toPath(baseDir.resolve("com/example/Foo.kt")))
    }

    @Test
    fun testToFile() {
        assertEquals(baseDir.resolve("com/example/Foo.kt"), pathConverter.toFile("com/example/Foo.kt"))
    }

    @Test
    fun testFileOutsideBaseDirectory() {
        val fileOutsideBaseDir = baseDir.resolve("../outsideBaseDir/com/example/Foo.kt").normalize()

        assertEquals("../outsideBaseDir/com/example/Foo.kt", pathConverter.toPath(fileOutsideBaseDir))
        assertEquals(fileOutsideBaseDir, pathConverter.toFile("../outsideBaseDir/com/example/Foo.kt"))
    }

}
