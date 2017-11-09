package org.jetbrains.kotlin.incremental.snapshots

import org.jetbrains.kotlin.TestWithWorkingDir
import org.junit.Assert.*
import org.junit.Test
import java.io.*

class FileSnapshotTest : TestWithWorkingDir() {
    private val fileSnapshotProvider: FileSnapshotProvider
            get() = SimpleFileSnapshotProviderImpl()

    @Test
    fun testExternalizer() {
        val file = File(workingDir, "1.txt")
        file.writeText("test")
        val snapshot = fileSnapshotProvider[file]
        val deserializedSnapshot = saveAndReadBack(snapshot)
        assertEquals(snapshot, deserializedSnapshot)
    }

    @Test
    fun testEqualityNoChanges() {
        val file = File(workingDir, "1.txt").apply { writeText("file") }
        val oldSnapshot = fileSnapshotProvider[file]
        val newSnapshot = fileSnapshotProvider[file]
        assertEquals(oldSnapshot, newSnapshot)
    }

    @Test
    fun testEqualityDifferentFile() {
        val file1 = File(workingDir, "1.txt").apply { writeText("file1") }
        val file2 = File(workingDir, "2.txt").apply {
            writeText(file1.readText())
            setLastModified(file1.lastModified())
        }
        val oldSnapshot = fileSnapshotProvider[file1]
        val newSnapshot = fileSnapshotProvider[file2]
        assertNotEquals(oldSnapshot, newSnapshot)
    }

    @Test
    fun testEqualityDifferentTimestamp() {
        val text = "file"
        val file = File(workingDir, "1.txt").apply { writeText(text) }
        val oldSnapshot = fileSnapshotProvider[file]
        Thread.sleep(1000)
        file.writeText(text)
        val newSnapshot = fileSnapshotProvider[file]
        assertEquals(oldSnapshot, newSnapshot)
    }

    @Test
    fun testEqualityDifferentSize() {
        val file = File(workingDir, "1.txt").apply { writeText("file") }
        val oldSnapshot = fileSnapshotProvider[file]
        file.writeText("file modified")
        val newSnapshot = fileSnapshotProvider[file]
        assertNotEquals(oldSnapshot, newSnapshot)
    }

    @Test
    fun testEqualityDifferentHash() {
        val file = File(workingDir, "1.txt").apply { writeText("file") }
        val oldSnapshot = fileSnapshotProvider[file]
        file.writeText("main")
        val newSnapshot = fileSnapshotProvider[file]
        assertNotEquals(oldSnapshot, newSnapshot)
    }

    private fun saveAndReadBack(snapshot: FileSnapshot): FileSnapshot {
        val byteOut = ByteArrayOutputStream()
        DataOutputStream(byteOut).use { FileSnapshotExternalizer.save(it, snapshot) }
        val byteIn = ByteArrayInputStream(byteOut.toByteArray())
        return DataInputStream(byteIn).use { FileSnapshotExternalizer.read(it) }
    }
}