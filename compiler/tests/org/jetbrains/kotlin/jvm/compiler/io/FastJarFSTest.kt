/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler.io

import com.intellij.core.JavaCoreApplicationEnvironment
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.FastJarFileSystem
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


abstract class AbstractFastJarFSTest {

    protected var fs: FastJarFileSystem? = null
    protected lateinit var testInfo: TestInfo
    protected var coreAppEnv: JavaCoreApplicationEnvironment? = null
    private val rootDisposable = Disposer.newDisposable("${FastJarFSTest::class.simpleName}.rootDisposable")

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        this.testInfo = testInfo
        fs = FastJarFileSystem.createIfUnmappingPossible()
        coreAppEnv = JavaCoreApplicationEnvironment(rootDisposable)
    }

    @AfterEach
    fun tearDown() {
        coreAppEnv = null
        rootDisposable.dispose()
        fs?.clearHandlersCache()
        fs = null
    }
}

class FastJarFSTest : AbstractFastJarFSTest() {

    @Test
    fun testZip64FormatIsSupported() {
        val fs = fs ?: return
        val tmpDir = KotlinTestUtils.tmpDirForTest(testInfo)
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
            assertEquals(String(file.contentsToByteArray()), i.toString())
        }
    }

    @Test
    fun testInvalidJar() {
        val fs = fs ?: return
        val tmpDir = KotlinTestUtils.tmpDirForTest(testInfo)
        val badJarFile = File(tmpDir, "file.pom")
        badJarFile.writeText(A_POM_FILE)

        val errFromFastJarFs = captureErr {
            fs.findFileByPath(badJarFile.absolutePath + "!/a.class")
        }
        Assertions.assertTrue(errFromFastJarFs.contains("WARN: Error while reading zip file:"))

        val errFromCoreJarFs = captureErr {
            coreAppEnv!!.jarFileSystem.findFileByPath(badJarFile.absolutePath + "!/a.class")
        }
        // Asserting that core jar FS still behaves the same way as the "emulation" implemented in FastJarFS
        Assertions.assertTrue(
            errFromCoreJarFs.contains("WARN: error in opening zip file") ||
                                errFromCoreJarFs.contains("WARN: zip END header not found")
        )
    }

    @Test
    fun testEmptyJar() {
        val fs = fs ?: return
        val tmpDir = KotlinTestUtils.tmpDirForTest(testInfo)
        val emptyJarFile = File(tmpDir, "empty.jar")
        emptyJarFile.createNewFile()

        val errFromFastJarFs = captureErr {
            fs.findFileByPath(emptyJarFile.absolutePath + "!/a.class")
        }
        Assertions.assertTrue(errFromFastJarFs.contains("WARN: Error while reading zip file:"))

        val errFromCoreJarFs = captureErr {
            coreAppEnv!!.jarFileSystem.findFileByPath(emptyJarFile.absolutePath + "!/a.class")
        }
        // Asserting that core jar FS still behaves the same way as the "emulation" implemented in FastJarFS
        Assertions.assertTrue(errFromCoreJarFs.contains("WARN: zip file is empty"))
    }

    @Test
    fun testPlainDecoding() {
        val fs = fs ?: return
        val tmpDir = KotlinTestUtils.tmpDirForTest(testInfo)
        val jarFile = File(tmpDir, "tmp.jar")

        val data = "someFlatData"
        ZipOutputStream(FileOutputStream(jarFile)).use { out ->
            val buf = data.toByteArray()
            out.setMethod(ZipOutputStream.STORED)
            val entry = ZipEntry("flat.txt").apply {
                size = buf.size.toLong()
                compressedSize = buf.size.toLong()
                val crc32compute = CRC32()
                crc32compute.update(buf)
                crc = crc32compute.value
            }
            out.putNextEntry(entry)
            out.write(buf)
            out.closeEntry()
        }
        assertEquals(data, String(fs.findFileByPath(jarFile.absolutePath + "!/flat.txt")!!.contentsToByteArray()))
    }
}

private fun captureErr(body: () -> Unit): String {
    val outStream = ByteArrayOutputStream()
    val prevErr = System.err
    System.setErr(PrintStream(outStream))
    try {
        body()
    } finally {
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
