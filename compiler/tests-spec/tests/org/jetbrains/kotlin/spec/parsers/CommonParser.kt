/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.parsers

import org.jetbrains.kotlin.spec.*
import org.jetbrains.kotlin.spec.SpecTestInfoElementContent
import org.jetbrains.kotlin.spec.SpecTestLinkedType
import org.jetbrains.kotlin.spec.models.LinkedSpecTest
import org.jetbrains.kotlin.spec.models.LinkedSpecTestFileInfoElementType
import org.jetbrains.kotlin.spec.models.NotLinkedSpecTest
import org.jetbrains.kotlin.spec.models.SpecTestInfoElements
import org.jetbrains.kotlin.spec.parsers.CommonPatterns.testInfoElementPattern
import org.jetbrains.kotlin.spec.parsers.CommonPatterns.testPathBaseRegexTemplate
import org.jetbrains.kotlin.spec.parsers.LinkedSpecTestPatterns.testInfoPattern
import org.jetbrains.kotlin.spec.parsers.TestCasePatterns.testCaseInfoPattern
import org.jetbrains.kotlin.spec.validators.*
import java.io.File
import java.util.regex.Pattern

object CommonParser {
    fun String.withUnderscores() = replace(" ", "_").toUpperCase()
    fun String.splitByComma() = split(Regex(""",\s*"""))
    fun String.splitByPathSeparator() = split(File.separator)
    fun String.withSpaces() = replace("_", " ")

    private fun isPathMatched(pathPartRegex: String, testFilePath: String) =
        Pattern.compile(testPathBaseRegexTemplate.format(pathPartRegex)).matcher(testFilePath).find()

    fun parseSpecTest(testFilePath: String, files: TestFiles) = when {
        isPathMatched(LinkedSpecTestPatterns.pathPartRegex, testFilePath) ->
            Pair(parseLinkedSpecTest(testFilePath, files), SpecTestLinkedType.LINKED)
        isPathMatched(NotLinkedSpecTestPatterns.pathPartRegex, testFilePath) ->
            Pair(parseNotLinkedSpecTest(testFilePath, files), SpecTestLinkedType.NOT_LINKED)
        else ->
            throw SpecTestValidationException(SpecTestValidationFailedReason.FILENAME_NOT_VALID)
    }

    private fun parseLinkedSpecTest(testFilePath: String, testFiles: TestFiles): LinkedSpecTest {
        val parsedTestFile = parseTestInfo(testFilePath, testFiles, SpecTestLinkedType.LINKED)
        val testInfoElements = parsedTestFile.testInfoElements
        val sentenceMatcher = testInfoElements[LinkedSpecTestFileInfoElementType.SENTENCE]!!.additionalMatcher!!

        return LinkedSpecTest(
            parsedTestFile.testArea,
            parsedTestFile.testType,
            parsedTestFile.sections,
            testInfoElements[LinkedSpecTestFileInfoElementType.PARAGRAPH]!!.content.toInt(),
            sentenceMatcher.group("number").toInt(),
            sentenceMatcher.group("text"),
            parsedTestFile.testNumber,
            parsedTestFile.testDescription,
            parsedTestFile.testCasesSet,
            parsedTestFile.unexpectedBehavior,
            parsedTestFile.issues
        )
    }

    private fun parseNotLinkedSpecTest(testFilePath: String, testFiles: TestFiles): NotLinkedSpecTest {
        val parsedTestFile = parseTestInfo(testFilePath, testFiles, SpecTestLinkedType.NOT_LINKED)

        return NotLinkedSpecTest(
            parsedTestFile.testArea,
            parsedTestFile.testType,
            parsedTestFile.sections,
            parsedTestFile.testNumber,
            parsedTestFile.testDescription,
            parsedTestFile.testCasesSet,
            parsedTestFile.unexpectedBehavior,
            parsedTestFile.issues
        )
    }

    fun parseIssues(issues: SpecTestInfoElementContent?) = issues?.content?.splitByComma()?.toSet().orEmpty()

    fun parseTestInfoElements(rules: Array<SpecTestInfoElementType>, rawElements: String):
            SpecTestInfoElements<SpecTestInfoElementType> {
        val testInfoElementsMap = mutableMapOf<SpecTestInfoElementType, SpecTestInfoElementContent>()
        val testInfoElementMatcher = testInfoElementPattern.matcher(rawElements)

        while (testInfoElementMatcher.find()) {
            val testInfoOriginalElementName = testInfoElementMatcher.group("name")
            val testInfoElementName = rules.find {
                it as Enum<*>
                it.name == testInfoOriginalElementName.withUnderscores()
            } ?: throw SpecTestValidationException(
                SpecTestValidationFailedReason.TESTINFO_NOT_VALID,
                "Unknown '$testInfoOriginalElementName' test info element name."
            )
            val testInfoElementValue = testInfoElementMatcher.group("value")
            val testInfoElementValueMatcher = testInfoElementName.valuePattern?.matcher(testInfoElementValue)

            if (testInfoElementValueMatcher != null && !testInfoElementValueMatcher.find())
                throw SpecTestValidationException(
                    SpecTestValidationFailedReason.TESTINFO_NOT_VALID,
                    "'$testInfoElementValue' in '$testInfoElementName' is not parsed."
                )

            testInfoElementsMap[testInfoElementName] =
                    SpecTestInfoElementContent(testInfoElementValue ?: "", testInfoElementValueMatcher)
        }

        rules.forEach {
            if (it.required && !testInfoElementsMap.contains(it)) {
                throw SpecTestValidationException(
                    SpecTestValidationFailedReason.TESTINFO_NOT_VALID,
                    "$it in case or test info is required."
                )
            }
        }

        return testInfoElementsMap
    }

    fun testInfoFilter(fileContent: String) =
        testInfoPattern.matcher(fileContent).replaceAll("").let {
            testCaseInfoPattern.matcher(it).replaceAll("")
        }
}
