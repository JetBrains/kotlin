/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler.io

import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random

private const val BIG_JAR_ENTRY_CHUNK_SIZE = 1024 * 1024
private const val BIG_JAR_ENTRY_NUMBER_OF_CHUNKS = (Int.MAX_VALUE.toLong() + 1) / BIG_JAR_ENTRY_CHUNK_SIZE

// separate test cases to allow parallel run
// NOTE: excluded from regular tests in compiler/build.gradle.kts
class FastJarFSLongTest2M : AbstractFastJarFSTest() {

    // tests the zip file larger than Int.MAX_VALUE, so it doesn't fit into MappedByteBuffer, but smaller than UInt.MAX__VALUE,
    // so no ZIP64 format is triggered
    fun testInterleaveSmallAndBigJarEntries() {
        val fs = fs!!
        val tmpDir = KotlinTestUtils.tmpDirForTest(this)
        val jarFile = File(tmpDir, "tmp-2m.jar")

        ZipOutputStream(FileOutputStream(jarFile)).use { out ->
            out.addEntry("small1.txt") { append("smallOne") }
            out.addRandomEntry("big.txt", BIG_JAR_ENTRY_CHUNK_SIZE, BIG_JAR_ENTRY_NUMBER_OF_CHUNKS)
            out.addEntry("small2.txt") { append("smallTwo") }
        }

        assert(jarFile.length() > Int.MAX_VALUE)

        // accessing out of order to test remapping
        assertEquals("smallOne", String(fs.findFileByPath(jarFile.absolutePath + "!/small1.txt")!!.contentsToByteArray()))
        assertEquals("smallTwo", String(fs.findFileByPath(jarFile.absolutePath + "!/small2.txt")!!.contentsToByteArray()))
        assertEquals(
            BIG_JAR_ENTRY_NUMBER_OF_CHUNKS * BIG_JAR_ENTRY_CHUNK_SIZE,
            fs.findFileByPath(jarFile.absolutePath + "!/big.txt")!!.length
        )
    }
}


// separate test cases to allow parallel run
// NOTE: excluded from regular tests in compiler/build.gradle.kts
class FastJarFSLongTestZip64 : AbstractFastJarFSTest() {

    // tests the zip file larger than UInt.MAX_VALUE, so ZIP64 fields are used in the directory
    fun testInterleaveSmallAndBigJarEntriesZip64() {
        val fs = fs!!
        val tmpDir = KotlinTestUtils.tmpDirForTest(this)
        val jarFile = File(tmpDir, "tmp-zip64.jar")

        ZipOutputStream(FileOutputStream(jarFile)).use { out ->
            out.addEntry("small1.txt") { append("smallOne") }
            out.addRandomEntry("big1.txt", BIG_JAR_ENTRY_CHUNK_SIZE, BIG_JAR_ENTRY_NUMBER_OF_CHUNKS)
            out.addEntry("small2.txt") { append("smallTwo") }
            out.addRandomEntry("big2.txt", BIG_JAR_ENTRY_CHUNK_SIZE, BIG_JAR_ENTRY_NUMBER_OF_CHUNKS + 2)
            out.addEntry("small3.txt") { append("smallThree") }
        }

        assert(jarFile.length() > UInt.MAX_VALUE.toLong())

        // accessing out of order to test remapping
        assertEquals("smallOne", String(fs.findFileByPath(jarFile.absolutePath + "!/small1.txt")!!.contentsToByteArray()))
        assertEquals("smallThree", String(fs.findFileByPath(jarFile.absolutePath + "!/small3.txt")!!.contentsToByteArray()))
        assertEquals(
            (BIG_JAR_ENTRY_NUMBER_OF_CHUNKS + 2) * BIG_JAR_ENTRY_CHUNK_SIZE,
            fs.findFileByPath(jarFile.absolutePath + "!/big2.txt")!!.length
        )
        assertEquals(
            BIG_JAR_ENTRY_NUMBER_OF_CHUNKS * BIG_JAR_ENTRY_CHUNK_SIZE,
            fs.findFileByPath(jarFile.absolutePath + "!/big1.txt")!!.length
        )
        assertEquals("smallTwo", String(fs.findFileByPath(jarFile.absolutePath + "!/small2.txt")!!.contentsToByteArray()))
    }

    // it will be nice to have a test that checks logic of ZIP directory reading on the directories that doesn't fit to MappedByteBuffer
    // but this tests fails with OOM on such zip file generation.
    // TODO: find out how to implement such tests
    @Suppress("unused")
    fun skip_testBigJarDirectory() {
        val fs = fs ?: return
        val tmpDir = KotlinTestUtils.tmpDirForTest(this)
        val jarFile = File(tmpDir, "tmp.jar")
        val out = ZipOutputStream(FileOutputStream(jarFile))

        val chunkSize = 1024 * 4
        val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val chunk = generateSequence { characters[Random.nextInt(0, characters.length)] }.take(chunkSize).joinToString(separator = "")
        val entriesNumber = (Int.MAX_VALUE.toLong() + 1) / chunkSize
        for (i in 0..entriesNumber) {
            out.putNextEntry(ZipEntry("$i$chunk"))
            out.writer().apply {
                append(i.toString())
                flush()
            }
            out.closeEntry()
        }
        out.putNextEntry(ZipEntry("000"))
        out.writer().apply {
            append("!")
            flush()
        }
        out.close()

        fs.findFileByPath(jarFile.absolutePath + "!/000")
    }
}

private fun ZipOutputStream.addEntry(name: String, body: OutputStreamWriter.() -> Unit) {
    putNextEntry(ZipEntry(name))
    writer().apply {
        body()
        flush()
    }
    closeEntry()
}

private fun ZipOutputStream.addRandomEntry(name: String, chunkSize: Int, repetitions: Long) {
    putNextEntry(ZipEntry(name))
    val chunk = Random.nextBytes(ByteArray(chunkSize), 0, chunkSize)
    for (i in 0 until repetitions) {
        write(chunk)
    }
    flush()
    closeEntry()
}
