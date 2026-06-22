/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.io.unzipTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.assertThrows
import java.io.IOException
import java.nio.file.Files.createTempDirectory
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipOutputStream
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.outputStream

class UnzipTest {
    private lateinit var tmpDir: Path

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        tmpDir = createTempDirectory(testInfo.testClass.get().simpleName + "_" + testInfo.testMethod.get().name)
    }

    @OptIn(ExperimentalPathApi::class)
    @AfterEach
    fun tearDown() {
        tmpDir.deleteRecursively()
    }

    @Test
    fun testZipSlip() {
        // https://security.snyk.io/research/zip-slip-vulnerability
        val zipArchive = tmpDir.resolve("sneaky.klib")

        createMaliciousArchive(zipArchive)

        try {
            zipArchive.unzipTo(tmpDir.resolve("unpacked"))
        } catch (e: Exception) {
            if (e !is IOException && e.cause !is IOException) throw e
        }

        assert(!tmpDir.resolve("definitelySafe.txt").exists()) { "ZipSlip vulnerability found!" }
    }

    @Test
    fun testZipSlipValidation() {
        val zipArchive = tmpDir.resolve("sneaky.klib")

        createMaliciousArchive(zipArchive)

        assertThrows<ZipException> {
            zipArchive.unzipTo(tmpDir.resolve("unpacked"))
        }
    }
}

private fun createMaliciousArchive(file: Path) {
    file.outputStream().use { outputStream ->
        ZipOutputStream(outputStream).use { zipOutputStream ->
            zipOutputStream.putNextEntry(ZipEntry("../definitelySafe.txt"))
            zipOutputStream.closeEntry()
        }
    }
}
