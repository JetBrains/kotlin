/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.repl

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.script.loadScriptingPlugin
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplInterpreter
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.configuration.ConsoleReplConfiguration
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.junit.Assert
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.io.PrintWriter
import java.util.*
import java.util.regex.Pattern

// Switch this flag to render bytecode after each line in the REPL test. Useful for debugging verify errors or other codegen problems
private val DUMP_BYTECODE = false

private val START_PATTERN = Pattern.compile(">>>( *)(.*)$")
private val INCOMPLETE_PATTERN = Pattern.compile("\\.\\.\\.( *)(.*)$")
private val TRAILING_NEWLINE_REGEX = Regex("\n$")

private val INCOMPLETE_LINE_MESSAGE = "incomplete line"
private val HISTORY_MISMATCH_LINE_MESSAGE = "history mismatch"

abstract class AbstractReplInterpreterTest : KtUsefulTestCase() {
    init {
        System.setProperty("java.awt.headless", "true")
    }

    private data class OneLine(val code: String, val expected: String)

    private fun loadLines(file: File): List<OneLine> {
        val lines = ArrayDeque(file.readLines())

        val result = ArrayList<OneLine>()

        while (lines.isNotEmpty()) {
            val line = lines.poll()!!
            var matcher = START_PATTERN.matcher(line)
            if (!matcher.matches()) {
                matcher = INCOMPLETE_PATTERN.matcher(line)
            }
            assert(matcher.matches()) { "Line doesn't begin with \">>>\" or \"...\": $line" }
            val code = matcher.group(2)!!

            if (lines.isNotEmpty()) {
                val nextLine = lines.peek()!!

                val incompleteMatcher = INCOMPLETE_PATTERN.matcher(nextLine)
                if (incompleteMatcher.matches()) {
                    result.add(OneLine(code, INCOMPLETE_LINE_MESSAGE))
                    continue
                }
            }

            val value = StringBuilder()
            while (lines.isNotEmpty() && !START_PATTERN.matcher(lines.peek()!!).matches()) {
                value.appendLine(lines.poll()!!)
            }

            result.add(OneLine(code, value.toString()))
        }

        return result
    }

    internal fun <T> captureOutErrRet(body: () -> T): Triple<String, String, T> {
        val outStream = ByteArrayOutputStream()
        val errStream = ByteArrayOutputStream()
        val prevOut = System.out
        val prevErr = System.err
        System.setOut(PrintStream(outStream))
        System.setErr(PrintStream(errStream))
        val ret = try {
            body()
        } finally {
            System.out.flush()
            System.err.flush()
            System.setOut(prevOut)
            System.setErr(prevErr)
        }
        return Triple(outStream.toString().trim(), errStream.toString().trim(), ret)
    }

    protected fun doTest(path: String) {
        val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK)
        loadScriptingPlugin(configuration)
        val projectEnvironment =
            KotlinCoreEnvironment.ProjectEnvironment(
                testRootDisposable,
                KotlinCoreEnvironment.getOrCreateApplicationEnvironmentForTests(testRootDisposable, configuration),
                configuration
            )
        val repl = ReplInterpreter(
            projectEnvironment, configuration,
            ConsoleReplConfiguration()
        )

        for ((code, expected) in loadLines(File(path))) {
            val (output, _, lineResult) = captureOutErrRet { repl.eval(code) }

            if (DUMP_BYTECODE) {
                repl.dumpClasses(PrintWriter(System.out))
            }

            val actual = when (lineResult) {
                is ReplEvalResult.ValueResult -> lineResult.value.toString()
                is ReplEvalResult.Error.CompileTime -> output
                is ReplEvalResult.Error -> lineResult.message
                is ReplEvalResult.Incomplete -> INCOMPLETE_LINE_MESSAGE
                is ReplEvalResult.UnitResult -> ""
                is ReplEvalResult.HistoryMismatch -> HISTORY_MISMATCH_LINE_MESSAGE
            }

            Assert.assertEquals(
                "After evaluation of: $code",
                StringUtil.convertLineSeparators(expected).replaceFirst(TRAILING_NEWLINE_REGEX, ""),
                StringUtil.convertLineSeparators(actual).replaceFirst(TRAILING_NEWLINE_REGEX, "")
            )
        }
    }
}
