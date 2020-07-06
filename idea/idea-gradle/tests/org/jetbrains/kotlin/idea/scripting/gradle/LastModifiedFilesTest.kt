/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.test.assertEquals

class LastModifiedFilesTest {
    val files = LastModifiedFiles()

    @Test
    fun testSimple0() {
        assertEquals(Long.MIN_VALUE, files.lastModifiedTimeStampExcept("x"))
    }

    @Test
    fun testSimple1() {
        files.fileChanged(1, "x")
        assertEquals(Long.MIN_VALUE, files.lastModifiedTimeStampExcept("x"))
        assertEquals(1, files.lastModifiedTimeStampExcept("y"))
    }

    @Test
    fun testSimple2() {
        files.fileChanged(1, "x")
        files.fileChanged(2, "y")
        assertEquals(2, files.lastModifiedTimeStampExcept("x"))
        assertEquals(1, files.lastModifiedTimeStampExcept("y"))
    }

    @Test
    fun testSimple3() {
        files.fileChanged(1, "x")
        files.fileChanged(2, "y")
        files.fileChanged(3, "z")
        assertEquals(3, files.lastModifiedTimeStampExcept("x"))
        assertEquals(3, files.lastModifiedTimeStampExcept("y"))
        assertEquals(2, files.lastModifiedTimeStampExcept("z"))
    }

    @Test
    fun testSame2() {
        files.fileChanged(1, "x")
        files.fileChanged(1, "y")
        assertEquals(1, files.lastModifiedTimeStampExcept("x"))
        assertEquals(1, files.lastModifiedTimeStampExcept("y"))
    }

    @Test
    fun testSame3() {
        files.fileChanged(1, "x")
        files.fileChanged(1, "y")
        files.fileChanged(2, "z")
        assertEquals(2, files.lastModifiedTimeStampExcept("x"))
        assertEquals(2, files.lastModifiedTimeStampExcept("y"))
        assertEquals(1, files.lastModifiedTimeStampExcept("z"))
    }

    @Test
    fun testSame4() {
        files.fileChanged(1, "x")
        files.fileChanged(1, "y")
        files.fileChanged(2, "z")
        files.fileChanged(3, "a")
        files.fileChanged(3, "b")
        assertEquals(3, files.lastModifiedTimeStampExcept("x"))
        assertEquals(3, files.lastModifiedTimeStampExcept("y"))
        assertEquals(3, files.lastModifiedTimeStampExcept("z"))
        assertEquals(3, files.lastModifiedTimeStampExcept("a"))
        assertEquals(3, files.lastModifiedTimeStampExcept("b"))
    }

    @Test
    fun testSameFile() {
        files.fileChanged(1, "x")
        files.fileChanged(2, "x")
        assertEquals(Long.MIN_VALUE, files.lastModifiedTimeStampExcept("x"))
    }

    @Test
    fun testSameFile1() {
        files.fileChanged(1, "z")
        files.fileChanged(2, "x")
        files.fileChanged(3, "x")
        assertEquals(1, files.lastModifiedTimeStampExcept("x"))
        assertEquals(3, files.lastModifiedTimeStampExcept("z"))
    }

    @Test
    fun testSameFile2() {
        files.fileChanged(1, "z")
        files.fileChanged(1, "x")
        files.fileChanged(2, "x")
        assertEquals(1, files.lastModifiedTimeStampExcept("x"))
        assertEquals(2, files.lastModifiedTimeStampExcept("z"))
    }

    @Test
    fun testSameFile3() {
        files.fileChanged(1, "x")
        files.fileChanged(1, "y")
        files.fileChanged(2, "x")
        assertEquals(1, files.lastModifiedTimeStampExcept("x"))
        assertEquals(2, files.lastModifiedTimeStampExcept("y"))
    }

    @Test
    fun testSameFile4() {
        files.fileChanged(1, "z")
        files.fileChanged(2, "x")
        files.fileChanged(2, "y")
        files.fileChanged(3, "x")
        assertEquals(2, files.lastModifiedTimeStampExcept("x"))
        assertEquals(3, files.lastModifiedTimeStampExcept("y"))
        assertEquals(3, files.lastModifiedTimeStampExcept("z"))
    }

    @Test
    fun testLoadingAfterSameFileBug() {
        val restored = LastModifiedFiles(
            LastModifiedFiles.SimultaneouslyChangedFiles(2, mutableSetOf("x")),
            LastModifiedFiles.SimultaneouslyChangedFiles(1, mutableSetOf("x"))
        )

        assertEquals(Long.MIN_VALUE, restored.lastModifiedTimeStampExcept("x"))
    }

    @Test
    fun testLoadingAfterSameFileBug2() {
        val restored = LastModifiedFiles(
            LastModifiedFiles.SimultaneouslyChangedFiles(2, mutableSetOf("x", "y")),
            LastModifiedFiles.SimultaneouslyChangedFiles(1, mutableSetOf("x"))
        )

        assertEquals(2, restored.lastModifiedTimeStampExcept("x"))
        assertEquals(2, restored.lastModifiedTimeStampExcept("y"))
    }

    @Test
    fun testLoadingAfterSameFileBug3() {
        val restored = LastModifiedFiles(
            LastModifiedFiles.SimultaneouslyChangedFiles(2, mutableSetOf("x", "y")),
            LastModifiedFiles.SimultaneouslyChangedFiles(1, mutableSetOf("x", "z"))
        )

        assertEquals(2, restored.lastModifiedTimeStampExcept("x"))
        assertEquals(2, restored.lastModifiedTimeStampExcept("y"))
    }

    @Test
    fun testFileAttributes() {
        val expected = LastModifiedFiles(
            LastModifiedFiles.SimultaneouslyChangedFiles(System.currentTimeMillis(), mutableSetOf("C:file/sldkfjlsdkjf")),
            LastModifiedFiles.SimultaneouslyChangedFiles(System.currentTimeMillis(), mutableSetOf("C:/file/foo/d/"))
        )
        checkFileAttributesSave(expected)
    }

    @Test
    fun testFileAttributesNullable() {
        val expected: LastModifiedFiles? = null
        checkFileAttributesSave(expected)
    }

    @Test
    fun testDefaultFileAttributes() {
        val expected = LastModifiedFiles()
        checkFileAttributesSave(expected)
    }

    private fun checkFileAttributesSave(expected: LastModifiedFiles?) {
        val buffer = ByteArrayOutputStream()
        LastModifiedFiles.writeLastModifiedFiles(DataOutputStream(buffer), expected)

        val actual = LastModifiedFiles.readLastModifiedFiles(DataInputStream(ByteArrayInputStream(buffer.toByteArray())))

        assertEquals(expected.toString(), actual.toString())
    }
}