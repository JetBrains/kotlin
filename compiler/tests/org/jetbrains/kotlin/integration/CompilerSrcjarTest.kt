/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.integration

import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Tests the ability to compile sources contained in .srcjar files.
 * This is useful in build systems like Bazel, where .srcjar files are used to hold generated sources.
 */
class CompilerSrcjarTest : CompilerSmokeTestBase() {
    fun testSrcjar() {
        val tmp = KotlinTestUtils.tmpDir("CompilerSrcjarTest.testSrcjar.tmp")
        val src = File(testDataDir)

        val main = File(src, "Main.kt")
        val foo = File(src, "Foo.kt")
        val bar = File(src, "Bar.java")

        runCompiler("normal.compile", main.absolutePath, foo.absolutePath, bar.absolutePath, "-d", tmp.absolutePath)

        val foobar = File(tmp, "FooBar.srcjar")
        ZipOutputStream(foobar.outputStream().buffered()).use { srcjar ->
            srcjar.addZipEntry(foo.name, foo.readBytes())
            srcjar.addZipEntry(bar.name, bar.readBytes())
        }

        runCompiler("srcjar.compile", main.absolutePath, foobar.absolutePath, "-d", tmp.absolutePath)
    }

    private fun ZipOutputStream.addZipEntry(name: String, content: ByteArray) {
        putNextEntry(ZipEntry(name))
        write(content)
        closeEntry()
    }
}
