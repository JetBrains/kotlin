/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.utils.parsers

import org.jetbrains.kotlin.TestsExceptionType
import org.jetbrains.kotlin.spec.utils.*
import org.jetbrains.kotlin.spec.utils.models.CommonInfoElementType
import org.jetbrains.kotlin.spec.utils.models.LinkedSpecTestFileInfoElementType
import org.jetbrains.kotlin.spec.utils.models.SpecTestCaseInfoElementType
import org.jetbrains.kotlin.spec.utils.models.SpecTestInfoElements
import org.jetbrains.kotlin.spec.utils.parsers.TestCasePatterns.testCaseInfoPattern
import org.jetbrains.kotlin.spec.utils.parsers.CommonParser.splitByComma
import org.jetbrains.kotlin.spec.utils.validators.SpecTestValidationException
import org.jetbrains.kotlin.spec.utils.validators.SpecTestValidationFailedReason
import java.util.*

private operator fun SpecTestCase.plusAssign(addTestCase: SpecTestCase) {
    this.code += addTestCase.code
    this.unexpectedBehavior = this.unexpectedBehavior or addTestCase.unexpectedBehavior
    this.unspecifiedBehavior = this.unspecifiedBehavior or addTestCase.unspecifiedBehavior
    this.issues?.addAll(addTestCase.issues!!)
    this.ranges.addAll(addTestCase.ranges)
}

private fun SpecTestCase.save(
    testCasesByNumbers: TestCasesByNumbers,
    testCasesOfFile: TestCasesByNumbers,
    testCasesByRangesOfFile: NavigableMap<Int, TestCasesByNumbers>,
    caseInfoElements: SpecTestInfoElements<SpecTestInfoElementType>
) {
    val testCaseNumbers =
        caseInfoElements[SpecTestCaseInfoElementType.TESTCASE_NUMBER]!!.content.splitByComma().map {
            it.trim().toIntOrNull()
                ?: throw SpecTestValidationException(
                    SpecTestValidationFailedReason.TEST_CASE_NUMBER_FORMAT,
                    "impossible to parse number '${it.trim()}'"
                )
        }
    val startPosition = this.ranges[0].first

    testCaseNumbers.forEach { testCaseNumber ->
        if (testCasesOfFile[testCaseNumber] != null) {
            testCasesOfFile[testCaseNumber]!! += this
            testCasesByNumbers[testCaseNumber]!! += this
        } else {
            testCasesOfFile[testCaseNumber] = this
            testCasesByNumbers[testCaseNumber] = this
        }
        testCasesByRangesOfFile.putIfAbsent(startPosition, mutableMapOf())
        testCasesByRangesOfFile[startPosition]!![testCaseNumber] = this
    }
}

fun parseTestCases(testFiles: TestFiles): SpecTestCasesSet {
    val testCasesSet = SpecTestCasesSet(mutableMapOf(), mutableMapOf(), mutableMapOf())
    var rangeOffset = 0

    for ((filename, fileContent) in testFiles) {
        val matcher = testCaseInfoPattern.matcher(fileContent)
        var startFind = 0

        testCasesSet.byFiles.computeIfAbsent(filename) {
            testCasesSet.byRanges.putIfAbsent(filename, TreeMap())
            mutableMapOf()
        }

        val testCasesOfFile = testCasesSet.byFiles[filename]!!
        val testCasesByRangesOfFile = testCasesSet.byRanges[filename]!!

        while (matcher.find(startFind)) {
            val caseInfoElements = CommonParser.parseTestInfoElements(
                arrayOf(*CommonInfoElementType.values(), *SpecTestCaseInfoElementType.values()),
                matcher.group("infoElementsSL") ?: matcher.group("infoElementsML")
            )
            val nextDirective = matcher.group("nextDirectiveSL") ?: matcher.group("nextDirectiveML")
            val range = matcher.start()..matcher.end() - nextDirective.length

            SpecTestCase(
                code = matcher.group("codeSL") ?: matcher.group("codeML"),
                ranges = mutableListOf(range),
                unexpectedBehavior = caseInfoElements.contains(CommonInfoElementType.UNEXPECTED_BEHAVIOUR),
                unspecifiedBehavior = caseInfoElements.contains(LinkedSpecTestFileInfoElementType.UNSPECIFIED_BEHAVIOR),
                issues = CommonParser.parseIssues(caseInfoElements[CommonInfoElementType.ISSUES]).toMutableList(),
                exception = caseInfoElements[CommonInfoElementType.EXCEPTION]?.content?.let { TestsExceptionType.fromValue(it) }
            ).save(testCasesSet.byNumbers, testCasesOfFile, testCasesByRangesOfFile, caseInfoElements)

            startFind = matcher.end() - nextDirective.length
        }
        rangeOffset += fileContent.length
    }

    return testCasesSet
}
