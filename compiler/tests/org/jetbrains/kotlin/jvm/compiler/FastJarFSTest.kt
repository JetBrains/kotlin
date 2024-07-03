/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import com.intellij.core.JavaCoreApplicationEnvironment
import com.intellij.openapi.util.Disposer
import junit.framework.TestCase
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.FastJarFileSystem
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random


class FastJarFSTest : TestCase() {

    private var fs: FastJarFileSystem? = null
    private var coreAppEnv: JavaCoreApplicationEnvironment? = null
    private val rootDisposable = Disposer.newDisposable("${FastJarFSTest::class.simpleName}.rootDisposable")

    override fun setUp() {
        super.setUp()
        fs = FastJarFileSystem.createIfUnmappingPossible()
        coreAppEnv = JavaCoreApplicationEnvironment(rootDisposable)
    }

    override fun tearDown() {
        coreAppEnv = null
        rootDisposable.dispose()
        fs?.clearHandlersCache()
        fs = null
        super.tearDown()
    }

    fun testZip64FormatIsSupported() {
        val fs = fs ?: return
        val tmpDir = KotlinTestUtils.tmpDirForTest(this)
        val jarFile = File(tmpDir, "tmp.jar")
        val out = ZipOutputStream(FileOutputStream(jarFile))

        // Should be more than 65535
        val entriesNumber = 70000
        for (i in 0..entriesNumber) {
            out.putNextEntry(ZipEntry("$i.txt"))
            out.writer().apply {
                append(i.toString())
                flush()
            }
        }

        out.close()

        val indicesToCheck = listOf(
            0, entriesNumber / 2, entriesNumber / 3, entriesNumber - 1
        )

        for (i in indicesToCheck) {
            val file = fs.findFileByPath(jarFile.absolutePath + "!/$i.txt") ?: error("Not found $i.txt")
            Assert.assertEquals(String(file.contentsToByteArray()), i.toString())
        }
    }

    fun testInvalidJar() {
        val fs = fs ?: return
        val tmpDir = KotlinTestUtils.tmpDirForTest(this)
        val badJarFile = File(tmpDir, "file.pom")
        badJarFile.writeText(A_POM_FILE)

        val errFromFastJarFs = captureErr {
            fs.findFileByPath(badJarFile.absolutePath + "!/a.class")
        }
        Assert.assertTrue(errFromFastJarFs.contains("WARN: Error while reading zip file:"))

        val errFromCoreJarFs = captureErr {
            coreAppEnv!!.jarFileSystem.findFileByPath(badJarFile.absolutePath + "!/a.class")
        }
        // Asserting that core jar FS still behaves the same way as the "emulation" implemented in FastJarFS
        Assert.assertTrue(errFromCoreJarFs.contains("WARN: error in opening zip file"))
    }

    fun testEmptyJar() {
        val fs = fs ?: return
        val tmpDir = KotlinTestUtils.tmpDirForTest(this)
        val emptyJarFile = File(tmpDir, "empty.jar")
        emptyJarFile.createNewFile()

        val errFromFastJarFs = captureErr {
            fs.findFileByPath(emptyJarFile.absolutePath + "!/a.class")
        }
        Assert.assertTrue(errFromFastJarFs.contains("WARN: Error while reading zip file:"))

        val errFromCoreJarFs = captureErr {
            coreAppEnv!!.jarFileSystem.findFileByPath(emptyJarFile.absolutePath + "!/a.class")
        }
        // Asserting that core jar FS still behaves the same way as the "emulation" implemented in FastJarFS
        Assert.assertTrue(errFromCoreJarFs.contains("WARN: zip file is empty"))
    }

    fun testInterleaveSmallAndBigJarEntriesInOrder() {
        // tests the zip file larger than Int.MAX_VALUE, so it doesn't fit into MappedByteBuffer, but smaller than UInt.MAX__VALUE,
        // so no ZIP64 format is triggered
        val fs = fs ?: return
        val tmpDir = KotlinTestUtils.tmpDirForTest(this)
        val jarFile = File(tmpDir, "tmp.jar")

        val chunkSize = 1024 * 1024
        val repetitions = (Int.MAX_VALUE.toLong() + 1) / chunkSize

        ZipOutputStream(FileOutputStream(jarFile)).use { out ->
            out.addEntry("small1.txt") { append("smallOne") }
            out.addRandomEntry("big.txt", chunkSize, repetitions)
            out.addEntry("small2.txt") { append("smallTwo") }
        }

        assert(jarFile.length() > Int.MAX_VALUE)
        assertEquals("smallOne", String(fs.findFileByPath(jarFile.absolutePath + "!/small1.txt")!!.contentsToByteArray()))
        assertEquals(repetitions * chunkSize, fs.findFileByPath(jarFile.absolutePath + "!/big.txt")!!.length)
        assertEquals("smallTwo", String(fs.findFileByPath(jarFile.absolutePath + "!/small2.txt")!!.contentsToByteArray()))
    }

    fun testInterleaveSmallAndBigJarEntriesOutOfOrder() {
        // similar to testInterleaveSmallAndBigJarEntriesInOrder, but with out of order files reading, promting more remappings
        val fs = fs ?: return
        val tmpDir = KotlinTestUtils.tmpDirForTest(this)
        val jarFile = File(tmpDir, "tmp.jar")

        val chunkSize = 1024 * 1024
        val repetitions = (Int.MAX_VALUE.toLong() + 1) / chunkSize

        ZipOutputStream(FileOutputStream(jarFile)).use { out ->
            out.addEntry("small1.txt") { append("smallOne") }
            out.addRandomEntry("big.txt", chunkSize, repetitions)
            out.addEntry("small2.txt") { append("smallTwo") }
        }

        assert(jarFile.length() > Int.MAX_VALUE)
        assertEquals("smallTwo", String(fs.findFileByPath(jarFile.absolutePath + "!/small2.txt")!!.contentsToByteArray()))
        assertEquals(repetitions * chunkSize, fs.findFileByPath(jarFile.absolutePath + "!/big.txt")!!.length)
        assertEquals("smallOne", String(fs.findFileByPath(jarFile.absolutePath + "!/small1.txt")!!.contentsToByteArray()))
    }

    fun skip_testBigJarDirectory() {
        // it will be nice to have a test that checks logic of ZIP directory reading on the directories that doesn't fit to MappedByteBuffer
        // but this tests fails with OOM on such zip file generation.
        // TODO: find out how to implement such tests
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

private fun captureErr(body: () -> Unit): String {
    val outStream = ByteArrayOutputStream()
    val prevErr = System.err
    System.setErr(PrintStream(outStream))
    try {
        body()
    }
    finally {
        System.err.flush()
        System.setErr(prevErr)
    }
    return outStream.toString()
}

private const val A_POM_FILE =
    """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <modelVersion>4.0.0</modelVersion>
  <groupId>a</groupId>
  <artifactId>b</artifactId>
  <version>0.0.1</version>
  <packaging>pom</packaging>
  <dependencies>
    <dependency>
      <groupId>c</groupId>
      <artifactId>d</artifactId>
      <version>0.0.1</version>
      <scope>compile</scope>
    </dependency>
  </dependencies>
</project>
"""