/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import org.jetbrains.kotlin.TestWithWorkingDir
import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.properties.Delegates

class SourceToOutputFilesMapTest : TestWithWorkingDir() {
    private var stofMap: SourceToOutputFilesMap by Delegates.notNull()
    private var pathConverter: FileToPathConverter by Delegates.notNull()

    @Before
    override fun setUp() {
        super.setUp()
        val caches = File(workingDir, "caches").apply { mkdirs() }
        val stofMapFile = File(caches, "stof.tab")
        pathConverter = IncrementalFileToPathConverter((workingDir.canonicalFile))
        val icContext = IncrementalCompilationContext(
            pathConverter = pathConverter
        )
        stofMap = SourceToOutputFilesMap(stofMapFile, icContext)
    }

    @After
    override fun tearDown() {
        stofMap.flush(false)
        stofMap.closeForTest()
        super.tearDown()
    }

    @Test
    fun testEmptyGetReturnsEmpty() {
        assertTrue(stofMap.get(File("")).isEmpty())
    }

    @Test
    fun testSetGetOneReturnsOne() {
        stofMap.set(
            File(""),
            listOf(File("one").canonicalFile))
        assertEquals(
            listOf(File("one").canonicalFile),
            stofMap.get(File("")))
    }

    @Test
    fun testSetDupeReturnsUnique() {
        stofMap.set(
            File(""),
            listOf(File("one").canonicalFile, File("one").canonicalFile, File("one").canonicalFile))

        assertEquals(
            listOf(File("one").canonicalFile),
            stofMap.get(File("")))
    }

    @Test
    fun testSetOverwriteReturnsNew() {
        stofMap.set(
            File(""),
            listOf(File("old").canonicalFile, File("old").canonicalFile, File("old").canonicalFile))
        stofMap.set(
            File(""),
            listOf(File("one").canonicalFile, File("two").canonicalFile, File("three").canonicalFile))

        assertArrayEquals(
            listOf(File("one").canonicalFile, File("two").canonicalFile, File("three").canonicalFile).toSortedPaths(),
            stofMap.get(File("")).toSortedPaths())
    }

    @Test
    fun testRelativeInReturnsAbsolute() {
        stofMap.set(
            File(""),
            listOf(File("one"), File("two"), File("three")))

        assertArrayEquals(
            listOf(File("one").canonicalFile, File("two").canonicalFile, File("three").canonicalFile).toSortedPaths(),
            stofMap.get(File("")).toSortedPaths()
        )
    }

    @Test
    fun testSetRelativeGetAbsolute() {
        stofMap.set(
            File("blah"),
            listOf(File("one"), File("two"), File("three")))

        assertArrayEquals(
            listOf(File("one").canonicalFile, File("two").canonicalFile, File("three").canonicalFile).toSortedPaths(),
            stofMap.get(File("blah").canonicalFile).toSortedPaths()
        )
    }

    @Test
    fun testSetRemove() {
        stofMap.set(
            File("blah"),
            listOf(File("one"), File("two"), File("three")))

        assertArrayEquals(
            listOf(File("one").canonicalFile, File("two").canonicalFile, File("three").canonicalFile).toSortedPaths(),
            stofMap.remove(File("blah")).toSortedPaths()
        )
        assertTrue(stofMap.get(File("blah")).isEmpty())
    }

    @Test
    fun testSetRemoveLoop() {
        repeat(5) {
            stofMap.set(
                File("blah"),
                listOf(File("one"), File("two"), File("three"))
            )

            assertArrayEquals(
                listOf(File("one").canonicalFile, File("two").canonicalFile, File("three").canonicalFile).toSortedPaths(),
                stofMap.remove(File("blah")).toSortedPaths()
            )
            assertTrue(stofMap.get(File("blah")).isEmpty())
        }
    }

    private fun Iterable<File>.toSortedPaths(): Array<String> =
        map { it.canonicalPath }.sorted().toTypedArray()
}