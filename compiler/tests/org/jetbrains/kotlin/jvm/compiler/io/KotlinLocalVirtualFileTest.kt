/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler.io

import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.localfs.KotlinLocalFileSystem
import org.jetbrains.kotlin.cli.common.localfs.KotlinLocalVirtualFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import kotlin.test.assertNotEquals

class KotlinLocalVirtualFileTest : TestCase() {
    private var fs: KotlinLocalFileSystem? = null

    override fun setUp() {
        super.setUp()
        fs = KotlinLocalFileSystem()
    }

    override fun tearDown() {
        fs = null
        super.tearDown()
    }

    fun testEqualityOnTheSamePhysicalFile() {
        val fs = fs ?: return
        val testDir = KotlinTestUtils.tmpDirForTest(this)
        val file0 = File(testDir, "test")
        val file1 = File(testDir, "test")

        file0.createNewFile()
        val localFile0 = KotlinLocalVirtualFile(file0, fs)
        var localFile1 = KotlinLocalVirtualFile(file1, fs)

        assertEquals(localFile0, localFile1)
        assertEquals(localFile0.hashCode(), localFile1.hashCode())

        file1.writeText("newText")

        localFile1 = KotlinLocalVirtualFile(file1, fs)
        assertNotEquals(localFile0, localFile1)
        assertNotEquals(localFile0.hashCode(), localFile1.hashCode())

        file1.writeText("")

        localFile1 = KotlinLocalVirtualFile(file1, fs)
        // Files are not equal even if their content is the same because the modified date is only considered
        assertNotEquals(localFile0, localFile1)
        assertNotEquals(localFile0.hashCode(), localFile1.hashCode())
    }

    fun testEqualityOnTheSameDirectory() {
        val fs = fs ?: return

        val testDir = KotlinTestUtils.tmpDirForTest(this)
        val directory = File(testDir, "testDir")

        directory.mkdir()

        val localDir = KotlinLocalVirtualFile(directory, fs)
        assertTrue(localDir.isDirectory)
        val timeStampOrigin = localDir.timeStamp

        val fileInsideDir = File(directory, "testFile").also { it.createNewFile() }
        fileInsideDir.writeText("text")

        var localDirAfterFileAdding = KotlinLocalVirtualFile(directory, fs)
        assertTrue(localDirAfterFileAdding.isDirectory)
        val timeStampAfterFileAdding = localDirAfterFileAdding.timeStamp

        if (timeStampOrigin == timeStampAfterFileAdding) { // This equality depends on a file system. Sometimes they are equal, sometimes they are not
            localDirAfterFileAdding.file.setLastModified(timeStampOrigin + 1)
            localDirAfterFileAdding = KotlinLocalVirtualFile(directory, fs)
        }

        // But two directories with the same path should always be equal even if they have different modified dates
        assertEquals(localDir, localDirAfterFileAdding)
        assertEquals(localDir.hashCode(), localDirAfterFileAdding.hashCode())
    }
}