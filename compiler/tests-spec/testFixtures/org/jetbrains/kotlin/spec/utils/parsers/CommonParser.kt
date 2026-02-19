/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.utils.parsers

import org.jetbrains.kotlin.spec.utils.SpecTestInfoElementContent
import org.jetbrains.kotlin.spec.utils.SpecTestInfoElementType
import org.jetbrains.kotlin.spec.utils.SpecTestLinkedType
import org.jetbrains.kotlin.spec.utils.TestFiles
import org.jetbrains.kotlin.spec.utils.models.*
import org.jetbrains.kotlin.spec.utils.parsers.CommonPatterns.testInfoElementPattern
import org.jetbrains.kotlin.spec.utils.parsers.CommonPatterns.testPathBaseRegexTemplate
import org.jetbrains.kotlin.spec.utils.parsers.LinkedSpecTestPatterns.testInfoPattern
import org.jetbrains.kotlin.spec.utils.parsers.TestCasePatterns.testCaseInfoPattern
import org.jetbrains.kotlin.spec.utils.validators.SpecTestValidationException
import org.jetbrains.kotlin.spec.utils.validators.SpecTestValidationFailedReason
import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern

object CommonParser {
    fun String.withUnderscores() = replace(" ", "_")
        .replace(File.separator, "_")
        .uppercase()

    fun String.splitByComma() = split(Regex(""",\s*"""))
    fun String.splitByPathSeparator() = split(File.separator)
    fun String.withSpaces() = replace("_", " ")

    private fun isPathMatched(pathPartRegex: String, testFilePath: String) =
        parseBasePath(pathPartRegex, testFilePath).find()

    private fun parseBasePath(pathPartRegex: String, testFilePath: String) =
        Pattern.compile(testPathBaseRegexTemplate.format(pathPartRegex)).matcher(testFilePath)

    fun parseSpecTest(testFilePath: String, files: TestFiles, isImplementationTest: Boolean = false) = when {
        isPathMatched(LinkedSpecTestPatterns.pathPartRegex, testFilePath) ->
            Pair(parseLinkedSpecTest(testFilePath, files), SpecTestLinkedType.LINKED)
        isPathMatched(NotLinkedSpecTestPatterns.pathPartRegex, testFilePath) ->
            Pair(parseNotLinkedSpecTest(testFilePath, files), SpecTestLinkedType.NOT_LINKED)
        isImplementationTest ->
            Pair(parseLinkedSpecTest(testFilePath, files, true), SpecTestLinkedType.LINKED)
        else ->
            throw SpecTestValidationException(SpecTestValidationFailedReason.FILENAME_NOT_VALID)
    }

    private fun createSpecPlace(placeMatcher: Matcher, basePlaceMatcher: Matcher = placeMatcher) =
        SpecPlace(
            placeMatcher.group("sections")?.splitByComma() ?: basePlaceMatcher.group("sections").splitByComma(),
            placeMatcher.group("paragraphNumber")?.toInt() ?: basePlaceMatcher.group("paragraphNumber").toInt(),
            placeMatcher.group("sentenceNumber").toInt()
        )

    class RelevantLinks(linksMatcher: Matcher?) {
        val linksSet: MutableSet<SpecPlace> = mutableSetOf()

        init {
            if (linksMatcher != null) {
                linksSet.add(createSpecPlace(linksMatcher))
                while (linksMatcher.find()) {
                    linksSet.add(createSpecPlace(linksMatcher))
                }
            }
        }
    }


    fun parseLinkedSpecTest(testFilePath: String, testFiles: TestFiles, isImplementationTest: Boolean = false): LinkedSpecTest {

        val parsedTestFile = tryParseTestInfo(testFilePath, testFiles, SpecTestLinkedType.LINKED, isImplementationTest)

        val testInfoElements = parsedTestFile.testInfoElements

        val primaryLinks =
            RelevantLinks(testInfoElements[LinkedSpecTestFileInfoElementType.PRIMARY_LINKS]?.additionalMatcher).linksSet
        val secondaryLinks =
            RelevantLinks(testInfoElements[LinkedSpecTestFileInfoElementType.SECONDARY_LINKS]?.additionalMatcher).linksSet

        val placeMatcher = testInfoElements[LinkedSpecTestFileInfoElementType.MAIN_LINK]?.additionalMatcher

        return LinkedSpecTest(
            testInfoElements[LinkedSpecTestFileInfoElementType.SPEC_VERSION]!!.content,
            parsedTestFile.testArea,
            parsedTestFile.testType,
            placeMatcher?.let { createSpecPlace(placeMatcher) },
            primaryLinks,
            secondaryLinks,
            parsedTestFile.testNumber,
            parsedTestFile.testDescription,
            parsedTestFile.testCasesSet,
            parsedTestFile.unexpectedBehavior,
            LinkedSpecTestFileInfoElementType.UNSPECIFIED_BEHAVIOR in testInfoElements,
            parsedTestFile.issues,
            parsedTestFile.helpers,
            parsedTestFile.exception
        )
    }

    private fun parseNotLinkedSpecTest(testFilePath: String, testFiles: TestFiles): NotLinkedSpecTest {
        val parsedTestFile = tryParseTestInfo(testFilePath, testFiles, SpecTestLinkedType.NOT_LINKED)
        val testInfoElements = parsedTestFile.testInfoElements
        val sectionsMatcher = testInfoElements[NotLinkedSpecTestFileInfoElementType.SECTIONS]!!.additionalMatcher!!

        return NotLinkedSpecTest(
            parsedTestFile.testArea,
            parsedTestFile.testType,
            sectionsMatcher.group("sections").splitByComma(),
            parsedTestFile.testNumber,
            parsedTestFile.testDescription,
            parsedTestFile.testCasesSet,
            parsedTestFile.unexpectedBehavior,
            parsedTestFile.issues,
            parsedTestFile.helpers,
            parsedTestFile.exception
        )
    }

    fun parseIssues(issues: SpecTestInfoElementContent?) = issues?.content?.splitByComma()?.toSet().orEmpty()

    fun parseTestInfoElements(rules: Array<SpecTestInfoElementType>, rawElements: String):
            SpecTestInfoElements<SpecTestInfoElementType> {
        val testInfoElementsMap = mutableMapOf<SpecTestInfoElementType, SpecTestInfoElementContent>()
        val testInfoElementMatcher = testInfoElementPattern.matcher(rawElements)

        while (testInfoElementMatcher.find()) {
            val testInfoOriginalElementName = testInfoElementMatcher.group("name")
            val testInfoElementValue = parseTestInfoElementValue(testInfoOriginalElementName, testInfoElementMatcher, rawElements)
            val testInfoElementName = parseSpecTestInfoElementType(rules, testInfoOriginalElementName)
            val testInfoElementValueMatcher = testInfoElementName.valuePattern?.matcher(testInfoElementValue)
            checkTestInfoElementIsCorrect(testInfoElementValueMatcher, testInfoElementName, testInfoElementValue)
            testInfoElementsMap[testInfoElementName] =
                SpecTestInfoElementContent(testInfoElementValue ?: "", testInfoElementValueMatcher)
        }
        checkRulesObservance(rules, testInfoElementsMap)
        return testInfoElementsMap
    }

    private fun parseTestInfoElementValue(
        testInfoOriginalElementName: String?,
        testInfoElementMatcher: Matcher,
        rawElements: String,
    ) = when (testInfoOriginalElementName) {
        LinkedSpecTestPatterns.PRIMARY_LINKS ->
            groupRelevantLinks(LinkedSpecTestPatterns.primaryLinks, rawElements, testInfoOriginalElementName)
        LinkedSpecTestPatterns.SECONDARY_LINKS ->
            groupRelevantLinks(LinkedSpecTestPatterns.secondaryLinks, rawElements, testInfoOriginalElementName)
        else ->
            testInfoElementMatcher.group("value")
    }


    private fun parseSpecTestInfoElementType(
        rules: Array<SpecTestInfoElementType>,
        testInfoOriginalElementName: String
    ) = rules.find {
        it as Enum<*>
        it.name == testInfoOriginalElementName.withUnderscores()
    } ?: throw SpecTestValidationException(
        SpecTestValidationFailedReason.TESTINFO_NOT_VALID,
        "Unknown '$testInfoOriginalElementName' test info element name."
    )

    private fun checkTestInfoElementIsCorrect(
        testInfoElementValueMatcher: Matcher?,
        testInfoElementName: SpecTestInfoElementType,
        testInfoElementValue: String?
    ) {
        if (testInfoElementValueMatcher != null && !testInfoElementValueMatcher.find())
            throw SpecTestValidationException(
                SpecTestValidationFailedReason.TESTINFO_NOT_VALID,
                "'$testInfoElementValue' in '$testInfoElementName' is not parsed."
            )
    }


    private fun checkRulesObservance(
        rules: Array<SpecTestInfoElementType>,
        testInfoElementsMap: MutableMap<SpecTestInfoElementType, SpecTestInfoElementContent>
    ) {
        rules.forEach {
            if (it.required && !testInfoElementsMap.contains(it)) {
                throw SpecTestValidationException(
                    SpecTestValidationFailedReason.TESTINFO_NOT_VALID,
                    "$it in case or test info is required."
                )
            }
        }
    }


    private fun groupRelevantLinks(linksPattern: Pattern, rawElements: String, linkType: String): String {
        val placesMatcher = linksPattern.matcher(rawElements)
        if (placesMatcher.find()) {
            return placesMatcher.group("places")
        } else throw Exception("$linkType link is incorrect")
    }

    fun testInfoFilter(fileContent: String): String =
        testInfoPattern.matcher(fileContent).replaceAll("").let {
            testCaseInfoPattern.matcher(it).replaceAll("")
        }
}
