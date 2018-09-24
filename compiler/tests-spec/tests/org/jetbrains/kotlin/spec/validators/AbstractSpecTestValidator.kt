/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.validators

import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern

enum class TestType(val type: String) {
    POSITIVE("pos"),
    NEGATIVE("neg");

    companion object {
        private val map = TestType.values().associateBy(TestType::type)
        fun fromValue(type: String) = map[type]
    }
}

enum class TestArea {
    PSI,
    DIAGNOSTICS,
    CODEGEN
}

enum class SpecTestLinkedType {
    LINKED,
    NOT_LINKED
}

interface SpecTestInfoElementType {
    val valuePattern: Pattern?
    val required: Boolean
}

enum class SpecTestCaseInfoElementType(
    override val valuePattern: Pattern? = null,
    override val required: Boolean = false
) : SpecTestInfoElementType {
    CASE_DESCRIPTION(required = true),
    ISSUES(valuePattern = LinkedSpecTestFileInfoElementType.ISSUES.valuePattern),
    UNEXPECTED_BEHAVIOUR,
    DISCUSSION,
    NOTE
}

data class SpecTestInfoElementContent(
    val content: String,
    val additionalMatcher: Matcher? = null
)

data class SpecTestCase(
    val number: Int,
    val description: String,
    val unexpectedBehavior: Boolean,
    val issues: List<String>?
)

enum class SpecTestValidationFailedReason(val description: String) {
    FILENAME_NOT_VALID("Incorrect test filename or folder name."),
    TESTINFO_NOT_VALID("Test info is incorrect."),
    FILEPATH_AND_TESTINFO_IN_FILE_NOT_CONSISTENCY("Test info from filepath and file content is not consistency"),
    TEST_IS_NOT_POSITIVE("Test is not positive because it contains error elements (PsiErrorElement or diagnostic with error severity)."),
    TEST_IS_NOT_NEGATIVE("Test is not negative because it not contains error type elements (PsiErrorElement or diagnostic with error severity)."),
    UNKNOWN("Unknown validation error.")
}

class SpecTestValidationException(reason: SpecTestValidationFailedReason, details: String = "") : Exception() {
    val description = "${reason.description} $details"
}

typealias SpecTestInfoElements<T> = Map<T, SpecTestInfoElementContent>

interface SpecTestValidatorHelperObject {
    val pathPartRegex: String
    val filenameRegex: String
    fun getPathPattern(): Pattern
}

abstract class AbstractSpecTest(
    val testArea: TestArea,
    val testType: TestType,
    val section: String,
    val testNumber: Int,
    val description: String? = null,
    val cases: List<SpecTestCase>? = null,
    val unexpectedBehavior: Boolean? = null,
    val issues: Set<String>? = null
) {
    abstract fun checkConsistency(other: AbstractSpecTest): Boolean
}

abstract class AbstractSpecTestValidator<T : AbstractSpecTest>(private val testDataFile: File) {
    val testInfo by lazy { testInfoByContent }

    protected lateinit var testInfoByFilename: T
    protected lateinit var testInfoByContent: T
    abstract val testPathPattern: Pattern
    abstract val testInfoPattern: Pattern

    companion object {
        const val ISSUE_TRACKER = "https://youtrack.jetbrains.com/issue/"
        const val INTEGER_REGEX = """[1-9]\d*"""
        const val MULTILINE_COMMENT_REGEX = """\/\*\s*%s\s+\*\/\n*"""
        private const val SINGLELINE_COMMENT_REGEX = """\/\/\s*%s\n*"""

        val pathSeparator: String = Pattern.quote(File.separator)
        val lineSeparator: String = System.lineSeparator()
        val testAreaRegex = """(?<testArea>${TestArea.values().joinToString("|")})"""
        val testTypeRegex = """(?<testType>${TestType.values().joinToString("|")})"""
        val dirsByLinkedType = mapOf(
            SpecTestLinkedType.LINKED to "linked",
            SpecTestLinkedType.NOT_LINKED to "notLinked"
        )
        private val testInfoElementPattern: Pattern = Pattern.compile("""\s*(?<name>[A-Z ]+?)(?::\s*(?<value>.*?))?$lineSeparator""")
        private val testCaseInfoRegex = """(?<infoElements>CASE DESCRIPTION:[\s\S]*?$lineSeparator)$lineSeparator*"""
        private val testPathBaseRegexTemplate = """^.*?$pathSeparator(?<testArea>diagnostics|psi|codegen)$pathSeparator%s"""
        val testPathRegexTemplate = """$testPathBaseRegexTemplate$pathSeparator(?<testType>pos|neg)/%s$"""
        val testCaseInfoSingleLinePattern: Pattern = Pattern.compile(SINGLELINE_COMMENT_REGEX.format(testCaseInfoRegex))
        val testCaseInfoMultilinePattern: Pattern = Pattern.compile(MULTILINE_COMMENT_REGEX.format(testCaseInfoRegex))

        fun getInstanceByType(testFile: File) = when {
            Pattern.compile(testPathBaseRegexTemplate.format(LinkedSpecTestValidator.pathPartRegex)).matcher(testFile.absolutePath).find() ->
                LinkedSpecTestValidator(testFile)
            Pattern.compile(testPathBaseRegexTemplate.format(NotLinkedSpecTestValidator.pathPartRegex)).matcher(testFile.absolutePath).find() ->
                NotLinkedSpecTestValidator(testFile)
            else -> throw SpecTestValidationException(SpecTestValidationFailedReason.FILENAME_NOT_VALID)
        }

        fun getInstanceByType(testPath: String) = getInstanceByType(File(testPath))

        private fun getTestInfoElements(
            testInfoElementRules: Array<out SpecTestInfoElementType>,
            testInfoElements: String
        ): SpecTestInfoElements<SpecTestInfoElementType> {
            val testInfoElementsMap = mutableMapOf<SpecTestInfoElementType, SpecTestInfoElementContent>()
            val testInfoElementMatcher = testInfoElementPattern.matcher(testInfoElements)

            while (testInfoElementMatcher.find()) {
                val testInfoOriginalElementName = testInfoElementMatcher.group("name")
                val testInfoElementName = testInfoElementRules.find {
                    it as Enum<*>
                    it.name == testInfoOriginalElementName.replace(" ", "_")
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

            testInfoElementRules.forEach {
                if (it.required && !testInfoElementsMap.contains(it)) {
                    throw SpecTestValidationException(
                        SpecTestValidationFailedReason.TESTINFO_NOT_VALID,
                        "$it in case or test info is required."
                    )
                }
            }

            return testInfoElementsMap
        }

        private fun getIssues(testCases: List<SpecTestCase>, testIssues: List<String>?): Set<String> {
            val issues = mutableSetOf<String>()

            testCases.forEach {
                if (it.issues != null) issues.addAll(it.issues)
            }

            if (testIssues != null) issues.addAll(testIssues)

            return issues
        }

        fun parseIssues(issues: SpecTestInfoElementContent?) = issues?.content?.split(",")
    }

    private fun getTestCasesInfo(
        testCaseInfoMatcher: Matcher,
        infoElements: SpecTestInfoElements<SpecTestInfoElementType>
    ): List<SpecTestCase> {
        val testCases = mutableListOf<SpecTestCase>()
        var testCasesCounter = 1

        while (testCaseInfoMatcher.find()) {
            val caseInfoElements = getTestInfoElements(
                SpecTestCaseInfoElementType.values(),
                testCaseInfoMatcher.group("infoElements")
            )

            testCases.add(
                SpecTestCase(
                    testCasesCounter++,
                    caseInfoElements[SpecTestCaseInfoElementType.CASE_DESCRIPTION]!!.content,
                    caseInfoElements.contains(SpecTestCaseInfoElementType.UNEXPECTED_BEHAVIOUR),
                    parseIssues(caseInfoElements[SpecTestCaseInfoElementType.ISSUES])
                )
            )
        }

        if (testCases.isEmpty())
            testCases.add(getSingleTestCase(infoElements))

        return testCases
    }

    abstract fun getSingleTestCase(testInfoElements: SpecTestInfoElements<SpecTestInfoElementType>) : SpecTestCase

    fun testInfoFilter(fileContent: String): String =
        testInfoPattern.matcher(fileContent).replaceAll("").let {
            testCaseInfoSingleLinePattern.matcher(it).replaceAll("").let {
                testCaseInfoMultilinePattern.matcher(it).replaceAll("")
            }
        }

    abstract fun getTestInfo(
        testInfoMatcher: Matcher,
        testInfoElements: SpecTestInfoElements<SpecTestInfoElementType>,
        testCases: List<SpecTestCase>,
        unexpectedBehavior: Boolean = false,
        issues: Set<String>? = null
    ): T

    abstract fun getTestInfo(testInfoMatcher: Matcher): T

    abstract fun parseTestInfo()

    abstract fun printTestInfo()

    fun parseTestInfo(testInfoElementsRules: Array<out SpecTestInfoElementType>) {
        val testInfoByFilenameMatcher = testPathPattern.matcher(testDataFile.canonicalPath)

        if (!testInfoByFilenameMatcher.find())
            throw SpecTestValidationException(SpecTestValidationFailedReason.FILENAME_NOT_VALID)

        val fileContent = testDataFile.readText()
        val testInfoByContentMatcher = testInfoPattern.matcher(fileContent)

        if (!testInfoByContentMatcher.find())
            throw SpecTestValidationException(SpecTestValidationFailedReason.TESTINFO_NOT_VALID)

        val testInfoElements = getTestInfoElements(
            testInfoElementsRules,
            testInfoByContentMatcher.group("infoElements")
        )
        val testCases = getTestCasesInfo(testCaseInfoSingleLinePattern.matcher(fileContent), testInfoElements) +
                getTestCasesInfo(testCaseInfoMultilinePattern.matcher(fileContent), testInfoElements)

        testInfoByFilename = getTestInfo(testInfoByFilenameMatcher)
        testInfoByContent = getTestInfo(
            testInfoByContentMatcher,
            testInfoElements,
            testCases = testCases,
            unexpectedBehavior = testInfoElements.contains(LinkedSpecTestFileInfoElementType.UNEXPECTED_BEHAVIOUR) || testCases.any { it.unexpectedBehavior },
            issues = getIssues(
                testCases,
                parseIssues(
                    testInfoElements[LinkedSpecTestFileInfoElementType.ISSUES]
                )
            )
        )

        if (!testInfoByFilename.checkConsistency(testInfoByContent))
            throw SpecTestValidationException(SpecTestValidationFailedReason.FILEPATH_AND_TESTINFO_IN_FILE_NOT_CONSISTENCY)
    }

    fun validateTestType(computedTestType: TestType) {
        if (computedTestType != testInfo.testType) {
            val isNotNegative = computedTestType == TestType.POSITIVE && testInfo.testType == TestType.NEGATIVE
            val isNotPositive = computedTestType == TestType.NEGATIVE && testInfo.testType == TestType.POSITIVE
            val reason = when {
                isNotNegative -> SpecTestValidationFailedReason.TEST_IS_NOT_NEGATIVE
                isNotPositive -> SpecTestValidationFailedReason.TEST_IS_NOT_POSITIVE
                else -> SpecTestValidationFailedReason.UNKNOWN
            }
            throw SpecTestValidationException(reason)
        }
    }
}