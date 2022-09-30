/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import junit.framework.TestCase
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.FastJarFileSystem
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class FastJarFSTest : TestCase() {

    private var fs: FastJarFileSystem? = null

    override fun setUp() {
        super.setUp()
        fs = FastJarFileSystem.createIfUnmappingPossible()
    }

    override fun tearDown() {
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
}
