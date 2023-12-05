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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


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