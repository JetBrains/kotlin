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
private val SUBSTRING_PATTERN = Pattern.compile("substring: (.*)")

public abstract class AbstractReplInterpreterTest : UsefulTestCase() {
    {
        System.setProperty("java.awt.headless", "true")
    }

    private enum class MatchType {
        EQUALS
        SUBSTRING
    }

    private data class OneLine(val code: String, val expected: String, val matchType: MatchType)

    private fun loadLines(file: File): List<OneLine> {
        val lines = ArrayDeque(file.readLines())

        val result = ArrayList<OneLine>()

        while (lines.isNotEmpty()) {
            val line = lines.poll()!!
            val matcher = START_PATTERN.matcher(line)
            assert(matcher.matches()) { "Line doesn't match start pattern: $line" }
            val code = matcher.group(2)!!

            if (lines.isNotEmpty()) {
                val substringMatcher = SUBSTRING_PATTERN.matcher(lines.peek()!!)
                if (substringMatcher.matches()) {
                    result.add(OneLine(code, substringMatcher.group(1)!!, MatchType.SUBSTRING))
                    lines.poll()
                    continue
                }
            }

            val value = StringBuilder()
            while (lines.isNotEmpty() && !START_PATTERN.matcher(lines.peek()!!).matches()) {
                value.appendln(lines.poll()!!)
            }

            result.add(OneLine(code, value.toString(), MatchType.EQUALS))
        }

        return result
    }

    protected fun doTest(path: String) {
        val configuration = JetTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.FULL_JDK)
        val repl = ReplInterpreter(getTestRootDisposable()!!, configuration)

        for ((code, expected, matchType) in loadLines(File(path))) {
            val expectedString = StringUtil.convertLineSeparators(expected).replaceFirst("\n$", "")

            val lineResult = repl.eval(code)
            val actual = when (lineResult.getType()) {
                ReplInterpreter.LineResultType.SUCCESS -> lineResult.getValue()?.toString() ?: ""
                ReplInterpreter.LineResultType.ERROR -> lineResult.getErrorText()
                ReplInterpreter.LineResultType.INCOMPLETE -> "incomplete"
            }
            val actualString = StringUtil.convertLineSeparators(actual).replaceFirst("\n$", "")

            when (matchType) {
                MatchType.EQUALS -> {
                    Assert.assertEquals("After evaluation of: $code", expectedString, actualString)
                }
                MatchType.SUBSTRING -> {
                    Assert.assertTrue("Evaluated result must contain substring: $expectedString, actual: $actualString, line: $code",
                                      expectedString in actualString)
                }
            }
        }
    }
}
