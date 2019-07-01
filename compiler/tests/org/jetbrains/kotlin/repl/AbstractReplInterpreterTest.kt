/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.repl

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.script.loadScriptingPlugin
import org.jetbrains.kotlin.scripting.repl.ReplInterpreter
import org.jetbrains.kotlin.scripting.repl.configuration.ConsoleReplConfiguration
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.junit.Assert
import java.io.File
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
                value.appendln(lines.poll()!!)
            }

            result.add(OneLine(code, value.toString()))
        }

        return result
    }

    protected fun doTest(path: String) {
        val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK)
        loadScriptingPlugin(configuration)
        val repl = ReplInterpreter(
            testRootDisposable, configuration,
            ConsoleReplConfiguration()
        )

        for ((code, expected) in loadLines(File(path))) {
            val lineResult = repl.eval(code)

            if (DUMP_BYTECODE) {
                repl.dumpClasses(PrintWriter(System.out))
            }

            val actual = when (lineResult) {
                is ReplEvalResult.ValueResult -> lineResult.value.toString()
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
