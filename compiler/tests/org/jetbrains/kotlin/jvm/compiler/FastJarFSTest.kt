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
        super.tearDown()
    }

    fun testAllSetUpAnnotationsArePresent() {
        val fs = fs ?: return
        val tmpDir = KotlinTestUtils.tmpDirForTest(this)
        val jarFile = File(tmpDir, "tmp.jar")
        val out = ZipOutputStream(FileOutputStream(jarFile))

        for (i in 0..70000) {
            out.putNextEntry(ZipEntry("$i.txt"))
            out.writer().apply {
                append(i.toString())
                flush()
            }
        }

        out.close()

        for (i in 0..70000) {
            val file = fs.findFileByPath(jarFile.absolutePath + "!/$i.txt") ?: error("Not found $i.txt")
            Assert.assertEquals(String(file.contentsToByteArray()), i.toString())
        }
    }
}
