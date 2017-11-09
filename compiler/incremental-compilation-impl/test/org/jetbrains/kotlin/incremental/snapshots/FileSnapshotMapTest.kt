package org.jetbrains.kotlin.incremental.snapshots

import org.jetbrains.kotlin.TestWithWorkingDir
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.properties.Delegates

class FileSnapshotMapTest : TestWithWorkingDir() {
    private var snapshotMap: FileSnapshotMap by Delegates.notNull()

    @Before
    override fun setUp() {
        super.setUp()
        val caches = File(workingDir, "caches").apply { mkdirs() }
        val snapshotMapFile = File(caches, "snapshots.tab")
        snapshotMap = FileSnapshotMap(snapshotMapFile)
    }

    @After
    override fun tearDown() {
        snapshotMap.flush(false)
        snapshotMap.close()
        super.tearDown()
    }

    @Test
    fun testSnapshotMap() {
        val src = File(workingDir, "src").apply { mkdirs() }
        val foo = File(src, "foo").apply { mkdirs() }

        val removedTxt = File(foo, "removed.txt").apply { writeText("removed") }
        val unchangedTxt = File(foo, "unchanged.txt").apply { writeText("unchanged") }
        val changedTxt = File(foo, "changed.txt").apply { writeText("changed") }

        val diff1 = snapshotMap.compareAndUpdate(src.filesWithExt("txt"))

        assertArrayEquals("diff1.removed",
                diff1.removed.toSortedPaths(),
                emptyArray<String>())
        assertArrayEquals("diff1.newOrModified",
                diff1.modified.toSortedPaths(),
                listOf(removedTxt, unchangedTxt, changedTxt).toSortedPaths())

        removedTxt.delete()
        unchangedTxt.writeText("unchanged")
        changedTxt.writeText("degnahc")
        val newTxt = File(foo, "new.txt").apply { writeText("new") }

        val diff2 = snapshotMap.compareAndUpdate(src.filesWithExt("txt"))
        assertArrayEquals("diff2.removed",
                diff2.removed.toSortedPaths(),
                listOf(removedTxt).toSortedPaths())
        assertArrayEquals("diff2.newOrModified",
                diff2.modified.toSortedPaths(),
                listOf(newTxt, changedTxt).toSortedPaths())
    }

    private fun Iterable<File>.toSortedPaths(): Array<String> =
            map { it.canonicalPath }.sorted().toTypedArray()

    private fun File.filesWithExt(ext: String): Iterable<File> =
            walk().filter { it.isFile && it.extension.equals(ext, ignoreCase = true) }.toList()
}