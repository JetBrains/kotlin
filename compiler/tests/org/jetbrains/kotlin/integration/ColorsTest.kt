/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.integration

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.isWindows
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PlainTextMessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

// This test checks that the compiler outputs ANSI escape codes enabling colors, bold text, etc when outputting warnings/errors.
// By default, the compiler does so on non-Windows platforms only if the output is a terminal (isatty returns 1 for stderr),
// but you can also pass a custom instance of PlainTextMessageRenderer and override that parameter.
class ColorsTest : TestCaseWithTmpdir() {
    fun testColorsDisabledByDefault() {
        doTest(MessageRenderer.WITHOUT_PATHS, false)
    }

    fun testColorsDisabledWithDefaultConstructor() {
        doTest(CustomRenderer(), false)
    }

    fun testColorsEnabledCustom() {
        doTest(CustomRenderer(true), true)
    }

    fun testColorsDisabledCustom() {
        doTest(CustomRenderer(false), false)
    }

    private fun doTest(renderer: MessageRenderer, colorsShouldBeEnabled: Boolean) {
        // Colors are currently disabled on Windows.
        if (isWindows) return

        // Create a source file which yields exactly one error when being compiled.
        File(tmpdir, "source.kt").writeText("val result: String = 42")

        val log = ByteArrayOutputStream()

        val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.FULL_JDK).apply {
            put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, PrintingMessageCollector(PrintStream(log), renderer, false))
            addKotlinSourceRoot(tmpdir.absolutePath)
            put(JVMConfigurationKeys.OUTPUT_DIRECTORY, tmpdir)
        }

        val environment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        // Compilation should return false, because there's one error.
        assertFalse(KotlinToJVMBytecodeCompiler.compileBunchOfSources(environment))

        val firstBytes = log.toByteArray().take(7)
        val logStartsWithColors = firstBytes.joinToString(" ") { it.toString(16) } == "1b 5b 31 3b 33 31 6d"
        val logStartsWithWordError = firstBytes == "error: ".map { it.code.toByte() }

        when {
            logStartsWithColors -> if (!colorsShouldBeEnabled) {
                fail("There should be no colors in the compiler log, but it seems that there are.")
            }
            logStartsWithWordError -> if (colorsShouldBeEnabled) {
                fail("There should be colors in the compiler log, but there aren't any.")
            }
            else -> {
                fail("The compiler log starts with something unexpected. Possibly the test needs to be updated.")
            }
        }
    }

    private class CustomRenderer : PlainTextMessageRenderer {
        constructor() : super()
        constructor(colorsShouldBeEnabled: Boolean) : super(colorsShouldBeEnabled)

        override fun getName(): String = "Test"

        // Do not output paths, so that the log will start with the word "error", so that we can investigate just the first few bytes
        // of the log and see if it's the color enabling ANSI codes, or the word "error".
        override fun getPath(location: CompilerMessageSourceLocation): String? = null
    }
}
