/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.utils.parsers

import org.jetbrains.kotlin.spec.utils.*
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
        .toUpperCase()

    fun String.splitByComma() = split(Regex(""",\s*"""))
    fun String.splitByPathSeparator() = split(File.separator)
    fun String.withSpaces() = replace("_", " ")

    private fun isPathMatched(pathPartRegex: String, testFilePath: String) =
        parseBasePath(pathPartRegex, testFilePath).find()

    private fun parseBasePath(pathPartRegex: String, testFilePath: String) =
        Pattern.compile(testPathBaseRegexTemplate.format(pathPartRegex)).matcher(testFilePath)

    fun parsePath(pathPartRegex: String, testFilePath: String) =
        Pattern.compile(testPathBaseRegexTemplate.format(pathPartRegex)).matcher(testFilePath)

    fun parseSpecTest(testFilePath: String, files: TestFiles) = when {
        isPathMatched(LinkedSpecTestPatterns.pathPartRegex, testFilePath) ->
            Pair(parseLinkedSpecTest(testFilePath, files), SpecTestLinkedType.LINKED)
        isPathMatched(NotLinkedSpecTestPatterns.pathPartRegex, testFilePath) ->
            Pair(parseNotLinkedSpecTest(testFilePath, files), SpecTestLinkedType.NOT_LINKED)
        else ->
            throw SpecTestValidationException(SpecTestValidationFailedReason.FILENAME_NOT_VALID)
    }

    fun parseImplementationTest(file: File, testArea: TestArea): LinkedSpecTest? {
        val matcher = ImplementationTestPatterns.testInfoPattern.matcher(file.readText())

        if (!matcher.find())
            return null

        val testType = TestType.fromValue(matcher.group("testType"))
            ?: throw SpecTestValidationException(SpecTestValidationFailedReason.TESTINFO_NOT_VALID)
        val specVersion = matcher.group("specVersion")
        val testSpecSentenceList = matcher.group("testSpecSentenceList")
        val specSentenceListMatcher = ImplementationTestPatterns.relevantSpecSentencesPattern.matcher(testSpecSentenceList)
        val specPlaces = mutableListOf<SpecPlace>()

        while (specSentenceListMatcher.find()) {
            specPlaces.add(
                SpecPlace(
                    sections = specSentenceListMatcher.group("specSections").split(Regex(""",\s*""")),
                    paragraphNumber = specSentenceListMatcher.group("specParagraph").toInt(),
                    sentenceNumber = specSentenceListMatcher.group("specSentence").toInt()
                )
            )
        }

        return LinkedSpecTest.getInstanceForImplementationTest(specVersion, testArea, testType, specPlaces, file.nameWithoutExtension)
    }

    private fun createSpecPlace(placeMatcher: Matcher, basePlaceMatcher: Matcher = placeMatcher) =
        SpecPlace(
            placeMatcher.group("sections")?.splitByComma() ?: basePlaceMatcher.group("sections").splitByComma(),
            placeMatcher.group("paragraphNumber")?.toInt() ?: basePlaceMatcher.group("paragraphNumber").toInt(),
            placeMatcher.group("sentenceNumber").toInt()
        )

    fun parseLinkedSpecTest(testFilePath: String, testFiles: TestFiles): LinkedSpecTest {
        val parsedTestFile = tryParseTestInfo(testFilePath, testFiles, SpecTestLinkedType.LINKED)
        val testInfoElements = parsedTestFile.testInfoElements
        val placeMatcher = testInfoElements[LinkedSpecTestFileInfoElementType.PLACE]!!.additionalMatcher!!
        val relevantPlacesMatcher = testInfoElements[LinkedSpecTestFileInfoElementType.RELEVANT_PLACES]?.additionalMatcher
        val relevantPlaces = relevantPlacesMatcher?.let {
            mutableListOf<SpecPlace>().apply {
                add(createSpecPlace(it, placeMatcher))
                while (it.find()) {
                    add(createSpecPlace(it, placeMatcher))
                }
            }
        }

        return LinkedSpecTest(
            testInfoElements[LinkedSpecTestFileInfoElementType.SPEC_VERSION]!!.content,
            parsedTestFile.testArea,
            parsedTestFile.testType,
            createSpecPlace(placeMatcher),
            relevantPlaces,
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
            val testInfoElementName = rules.find {
                it as Enum<*>
                it.name == testInfoOriginalElementName.withUnderscores()
            } ?: throw SpecTestValidationException(
                SpecTestValidationFailedReason.TESTINFO_NOT_VALID,
                "Unknown '$testInfoOriginalElementName' test info element name."
            )
            val testInfoElementValue: String?
            testInfoElementValue = if (testInfoOriginalElementName == "RELEVANT PLACES") {
                val relevantPlacesMatcher = LinkedSpecTestPatterns.relevantPlaces.matcher(rawElements)
                if (relevantPlacesMatcher.find()) {
                    relevantPlacesMatcher.group("places")
                } else throw Exception("Relevant link is incorrect")
            } else {
                testInfoElementMatcher.group("value")
            }
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
