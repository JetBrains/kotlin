/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.integration

import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import java.io.File

/**
 * Tests that [JvmCodegenUtil.getMappingFileName] sanitizes all Windows-forbidden characters
 * from module names so that `.kotlin_module` files can be created on any platform.
 */
class ModuleNameSanitizationTest : KotlinIntegrationTestBase() {

    fun testColonIsReplaced() {
        assertEquals("META-INF/com.example_app.kotlin_module", JvmCodegenUtil.getMappingFileName("com.example:app"))
    }

    fun testLessThanIsReplaced() {
        assertEquals("META-INF/a_b.kotlin_module", JvmCodegenUtil.getMappingFileName("a<b"))
    }

    fun testGreaterThanIsReplaced() {
        assertEquals("META-INF/a_b.kotlin_module", JvmCodegenUtil.getMappingFileName("a>b"))
    }

    fun testDoubleQuoteIsReplaced() {
        assertEquals("META-INF/a_b.kotlin_module", JvmCodegenUtil.getMappingFileName("a\"b"))
    }

    fun testForwardSlashIsReplaced() {
        assertEquals("META-INF/a_b.kotlin_module", JvmCodegenUtil.getMappingFileName("a/b"))
    }

    fun testBackslashIsReplaced() {
        assertEquals("META-INF/a_b.kotlin_module", JvmCodegenUtil.getMappingFileName("a\\b"))
    }

    fun testPipeIsReplaced() {
        assertEquals("META-INF/a_b.kotlin_module", JvmCodegenUtil.getMappingFileName("a|b"))
    }

    fun testQuestionMarkIsReplaced() {
        assertEquals("META-INF/a_b.kotlin_module", JvmCodegenUtil.getMappingFileName("a?b"))
    }

    fun testAsteriskIsReplaced() {
        assertEquals("META-INF/a_b.kotlin_module", JvmCodegenUtil.getMappingFileName("a*b"))
    }

    fun testPercentIsReplaced() {
        assertEquals("META-INF/a_b.kotlin_module", JvmCodegenUtil.getMappingFileName("a%b"))
    }

    fun testNullControlCharIsReplaced() {
        assertEquals("META-INF/a_b.kotlin_module", JvmCodegenUtil.getMappingFileName("a\u0000b"))
    }

    fun testControlCharIsReplaced() {
        assertEquals("META-INF/a_b.kotlin_module", JvmCodegenUtil.getMappingFileName("a\u001Fb"))
    }

    fun testGradleStyleModuleName() {
        assertEquals(
            "META-INF/com.example_my-project.kotlin_module",
            JvmCodegenUtil.getMappingFileName("com.example:my-project")
        )
    }

    fun testCombinedForbiddenChars() {
        assertEquals(
            "META-INF/com.example_my_app_.kotlin_module",
            JvmCodegenUtil.getMappingFileName("com.example:my<app>")
        )
    }

    fun testPlainNameIsUnchanged() {
        assertEquals("META-INF/my_module.kotlin_module", JvmCodegenUtil.getMappingFileName("my_module"))
    }

    // --- Integration test: actual compilation with a forbidden module name ---

    fun testCompilationWithForbiddenCharsInModuleName() {
        val sourceFile = File(tmpdir, "test.kt")
        sourceFile.writeText("fun hello() = \"Hello\"\n")

        val outputDir = File(tmpdir, "out").also { it.mkdirs() }
        val moduleName = "com.example:X<>:\"/\\|?*%"

        val (stdout, exitCode) = AbstractCliTest.executeCompilerGrabOutput(
            K2JVMCompiler(),
            listOf(
                sourceFile.path,
                "-module-name", moduleName,
                "-d", outputDir.path,
            )
        )
        assertEquals("Compilation failed:\n$stdout", ExitCode.OK, exitCode)

        val expectedModuleFile = File(outputDir, JvmCodegenUtil.getMappingFileName(moduleName))
        assertTrue(
            "Expected .kotlin_module file not found at ${expectedModuleFile.path}",
            expectedModuleFile.exists()
        )
    }
}
