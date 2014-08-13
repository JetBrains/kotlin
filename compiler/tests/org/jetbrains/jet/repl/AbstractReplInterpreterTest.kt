/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.repl

import com.intellij.openapi.util.text.StringUtil
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.jet.ConfigurationKind
import org.jetbrains.jet.JetTestUtils
import org.jetbrains.jet.TestJdkKind
import org.jetbrains.jet.cli.jvm.repl.ReplInterpreter
import java.io.File
import java.util.ArrayDeque
import java.util.ArrayList
import java.util.regex.Pattern
import org.junit.Assert

private val START_PATTERN = Pattern.compile(">>>( *)(.*)$")
private val INCOMPLETE_PATTERN = Pattern.compile("\\.\\.\\.( *)(.*)$")

private val INCOMPLETE_LINE_MESSAGE = "incomplete line"

public abstract class AbstractReplInterpreterTest : UsefulTestCase() {
    {
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
        val configuration = JetTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.FULL_JDK)
        val repl = ReplInterpreter(getTestRootDisposable()!!, configuration)

        for ((code, expected) in loadLines(File(path))) {
            val lineResult = repl.eval(code)
            val actual = when (lineResult.getType()) {
                ReplInterpreter.LineResultType.SUCCESS -> lineResult.getValue()?.toString() ?: ""
                ReplInterpreter.LineResultType.ERROR -> lineResult.getErrorText()
                ReplInterpreter.LineResultType.INCOMPLETE -> INCOMPLETE_LINE_MESSAGE
            }

            Assert.assertEquals(
                    "After evaluation of: $code",
                    StringUtil.convertLineSeparators(expected).replaceFirst("\n$", ""),
                    StringUtil.convertLineSeparators(actual).replaceFirst("\n$", "")
            )
        }
    }
}
