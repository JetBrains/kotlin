/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.parsers

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.TestsExceptionType
import org.jetbrains.kotlin.spec.*
import org.jetbrains.kotlin.spec.models.CommonInfoElementType
import org.jetbrains.kotlin.spec.models.CommonSpecTestFileInfoElementType
import org.jetbrains.kotlin.spec.models.SpecTestInfoElements
import org.jetbrains.kotlin.spec.parsers.CommonParser.withUnderscores
import org.jetbrains.kotlin.spec.parsers.CommonParser.splitByComma
import org.jetbrains.kotlin.spec.validators.*
import java.io.File

data class ParsedTestFile(
    val testArea: TestArea,
    val testType: TestType,
    val testNumber: Int,
    val testDescription: String,
    val testInfoElements: SpecTestInfoElements<SpecTestInfoElementType>,
    val testCasesSet: SpecTestCasesSet,
    val unexpectedBehavior: Boolean,
    val issues: Set<String>,
    val helpers: Set<String>?,
    val exception: TestsExceptionType?
)

fun parseTestInfo(testFilePath: String, testFiles: TestFiles, linkedTestType: SpecTestLinkedType): ParsedTestFile {
    val patterns = linkedTestType.patterns.value
    val testInfoByFilenameMatcher = patterns.testPathPattern.matcher(testFilePath)

    if (!testInfoByFilenameMatcher.find())
        throw SpecTestValidationException(SpecTestValidationFailedReason.FILENAME_NOT_VALID)

    val testInfoByContentMatcher = patterns.testInfoPattern.matcher(FileUtil.loadFile(File(testFilePath), true))

    if (!testInfoByContentMatcher.find())
        throw SpecTestValidationException(SpecTestValidationFailedReason.TESTINFO_NOT_VALID)

    val testInfoElements = CommonParser.parseTestInfoElements(
        arrayOf(*CommonInfoElementType.values(), *CommonSpecTestFileInfoElementType.values(), *linkedTestType.infoElements.value),
        testInfoByContentMatcher.group("infoElements")
    )
    val helpers = testInfoElements[CommonSpecTestFileInfoElementType.HELPERS]?.content?.splitByComma()?.toSet()

    return ParsedTestFile(
        testArea = TestArea.valueOf(testInfoByContentMatcher.group("testArea").withUnderscores()),
        testType = TestType.valueOf(testInfoByContentMatcher.group("testType")),
        testNumber = testInfoElements[CommonSpecTestFileInfoElementType.NUMBER]!!.content.toInt(),
        testDescription = testInfoElements[CommonSpecTestFileInfoElementType.DESCRIPTION]!!.content,
        testInfoElements = testInfoElements,
        testCasesSet = parseTestCases(testFiles),
        unexpectedBehavior = testInfoElements.contains(CommonInfoElementType.UNEXPECTED_BEHAVIOUR),
        issues = CommonParser.parseIssues(testInfoElements[CommonInfoElementType.ISSUES]),
        helpers = helpers,
        exception = testInfoElements[CommonInfoElementType.EXCEPTION]?.content?.let { TestsExceptionType.fromValue(it) }
    )
}
