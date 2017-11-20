/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.utils.rethrow
import org.junit.Assert
import java.io.File
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

abstract class AbstractBytecodeTextTest : CodegenTestCase() {

    @Throws(Exception::class)
    override fun doMultiFileTest(wholeFile: File, files: List<CodegenTestCase.TestFile>, javaFilesDir: File?) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL, files, javaFilesDir)
        loadMultiFiles(files)

        if (isMultiFileTest(files) && !InTextDirectivesUtils.isDirectiveDefined(wholeFile.readText(), "TREAT_AS_ONE_FILE")) {
            doTestMultiFile(files)
        }
        else {
            val expected = readExpectedOccurrences(wholeFile.path)
            val actual = generateToText()
            checkGeneratedTextAgainstExpectedOccurrences(actual, expected)
        }
    }

    @Throws(Exception::class)
    private fun doTestMultiFile(files: List<CodegenTestCase.TestFile>) {
        val expectedOccurrencesByOutputFile = LinkedHashMap<String, List<OccurrenceInfo>>()
        for (file in files) {
            readExpectedOccurrencesForMultiFileTest(file, expectedOccurrencesByOutputFile)
        }

        val generated = generateEachFileToText()
        for (expectedOutputFile in expectedOccurrencesByOutputFile.keys) {
            assertTextWasGenerated(expectedOutputFile, generated)
            val generatedText = generated[expectedOutputFile]!!
            val expectedOccurrences = expectedOccurrencesByOutputFile[expectedOutputFile]!!
            checkGeneratedTextAgainstExpectedOccurrences(generatedText, expectedOccurrences)
        }
    }

    @Throws(Exception::class)
    protected fun readExpectedOccurrences(filename: String): List<OccurrenceInfo> {
        val result = ArrayList<OccurrenceInfo>()
        val lines = FileUtil.loadFile(File(filename), Charsets.UTF_8.name(), true).split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        for (line in lines) {
            val matcher = EXPECTED_OCCURRENCES_PATTERN.matcher(line)
            if (matcher.matches()) {
                result.add(parseOccurrenceInfo(matcher))
            }
        }

        return result
    }

    class OccurrenceInfo constructor(private val numberOfOccurrences: Int, private val needle: String) {
        fun getActualOccurrence(text: String): String? {
            val actualCount = StringUtil.findMatches(text, Pattern.compile("($needle)")).size
            return "$actualCount $needle"
        }

        override fun toString(): String {
            return "$numberOfOccurrences $needle"
        }
    }

    companion object {
        private val AT_OUTPUT_FILE_PATTERN = Pattern.compile("^\\s*//\\s*@(.*):$")
        private val EXPECTED_OCCURRENCES_PATTERN = Pattern.compile("^\\s*//\\s*(\\d+)\\s*(.*)$")

        private fun isMultiFileTest(files: List<CodegenTestCase.TestFile>): Boolean {
            var kotlinFiles = 0
            for (file in files) {
                if (file.name.endsWith(".kt")) {
                    kotlinFiles++
                }
            }
            return kotlinFiles > 1
        }

        fun checkGeneratedTextAgainstExpectedOccurrences(text: String,
                                                         expectedOccurrences: List<OccurrenceInfo>) {
            val expected = StringBuilder()
            val actual = StringBuilder()

            for (info in expectedOccurrences) {
                expected.append(info).append("\n")
                actual.append(info.getActualOccurrence(text)).append("\n")
            }

            try {
                Assert.assertEquals(text, expected.toString(), actual.toString())
            }
            catch (e: Throwable) {
                println(text)
                throw rethrow(e)
            }

        }

        private fun assertTextWasGenerated(expectedOutputFile: String, generated: Map<String, String>) {
            if (!generated.containsKey(expectedOutputFile)) {
                val failMessage = StringBuilder()
                failMessage.append("Missing output file ").append(expectedOutputFile).append(", got ").append(generated.size).append(": ")
                for (generatedFile in generated.keys) {
                    failMessage.append(generatedFile).append(" ")
                }
                Assert.fail(failMessage.toString())
            }
        }

        private fun readExpectedOccurrencesForMultiFileTest(
                file: CodegenTestCase.TestFile,
                occurrenceMap: MutableMap<String, List<OccurrenceInfo>>) {
            var currentOccurrenceInfos: MutableList<OccurrenceInfo>? = null
            for (line in file.content.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                val atOutputFileMatcher = AT_OUTPUT_FILE_PATTERN.matcher(line)
                if (atOutputFileMatcher.matches()) {
                    val outputFileName = atOutputFileMatcher.group(1)
                    if (occurrenceMap.containsKey(outputFileName)) {
                        throw AssertionError(
                                file.name + ": Expected occurrences for output file " + outputFileName + " were already provided")
                    }
                    currentOccurrenceInfos = ArrayList<OccurrenceInfo>()
                    occurrenceMap.put(outputFileName, currentOccurrenceInfos)
                }

                val expectedOccurrencesMatcher = EXPECTED_OCCURRENCES_PATTERN.matcher(line)
                if (expectedOccurrencesMatcher.matches()) {
                    if (currentOccurrenceInfos == null) {
                        throw AssertionError(
                                file.name + ": Should specify output file with '// @<OUTPUT_FILE_NAME>:' before expectations")
                    }
                    val occurrenceInfo = parseOccurrenceInfo(expectedOccurrencesMatcher)
                    currentOccurrenceInfos.add(occurrenceInfo)
                }
            }
        }

        private fun parseOccurrenceInfo(matcher: Matcher): OccurrenceInfo {
            val numberOfOccurrences = Integer.parseInt(matcher.group(1))
            val needle = matcher.group(2)
            return OccurrenceInfo(numberOfOccurrences, needle)
        }
    }
}
