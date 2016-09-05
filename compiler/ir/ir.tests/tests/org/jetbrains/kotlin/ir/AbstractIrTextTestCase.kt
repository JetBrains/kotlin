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
import org.jetbrains.kotlin.ir.util.dumpTreesFromLineNumber
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
        val ignoreErrors = shouldIgnoreErrors(wholeFile)
        for ((testFile, irFile) in generateIrFilesAsSingleModule(testFiles, ignoreErrors)) {
            doTestIrFileAgainstExpectations(dir, testFile, irFile)
        }
    }

    protected fun doTestIrFileAgainstExpectations(dir: File, testFile: TestFile, irFile: IrFile) {
        val expectations = parseExpectations(dir, testFile)
        val irFileDump = irFile.dump()

        val expected = StringBuilder()
        val actual = StringBuilder()
        for (expectation in expectations.regexps) {
            expected.append(expectation.numberOfOccurrences).append(" ").append(expectation.needle).append("\n")
            val actualCount = StringUtil.findMatches(irFileDump, Pattern.compile("(" + expectation.needle + ")")).size
            actual.append(actualCount).append(" ").append(expectation.needle).append("\n")
        }

        for (irTreeFileLabel in expectations.irTreeFileLabels) {
            val actualTrees = irFile.dumpTreesFromLineNumber(irTreeFileLabel.lineNumber)
            KotlinTestUtils.assertEqualsToFile(irTreeFileLabel.expectedTextFile, actualTrees)
        }

        try {
            TestCase.assertEquals(irFileDump, expected.toString(), actual.toString())
        }
        catch (e: Throwable) {
            println(irFileDump)
            throw rethrow(e)
        }
    }

    internal class Expectations(val regexps: List<RegexpInText>, val irTreeFileLabels: List<IrTreeFileLabel>)

    internal class RegexpInText(val numberOfOccurrences: Int, val needle: String) {
        constructor(countStr: String, needle: String) : this(Integer.valueOf(countStr), needle)
    }

    internal class IrTreeFileLabel(val expectedTextFile: File, val lineNumber: Int)

    companion object {
        private val EXPECTED_OCCURRENCES_PATTERN = Regex("""^\s*//\s*(\d+)\s*(.*)$""")
        private val IR_FILE_TXT_PATTERN = Regex("""// IR_FILE: (.*)$""")

        internal fun parseExpectations(dir: File, testFile: TestFile): Expectations {
            val regexps = ArrayList<RegexpInText>()
            val treeFiles = ArrayList<IrTreeFileLabel>()

            for (line in testFile.content.split("\n")) {
                EXPECTED_OCCURRENCES_PATTERN.matchEntire(line)?.let { matchResult ->
                    regexps.add(RegexpInText(matchResult.groupValues[1], matchResult.groupValues[2].trim()))
                }
                ?: IR_FILE_TXT_PATTERN.find(line)?.let { matchResult ->
                    val fileName = matchResult.groupValues[1].trim()
                    val file = createExpectedTextFile(testFile, dir, fileName)
                    treeFiles.add(IrTreeFileLabel(file, 0))
                }
            }

            if (treeFiles.isEmpty()) {
                val file = createExpectedTextFile(testFile, dir, testFile.name.replace(".kt", ".txt"))
                treeFiles.add(IrTreeFileLabel(file, 0))
            }

            return Expectations(regexps, treeFiles)
        }
    }

}
