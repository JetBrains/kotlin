/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.test.Assertions
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File
import java.util.ArrayList
import java.util.regex.Matcher
import java.util.regex.Pattern

private val AT_OUTPUT_FILE_PATTERN = Pattern.compile("^\\s*//\\s*@(.*):$")
private val EXPECTED_OCCURRENCES_PATTERN = Pattern.compile("^\\s*//\\s*(\\d+)\\s*(.*)$")
private const val JVM_TEMPLATES = "// JVM_TEMPLATES"
private const val JVM_IR_TEMPLATES = "// JVM_IR_TEMPLATES"
private const val JVM_IR_TEMPLATES_WITH_INLINE_SCOPES = "// JVM_IR_TEMPLATES_WITH_INLINE_SCOPES"

class OccurrenceInfo constructor(
    private val numberOfOccurrences: Int,
    private val needle: String,
    val backend: TargetBackend,
    val inlineScopesNumbersEnabled: Boolean
) {
    private val pattern = Pattern.compile("($needle)")

    fun getActualOccurrence(text: String): String {
        val actualCount = StringUtil.findMatches(text, pattern).size
        return "$actualCount $needle"
    }

    override fun toString(): String {
        return "$numberOfOccurrences $needle"
    }
}

fun readExpectedOccurrences(filename: String): List<OccurrenceInfo> {
    val lines = File(filename).readLines().dropLastWhile(String::isEmpty)
    return readExpectedOccurrences(lines)
}

fun readExpectedOccurrences(lines: List<String>): List<OccurrenceInfo> {
    val result = ArrayList<OccurrenceInfo>()
    var backend = TargetBackend.ANY
    var inlineScopesNumbersEnabled = false
    for (line in lines) {
        when {
            line.contains(JVM_IR_TEMPLATES_WITH_INLINE_SCOPES) -> {
                backend = TargetBackend.JVM_IR
                inlineScopesNumbersEnabled = true
            }
            line.contains(JVM_IR_TEMPLATES) -> {
                backend = TargetBackend.JVM_IR
                inlineScopesNumbersEnabled = false
            }
            line.contains(JVM_TEMPLATES) -> {
                backend = TargetBackend.JVM
                inlineScopesNumbersEnabled = false
            }
        }

        val matcher = EXPECTED_OCCURRENCES_PATTERN.matcher(line)
        if (matcher.matches()) {
            result.add(parseOccurrenceInfo(matcher, backend, inlineScopesNumbersEnabled))
        }
    }

    return result
}

fun readExpectedOccurrencesForMultiFileTest(
    fileName: String,
    fileContent: String,
    withGeneratedFile: MutableMap<String, List<OccurrenceInfo>>,
    global: MutableList<OccurrenceInfo>
) {
    var currentOccurrenceInfos: MutableList<OccurrenceInfo> = global
    var backend = TargetBackend.ANY
    var inlineScopesNumbersEnabled = false
    for (line in fileContent.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
        when {
            line.contains(JVM_IR_TEMPLATES_WITH_INLINE_SCOPES) -> {
                backend = TargetBackend.JVM_IR
                inlineScopesNumbersEnabled = true
            }
            line.contains(JVM_IR_TEMPLATES) -> {
                backend = TargetBackend.JVM_IR
                inlineScopesNumbersEnabled = false
            }
            line.contains(JVM_TEMPLATES) -> {
                backend = TargetBackend.JVM
                inlineScopesNumbersEnabled = false
            }
        }


        val atOutputFileMatcher = AT_OUTPUT_FILE_PATTERN.matcher(line)
        if (atOutputFileMatcher.matches()) {
            val outputFileName = atOutputFileMatcher.group(1)
            if (withGeneratedFile.containsKey(outputFileName)) {
                throw AssertionError("${fileName}: Expected occurrences for output file $outputFileName were already provided")
            }
            currentOccurrenceInfos = ArrayList()
            withGeneratedFile[outputFileName] = currentOccurrenceInfos
        }

        val expectedOccurrencesMatcher = EXPECTED_OCCURRENCES_PATTERN.matcher(line)
        if (expectedOccurrencesMatcher.matches()) {
            val occurrenceInfo = parseOccurrenceInfo(expectedOccurrencesMatcher, backend, inlineScopesNumbersEnabled)
            currentOccurrenceInfos.add(occurrenceInfo)
        }
    }
}

private fun parseOccurrenceInfo(matcher: Matcher, backend: TargetBackend, inlineScopesNumbersEnabled: Boolean): OccurrenceInfo {
    val numberOfOccurrences = Integer.parseInt(matcher.group(1))
    val needle = matcher.group(2)
    return OccurrenceInfo(numberOfOccurrences, needle, backend, inlineScopesNumbersEnabled)
}

fun checkGeneratedTextAgainstExpectedOccurrences(
    text: String,
    expectedOccurrences: List<OccurrenceInfo>,
    currentBackend: TargetBackend,
    reportProblems: Boolean,
    assertions: Assertions,
    inlineScopesNumbersEnabled: Boolean = false
) {
    val expected = StringBuilder()
    val actual = StringBuilder()
    var lastBackend = TargetBackend.ANY
    val noScopesNumbersEntries = expectedOccurrences.none { it.inlineScopesNumbersEnabled }
    for (info in expectedOccurrences) {
        if (lastBackend != info.backend) {
            when (info.backend) {
                TargetBackend.JVM -> JVM_TEMPLATES
                TargetBackend.JVM_IR -> {
                    if (inlineScopesNumbersEnabled) {
                        JVM_IR_TEMPLATES_WITH_INLINE_SCOPES
                    } else {
                        JVM_IR_TEMPLATES
                    }
                }
                else -> error("Common part should be first one: ${expectedOccurrences.joinToString("\n")}")
            }.also {
                actual.append("\n$it\n")
                expected.append("\n$it\n")
            }
            lastBackend = info.backend
        }

        expected.append(info).append("\n")
        val hasAppropriateScopesNumbersSetting = info.inlineScopesNumbersEnabled == inlineScopesNumbersEnabled || noScopesNumbersEntries
        if (info.backend == TargetBackend.ANY || (info.backend == currentBackend && hasAppropriateScopesNumbersSetting)) {
            actual.append(info.getActualOccurrence(text)).append("\n")
        } else {
            actual.append(info).append("\n")
        }
    }

    try {
        assertions.assertEquals(expected.toString(), actual.toString()) { text }
    } catch (e: Throwable) {
        if (reportProblems) {
            println(text)
        }
        throw e
    }
}

fun assertTextWasGenerated(expectedOutputFile: String, generated: Map<String, String>, assertions: Assertions) {
    if (!generated.containsKey(expectedOutputFile)) {
        val failMessage = StringBuilder()
        failMessage.append("Missing output file ").append(expectedOutputFile).append(", got ").append(generated.size).append(": ")
        for (generatedFile in generated.keys) {
            failMessage.append(generatedFile).append(" ")
        }
        assertions.fail { failMessage.toString() }
    }
}
