/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

private const val EMPTY_MAIN_FUN = "fun main() {}"

class NopackArgumentTest : TestCaseWithTmpdir() {

    fun testDefault() {
        nopackTest(
            expectedProduceKlibFile = true, expectedProduceKlibDir = false
        )
    }

    fun testNopackTrue() {
        nopackTest(
            nopackArgument = true, expectedProduceKlibFile = false, expectedProduceKlibDir = true
        )
    }

    fun testLegacyOverridesNopackDefault() {
        nopackTest(
            produceKlibDirArgument = true,
            expectedProduceKlibDir = true,
        )
        nopackTest(
            produceKlibFileArgument = true,
            expectedProduceKlibFile = true,
        )
    }

    fun testLegacyOverridesExplicitNopack() {
        nopackTest(
            nopackArgument = true,
            produceKlibDirArgument = true,
            expectedProduceKlibDir = true,
        )
        nopackTest(
            nopackArgument = true,
            produceKlibFileArgument = true,
            expectedProduceKlibFile = true,
        )
    }

    private fun compileAndGetDiagnostics(sourceFiles: List<File>, additionalArgs: List<String> = emptyList()): List<Diagnostic> {
        val args = makeCompilerArgs(sourceFiles, tmpdir) + additionalArgs
        val diagnostics = mutableListOf<Diagnostic>()
        val result = CompilerTestUtil.executeCompiler(K2JSCompiler(), args, LoggingMessageRenderer(diagnostics)).second
        assertEquals("Compilation failed with exit code $result", ExitCode.OK, result)
        return diagnostics
    }

    private fun makeCompilerArgs(sourceFiles: List<File>, jarFile: File): List<String> {
        return listOf(
            K2JSCompilerArguments::libraries.cliArgument,
            ForTestCompileRuntime.stdlibJsForTests().absolutePath,
            K2JSCompilerArguments::outputDir.cliArgument(jarFile.absolutePath),
            K2JSCompilerArguments::moduleName.cliArgument("main"),
            K2JSCompilerArguments::verbose.cliArgument,
        ) + sourceFiles.map { it.absolutePath }
    }

    private data class Diagnostic(
        val severity: CompilerMessageSeverity,
        val message: String,
        val location: CompilerMessageSourceLocation?,
    )

    private class LoggingMessageRenderer(val diagnostics: MutableList<Diagnostic>) : MessageRenderer {
        override fun renderPreamble(): String = ""

        override fun render(
            severity: CompilerMessageSeverity,
            message: String,
            location: CompilerMessageSourceLocation?,
        ): String {
            diagnostics.add(Diagnostic(severity, message, location))
            return ""
        }

        override fun renderUsage(usage: String): String = render(CompilerMessageSeverity.STRONG_WARNING, usage, null)

        override fun renderConclusion(): String = ""

        override fun getName(): String = "Redirector"
    }

    @Suppress("DEPRECATION")
    private fun nopackTest(
        expectedProduceKlibFile: Boolean? = null,
        expectedProduceKlibDir: Boolean? = null,
        nopackArgument: Boolean? = null,
        produceKlibFileArgument: Boolean? = null,
        produceKlibDirArgument: Boolean? = null,
    ) {
        val mainKt = tmpdir.resolve("main.kt").apply {
            writeText(EMPTY_MAIN_FUN)
        }

        val additionalArgs = listOfNotNull(
            produceKlibDirArgument?.let { K2JSCompilerArguments::irProduceKlibDir.cliArgument(it.toString()) },
            produceKlibFileArgument?.let { K2JSCompilerArguments::irProduceKlibFile.cliArgument(it.toString()) },
            nopackArgument?.let { K2JSCompilerArguments::nopack.cliArgument(it.toString()) },
        )

        val diagnostics = compileAndGetDiagnostics(listOf(mainKt), additionalArgs)
        val produceKlibFile = diagnostics.extractBooleanConfiguration("PRODUCE_KLIB_FILE")
        val produceKlibDir = diagnostics.extractBooleanConfiguration("PRODUCE_KLIB_DIR")
        assertEquals("PRODUCE_KLIB_FILE", expectedProduceKlibFile, produceKlibFile)
        assertEquals("PRODUCE_KLIB_DIR", expectedProduceKlibDir, produceKlibDir)
    }

    private fun List<Diagnostic>.extractBooleanConfiguration(key: String): Boolean? =
        flatMap { it.message.lines() }.firstOrNull { it.startsWith(key) }?.split(" ")?.get(1)?.toBoolean()

}
