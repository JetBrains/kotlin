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

package org.jetbrains.kotlin.ir

import com.intellij.openapi.util.text.StringUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.rethrow
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.*
import java.util.regex.Pattern

abstract class AbstractIrTextTestCase : AbstractIrGeneratorTestCase() {
    override fun doTest(wholeFile: File, testFiles: List<TestFile>) {
        val dir = wholeFile.parentFile
        for ((testFile, irFile) in generateIrFilesAsSingleModule(testFiles)) {
            doTestIrFileAgainstExpectations(dir, testFile, irFile)
        }
    }

    protected fun doTestIrFileAgainstExpectations(dir: File, testFile: TestFile, irFile: IrFile) {
        val expectations = parseExpectations(dir, testFile)
        val irFileDump = irFile.dump()

        expectations.irExpectedTextFile?.let { expectFile ->
            KotlinTestUtils.assertEqualsToFile(expectFile, irFileDump)
        }

        val expected = StringBuilder()
        val actual = StringBuilder()
        for (expectation in expectations.regexps) {
            expected.append(expectation.numberOfOccurrences).append(" ").append(expectation.needle).append("\n")
            val actualCount = StringUtil.findMatches(irFileDump, Pattern.compile("(" + expectation.needle + ")")).size
            actual.append(actualCount).append(" ").append(expectation.needle).append("\n")
        }

        try {
            TestCase.assertEquals(irFileDump, expected.toString(), actual.toString())
        }
        catch (e: Throwable) {
            println(irFileDump)
            throw rethrow(e)
        }
    }

    protected class Expectations(val irExpectedTextFile: File?, val regexps: List<RegexpInText>)

    protected class RegexpInText(val numberOfOccurrences: Int, val needle: String) {
        constructor(countStr: String, needle: String) : this(Integer.valueOf(countStr), needle)
    }

    companion object {
        private val EXPECTED_OCCURRENCES_PATTERN = Regex("""^\s*//\s*(\d+)\s*(.*)$""")
        private val EXPECT_TXT_PATTERN = Regex("""^\s*//\s*IR_FILE_TXT\s+(.*)$""")

        private fun parseExpectations(dir: File, testFile: TestFile): Expectations {
            var expectedTextFileName: String? = null
            val regexps = ArrayList<RegexpInText>()
            for (line in testFile.content.split("\n")) {
                EXPECTED_OCCURRENCES_PATTERN.matchEntire(line)?.let { matchResult ->
                    regexps.add(RegexpInText(matchResult.groupValues[1], matchResult.groupValues[2]))
                }
                EXPECT_TXT_PATTERN.matchEntire(line)?.let { matchResult ->
                    expectedTextFileName = matchResult.groupValues[1]
                }
            }

            val expectedTextFile: File? = expectedTextFileName?.let {
                val textFile = File(dir, expectedTextFileName)
                if (!textFile.exists()) {
                    TestCase.assertTrue("Can't create an IR expected text file: ${textFile.absolutePath}", textFile.createNewFile())
                    PrintWriter(FileWriter(textFile)).use {
                        it.println("$expectedTextFileName: new IR expected text file for ${testFile.name}")
                    }
                }
                textFile
            }

            return Expectations(expectedTextFile, regexps)
        }
    }

}
