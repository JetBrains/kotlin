/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestJdkKind
import org.junit.Assert
import java.io.File
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

abstract class AbstractBytecodeTextTest : CodegenTestCase() {
    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        val isIgnored = InTextDirectivesUtils.isIgnoredTarget(backend, wholeFile)
        createEnvironmentWithMockJdkAndIdeaAnnotations(
            ConfigurationKind.ALL,
            files,
            TestJdkKind.MOCK_JDK,
            *listOfNotNull(writeJavaFiles(files)).toTypedArray()
        )
        loadMultiFiles(files)

        if (isMultiFileTest(files) && !InTextDirectivesUtils.isDirectiveDefined(wholeFile.readText(), "TREAT_AS_ONE_FILE")) {
            doTestMultiFile(files, !isIgnored)
        } else {
            val expected = readExpectedOccurrences(wholeFile.path)
            val actual = generateToText("helpers/")
            checkGeneratedTextAgainstExpectedOccurrences(actual, expected, backend, !isIgnored)
        }
    }

    private fun doTestMultiFile(files: List<TestFile>, reportProblems: Boolean) {
        val expectedOccurrencesByOutputFile = LinkedHashMap<String, List<OccurrenceInfo>>()
        for (file in files) {
            readExpectedOccurrencesForMultiFileTest(file, expectedOccurrencesByOutputFile)
        }

        val generated = generateEachFileToText()
        for (expectedOutputFile in expectedOccurrencesByOutputFile.keys) {
            assertTextWasGenerated(expectedOutputFile, generated)
            val generatedText = generated[expectedOutputFile]!!
            val expectedOccurrences = expectedOccurrencesByOutputFile[expectedOutputFile]!!
            checkGeneratedTextAgainstExpectedOccurrences(generatedText, expectedOccurrences, backend, reportProblems)
        }
    }

    protected fun readExpectedOccurrences(filename: String): List<OccurrenceInfo> {
        val result = ArrayList<OccurrenceInfo>()
        val lines = File(filename).readLines().dropLastWhile(String::isEmpty)
        var backend = TargetBackend.ANY
        for (line in lines) {
            if (line.contains(JVM_TEMPLATES)) backend = TargetBackend.JVM
            else if (line.contains(JVM_IR_TEMPLATES)) backend = TargetBackend.JVM_IR

            val matcher = EXPECTED_OCCURRENCES_PATTERN.matcher(line)
            if (matcher.matches()) {
                result.add(parseOccurrenceInfo(matcher, backend))
            }
        }

        return result
    }

    class OccurrenceInfo constructor(private val numberOfOccurrences: Int, private val needle: String, val backend: TargetBackend) {
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

        private fun isMultiFileTest(files: List<TestFile>): Boolean {
            var kotlinFiles = 0
            for (file in files) {
                if (file.name.endsWith(".kt")) {
                    kotlinFiles++
                }
            }
            return kotlinFiles > 1
        }

        fun checkGeneratedTextAgainstExpectedOccurrences(
            text: String,
            expectedOccurrences: List<OccurrenceInfo>,
            currentBackend: TargetBackend,
            reportProblems: Boolean
        ) {
            val expected = StringBuilder()
            val actual = StringBuilder()
            var lastBackend = TargetBackend.ANY
            for (info in expectedOccurrences) {
                if (lastBackend != info.backend) {
                    when (info.backend) {
                        TargetBackend.JVM -> JVM_TEMPLATES
                        TargetBackend.JVM_IR -> JVM_IR_TEMPLATES
                        else -> error("Common part should be first one: ${expectedOccurrences.joinToString("\n")}")
                    }.also {
                        actual.append("\n$it\n")
                        expected.append("\n$it\n")
                    }
                    lastBackend = info.backend
                }

                expected.append(info).append("\n")
                if (info.backend == TargetBackend.ANY || info.backend == currentBackend) {
                    actual.append(info.getActualOccurrence(text)).append("\n")
                } else {
                    actual.append(info).append("\n")
                }
            }

            try {
                Assert.assertEquals(text, expected.toString(), actual.toString())
            } catch (e: Throwable) {
                if (reportProblems) {
                    println(text)
                }
                throw e
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

        private const val JVM_TEMPLATES = "// JVM_TEMPLATES"

        private const val JVM_IR_TEMPLATES = "// JVM_IR_TEMPLATES"

        private fun readExpectedOccurrencesForMultiFileTest(file: TestFile, occurrenceMap: MutableMap<String, List<OccurrenceInfo>>) {
            var currentOccurrenceInfos: MutableList<OccurrenceInfo>? = null
            var backend = TargetBackend.ANY
            for (line in file.content.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                if (line.contains(JVM_TEMPLATES)) backend = TargetBackend.JVM
                else if (line.contains(JVM_IR_TEMPLATES)) backend = TargetBackend.JVM_IR

                val atOutputFileMatcher = AT_OUTPUT_FILE_PATTERN.matcher(line)
                if (atOutputFileMatcher.matches()) {
                    val outputFileName = atOutputFileMatcher.group(1)
                    if (occurrenceMap.containsKey(outputFileName)) {
                        throw AssertionError("${file.name}: Expected occurrences for output file $outputFileName were already provided")
                    }
                    currentOccurrenceInfos = ArrayList()
                    occurrenceMap[outputFileName] = currentOccurrenceInfos
                }

                val expectedOccurrencesMatcher = EXPECTED_OCCURRENCES_PATTERN.matcher(line)
                if (expectedOccurrencesMatcher.matches()) {
                    if (currentOccurrenceInfos == null) {
                        throw AssertionError("${file.name}: Should specify output file with '// @<OUTPUT_FILE_NAME>:' before expectations")
                    }
                    val occurrenceInfo = parseOccurrenceInfo(expectedOccurrencesMatcher, backend)
                    currentOccurrenceInfos.add(occurrenceInfo)
                }
            }
        }

        private fun parseOccurrenceInfo(matcher: Matcher, backend: TargetBackend): OccurrenceInfo {
            val numberOfOccurrences = Integer.parseInt(matcher.group(1))
            val needle = matcher.group(2)
            return OccurrenceInfo(numberOfOccurrences, needle, backend)
        }
    }
}
