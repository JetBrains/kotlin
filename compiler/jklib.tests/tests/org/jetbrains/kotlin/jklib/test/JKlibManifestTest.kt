/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jklib.test

import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jklib.K2JKlibCompiler
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Properties
import java.util.zip.ZipFile

class JKlibManifestTest {

    private val stdlibKlib: String
        get() = ForTestCompileRuntime.jklibStdlibForTests().path

    private fun compile(
        srcFile: File,
        outputKlib: File,
        moduleName: String,
        vararg extraArgs: String,
    ): Pair<String, ExitCode> {
        val args = mutableListOf(
            srcFile.path,
            "-d", outputKlib.path,
            "-module-name", moduleName,
            "-no-stdlib",
            "-Xklib=$stdlibKlib",
        ).apply { addAll(extraArgs) }
        return AbstractCliTest.executeCompilerGrabOutput(K2JKlibCompiler(), args)
    }

    private fun File.readZipManifestProperties(): Properties {
        return ZipFile(this).use { zip ->
            val entry = zip.getEntry("default/manifest")
            assertNotNull(entry, "Expected 'default/manifest' entry in KLIB zip archive")
            zip.getInputStream(entry).use { input ->
                Properties().apply { load(input) }
            }
        }
    }

    @Test
    fun testManifestAddendumInFullKlibCompilation(@TempDir tempDir: File) {
        val libSrc = File(tempDir, "Lib.kt").apply {
            writeText(
                """
                package test

                class Service {
                    fun serve(): String = "OK"
                }
                """.trimIndent()
            )
        }
        val manifestAddendum = File(tempDir, "manifest-addendum.properties").apply {
            writeText(
                """
                custom_foo=bar
                custom.number=42
                """.trimIndent()
            )
        }
        val outputKlib = File(tempDir, "libFull.klib")

        val result = compile(libSrc, outputKlib, "libFull", "-manifest", manifestAddendum.path)
        assertEquals(ExitCode.OK, result.second) { "Compilation failed: ${result.first}" }
        assertTrue(outputKlib.exists(), "Output KLIB file does not exist")

        val properties = outputKlib.readZipManifestProperties()
        assertEquals("bar", properties.getProperty("custom_foo"))
        assertEquals("42", properties.getProperty("custom.number"))
        assertEquals("libFull", properties.getProperty("unique_name"))
        assertEquals("JKLIB", properties.getProperty("builtins_platform"))
    }

    @Test
    fun testManifestAddendumInHeaderModeMetadataCompilation(@TempDir tempDir: File) {
        val libSrc = File(tempDir, "Lib.kt").apply {
            writeText(
                """
                package test

                class Service {
                    fun serve(): String = "OK"
                }
                """.trimIndent()
            )
        }
        val manifestAddendum = File(tempDir, "manifest-addendum.properties").apply {
            writeText(
                """
                header_custom_key=header_custom_val
                another_key=123
                """.trimIndent()
            )
        }
        val outputKlib = File(tempDir, "libHeader.klib")

        val result = compile(
            libSrc,
            outputKlib,
            "libHeader",
            "-Xheader-mode",
            "-Xheader-mode-type=compilation",
            "-manifest",
            manifestAddendum.path,
        )
        assertEquals(ExitCode.OK, result.second) { "Compilation failed: ${result.first}" }
        assertTrue(outputKlib.exists(), "Output KLIB file does not exist")

        val properties = outputKlib.readZipManifestProperties()
        assertEquals("header_custom_val", properties.getProperty("header_custom_key"))
        assertEquals("123", properties.getProperty("another_key"))
        assertEquals("libHeader", properties.getProperty("unique_name"))
        assertEquals("JKLIB", properties.getProperty("builtins_platform"))
    }

    @Test
    fun testManifestAddendumInDirectoryKlibCompilation(@TempDir tempDir: File) {
        val libSrc = File(tempDir, "Lib.kt").apply {
            writeText(
                """
                package test

                class Service {
                    fun serve(): String = "OK"
                }
                """.trimIndent()
            )
        }
        val manifestAddendum = File(tempDir, "manifest-addendum.properties").apply {
            writeText("dir_custom_key=dir_custom_val\n")
        }
        val outputKlibDir = File(tempDir, "libDir.klib")

        val result = compile(
            libSrc,
            outputKlibDir,
            "libDir",
            "-Xcompile-ir",
            "-manifest",
            manifestAddendum.path,
        )
        assertEquals(ExitCode.OK, result.second) { "Compilation failed: ${result.first}" }

        val manifestFile = File(outputKlibDir, "default/manifest")
        assertTrue(manifestFile.isFile, "Expected 'default/manifest' file in directory KLIB")

        val properties = manifestFile.inputStream().use { input ->
            Properties().apply { load(input) }
        }
        assertEquals("dir_custom_val", properties.getProperty("dir_custom_key"))
        assertEquals("libDir", properties.getProperty("unique_name"))
        assertEquals("JKLIB", properties.getProperty("builtins_platform"))
    }
}
