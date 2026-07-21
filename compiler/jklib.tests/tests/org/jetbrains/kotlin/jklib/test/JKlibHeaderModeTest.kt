/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jklib.test

import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jklib.K2JKlibCompiler
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipFile

class JKlibHeaderModeTest {

    private val stdlibKlib: String
        get() = ForTestCompileRuntime.jklibStdlibForTests().path

    private fun compile(
        srcFile: File,
        outputKlib: File,
        moduleName: String,
        vararg extraArgs: String
    ): Pair<String, ExitCode> {
        val args = mutableListOf(
            srcFile.path,
            "-d", outputKlib.path,
            "-module-name", moduleName,
            "-no-stdlib",
            "-Xklib=$stdlibKlib"
        ).apply { addAll(extraArgs) }
        return AbstractCliTest.executeCompilerGrabOutput(K2JKlibCompiler(), args)
    }

    private fun File.hasAnyIrEntries(): Boolean {
        return ZipFile(this).use { zip ->
            zip.entries().asSequence().any { 
                it.name.contains("/ir/") || it.name.contains("/ir_inlinable_functions") || it.name.startsWith("ir/")
            }
        }
    }

    private fun File.getAllEntryNames(): List<String> {
        return ZipFile(this).use { zip ->
            zip.entries().asSequence().map { it.name }.toList()
        }
    }

    @Test
    fun testHeaderModeKlibWithoutInlinesOmitsIr(@TempDir tempDir: File) {
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
        val outputKlib = File(tempDir, "libNoIr.klib")

        val result = compile(libSrc, outputKlib, "libNoIr", "-Xheader-mode", "-Xheader-mode-type=compilation")
        assertEquals(ExitCode.OK, result.second) { "Compilation failed: ${result.first}" }

        assertTrue(outputKlib.exists(), "Output KLIB file does not exist")
        assertFalse(outputKlib.hasAnyIrEntries()) {
            "KLIB compiled without inline functions should NOT contain IR entries. Found entries: ${outputKlib.getAllEntryNames()}"
        }
    }

    @Test
    fun testHeaderModeKlibWithInlineFunctionPreservesIr(@TempDir tempDir: File) {
        val libSrc = File(tempDir, "LibInline.kt").apply {
            writeText(
                """
                package test

                inline fun inlineHelper(): String = "OK"
                """.trimIndent()
            )
        }
        val outputKlib = File(tempDir, "libWithIr.klib")

        val result = compile(libSrc, outputKlib, "libWithIr", "-Xheader-mode", "-Xheader-mode-type=compilation")
        assertEquals(ExitCode.OK, result.second) { "Compilation failed: ${result.first}" }

        assertTrue(outputKlib.exists(), "Output KLIB file does not exist")
        assertTrue(outputKlib.hasAnyIrEntries()) {
            "KLIB compiled with inline functions MUST contain IR entries. Found entries: ${outputKlib.getAllEntryNames()}"
        }
    }

    @Test
    fun testHeaderModeKlibWithInlinePropertyAccessorPreservesIr(@TempDir tempDir: File) {
        val libSrc = File(tempDir, "LibInlineProp.kt").apply {
            writeText(
                """
                package test

                val inlineProp: String
                    inline get() = "OK"
                """.trimIndent()
            )
        }
        val outputKlib = File(tempDir, "libInlineProp.klib")

        val result = compile(libSrc, outputKlib, "libInlineProp", "-Xheader-mode", "-Xheader-mode-type=compilation")
        assertEquals(ExitCode.OK, result.second) { "Compilation failed: ${result.first}" }

        assertTrue(outputKlib.exists(), "Output KLIB file does not exist")
        assertTrue(outputKlib.hasAnyIrEntries()) {
            "KLIB compiled with inline property accessors MUST contain IR entries. Found entries: ${outputKlib.getAllEntryNames()}"
        }
    }

    @Test
    fun testHeaderModeKlibWithValueClassPreservesIr(@TempDir tempDir: File) {
        val libSrc = File(tempDir, "LibValueClass.kt").apply {
            writeText(
                """
                package test

                @JvmInline
                value class MyValue(val value: Int)
                """.trimIndent()
            )
        }
        val outputKlib = File(tempDir, "libValueClass.klib")

        val result = compile(libSrc, outputKlib, "libValueClass", "-Xheader-mode", "-Xheader-mode-type=compilation")
        assertEquals(ExitCode.OK, result.second) { "Compilation failed: ${result.first}" }

        assertTrue(outputKlib.exists(), "Output KLIB file does not exist")
        assertTrue(outputKlib.hasAnyIrEntries()) {
            "KLIB compiled with value classes MUST contain IR entries. Found entries: ${outputKlib.getAllEntryNames()}"
        }
    }

    @Test
    fun testDownstreamCompilationAgainstHeaderKlib(@TempDir tempDir: File) {
        val libSrc = File(tempDir, "Lib.kt").apply {
            writeText(
                """
                package test

                class Service {
                    fun serve(): String = "OK"
                }

                inline fun helper(): String = "OK"
                """.trimIndent()
            )
        }
        val libKlib = File(tempDir, "libHeader.klib")

        val libResult = compile(libSrc, libKlib, "libHeader", "-Xheader-mode", "-Xheader-mode-type=compilation")
        assertEquals(ExitCode.OK, libResult.second) { "Compilation of library failed: ${libResult.first}" }

        val appSrc = File(tempDir, "App.kt").apply {
            writeText(
                """
                package app

                import test.Service
                import test.helper

                fun useService() {
                    val s = Service()
                    s.serve()
                    helper()
                }
                """.trimIndent()
            )
        }
        val appKlib = File(tempDir, "app.klib")

        val appResult = AbstractCliTest.executeCompilerGrabOutput(
            K2JKlibCompiler(),
            listOf(
                appSrc.path,
                "-d", appKlib.path,
                "-module-name", "app",
                "-no-stdlib",
                "-Xklib=$stdlibKlib${File.pathSeparator}${libKlib.path}"
            )
        )
        assertEquals(ExitCode.OK, appResult.second) { "Downstream compilation against header KLIB failed: ${appResult.first}" }
    }

    @Test
    fun testInvalidHeaderModeTypeReportsError(@TempDir tempDir: File) {
        val libSrc = File(tempDir, "Lib.kt").apply {
            writeText("package test\nclass Service")
        }
        val outputKlib = File(tempDir, "lib.klib")

        val result = compile(libSrc, outputKlib, "lib", "-Xheader-mode", "-Xheader-mode-type=invalid_type")
        assertEquals(ExitCode.COMPILATION_ERROR, result.second) {
            "Expected COMPILATION_ERROR for invalid -Xheader-mode-type, but got exit code ${result.second}"
        }
    }
}
