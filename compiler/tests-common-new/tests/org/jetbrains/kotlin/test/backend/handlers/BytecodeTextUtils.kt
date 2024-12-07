/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.test.Assertions
import java.util.ArrayList
import java.util.regex.Matcher
import java.util.regex.Pattern

private val AT_OUTPUT_FILE_PATTERN = Pattern.compile("^\\s*//\\s*@(.*):$")
private val EXPECTED_OCCURRENCES_PATTERN = Pattern.compile("^\\s*//\\s*(\\d+)\\s*(.*)$")
private const val JVM_IR_TEMPLATES = "// JVM_IR_TEMPLATES"
private const val JVM_IR_TEMPLATES_WITH_INLINE_SCOPES = "// JVM_IR_TEMPLATES_WITH_INLINE_SCOPES"

internal class OccurrenceInfo(
    private val numberOfOccurrences: Int,
    private val needle: String,
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

internal fun readExpectedOccurrences(lines: List<String>): List<OccurrenceInfo> {
    val result = ArrayList<OccurrenceInfo>()
    var inlineScopesNumbersEnabled = false
    for (line in lines) {
        when {
            line.contains(JVM_IR_TEMPLATES_WITH_INLINE_SCOPES) -> inlineScopesNumbersEnabled = true
            line.contains(JVM_IR_TEMPLATES) -> inlineScopesNumbersEnabled = false
        }

        val matcher = EXPECTED_OCCURRENCES_PATTERN.matcher(line)
        if (matcher.matches()) {
            result.add(parseOccurrenceInfo(matcher, inlineScopesNumbersEnabled))
        }
    }

    return result
}

internal fun readExpectedOccurrencesForMultiFileTest(
    fileName: String,
    fileContent: String,
    withGeneratedFile: MutableMap<String, List<OccurrenceInfo>>,
    global: MutableList<OccurrenceInfo>
) {
    var currentOccurrenceInfos: MutableList<OccurrenceInfo> = global
    var inlineScopesNumbersEnabled = false
    for (line in fileContent.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
        when {
            line.contains(JVM_IR_TEMPLATES_WITH_INLINE_SCOPES) -> inlineScopesNumbersEnabled = true
            line.contains(JVM_IR_TEMPLATES) -> inlineScopesNumbersEnabled = false
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
            val occurrenceInfo = parseOccurrenceInfo(expectedOccurrencesMatcher, inlineScopesNumbersEnabled)
            currentOccurrenceInfos.add(occurrenceInfo)
        }
    }
}

private fun parseOccurrenceInfo(matcher: Matcher, inlineScopesNumbersEnabled: Boolean): OccurrenceInfo {
    val numberOfOccurrences = Integer.parseInt(matcher.group(1))
    val needle = matcher.group(2)
    return OccurrenceInfo(numberOfOccurrences, needle, inlineScopesNumbersEnabled)
}

internal fun checkGeneratedTextAgainstExpectedOccurrences(
    text: String,
    expectedOccurrences: List<OccurrenceInfo>,
    reportProblems: Boolean,
    assertions: Assertions,
    inlineScopesNumbersEnabled: Boolean,
) {
    val expected = StringBuilder()
    val actual = StringBuilder()
    val noScopesNumbersEntries = expectedOccurrences.none { it.inlineScopesNumbersEnabled }
    for (info in expectedOccurrences) {
        expected.append(info).append("\n")
        val hasAppropriateScopesNumbersSetting = info.inlineScopesNumbersEnabled == inlineScopesNumbersEnabled || noScopesNumbersEntries
        if (hasAppropriateScopesNumbersSetting) {
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

internal fun assertTextWasGenerated(expectedOutputFile: String, generated: Map<String, String>, assertions: Assertions) {
    if (!generated.containsKey(expectedOutputFile)) {
        val failMessage = StringBuilder()
        failMessage.append("Missing output file ").append(expectedOutputFile).append(", got ").append(generated.size).append(": ")
        for (generatedFile in generated.keys) {
            failMessage.append(generatedFile).append(" ")
        }
        assertions.fail { failMessage.toString() }
    }
}
