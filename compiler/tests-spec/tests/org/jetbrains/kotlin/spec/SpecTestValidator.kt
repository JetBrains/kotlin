/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec

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

interface SpecTestInfoElementType {
    val valuePattern: Pattern?
    val required: Boolean
}

enum class SpecTestFileInfoElementType(
    override val valuePattern: Pattern? = null,
    override val required: Boolean = false
) : SpecTestInfoElementType {
    SECTION(
        valuePattern = Pattern.compile("""(?<number>(?:${SpecTestValidator.INTEGER_REGEX})(?:\.${SpecTestValidator.INTEGER_REGEX})*)(?<name>.*?)"""),
        required = true
    ),
    PARAGRAPH(required = true),
    SENTENCE(
        valuePattern = Pattern.compile("""\[(?<number>${SpecTestValidator.INTEGER_REGEX})\](?<text>.*?)"""),
        required = true
    ),
    NUMBER(required = true),
    DESCRIPTION(required = true),
    ISSUES(valuePattern = Pattern.compile("""(KT-[1-9]\d*)(,\s*KT-[1-9]\d*)*""")),
    UNEXPECTED_BEHAVIOUR,
    DISCUSSION,
    NOTE
}

enum class SpecTestCaseInfoElementType(
    override val valuePattern: Pattern? = null,
    override val required: Boolean = false
) : SpecTestInfoElementType {
    CASE_DESCRIPTION(required = true),
    ISSUES(valuePattern = SpecTestFileInfoElementType.ISSUES.valuePattern),
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

class SpecTest(
    val testArea: TestArea,
    val testType: TestType,
    val sectionNumber: String,
    val sectionName: String,
    val paragraphNumber: Int,
    val sentenceNumber: Int,
    val sentence: String? = null,
    val testNumber: Int,
    val description: String? = null,
    val cases: List<SpecTestCase>? = null,
    val unexpectedBehavior: Boolean? = null,
    val issues: Set<String>? = null
) {
    fun checkConsistency(other: SpecTest): Boolean {
        return this.testArea == other.testArea
                && this.testType == other.testType
                && this.sectionNumber == other.sectionNumber
                && this.testNumber == other.testNumber
                && this.paragraphNumber == other.paragraphNumber
                && this.sentenceNumber == other.sentenceNumber
    }
}

enum class SpecTestValidationFailedReason(val description: String) {
    FILENAME_NOT_VALID(
        "Incorrect test filename or folder name.${SpecTestValidator.lineSeparator}" +
                "It must match the following path pattern: " +
                "testsData/<diagnostic|psi|codegen>/s<sectionNumber>_<sectionName>/p-<paragraph>/<pos|neg>/<sentence>_<testNumber>.kt " +
                "(example: testsData/diagnostic/s-16.30_when-expression/p-3/pos/1.3.kt)"
    ),
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

abstract class SpecTestValidator(private val testDataFile: File, private val testArea: TestArea) {
    private lateinit var testInfoByFilename: SpecTest
    private lateinit var testInfoByContent: SpecTest
    private val testInfo by lazy { testInfoByContent }

    companion object {
        const val INTEGER_REGEX = """[1-9]\d*"""
        private const val SINGLELINE_COMMENT_REGEX = """\/\/\s*%s"""
        private const val MULTILINE_COMMENT_REGEX = """\/\*\s*%s\s+\*\/"""

        val lineSeparator: String = System.lineSeparator()
        private val testCaseInfoRegex = """(?<infoElements>CASE DESCRIPTION:[\s\S]*?$lineSeparator)"""
        private val testAreaRegex = """(?<testArea>${TestArea.values().joinToString("|")})"""
        private val testTypeRegex = """(?<testType>${TestType.values().joinToString("|")})"""
        private val testInfoElementPattern: Pattern = Pattern.compile("""\s*(?<name>[A-Z ]*?):\s*(?<value>.*?)$lineSeparator""")
        private val pathSeparator = Pattern.quote(File.separator)

        val testPathPattern: Pattern =
            Pattern.compile("""^.*?$pathSeparator(?<testArea>diagnostics|psi|codegen)${pathSeparator}s-(?<sectionNumber>(?:$INTEGER_REGEX)(?:\.$INTEGER_REGEX)*)_(?<sectionName>[\w-]+)${pathSeparator}p-(?<paragraphNumber>$INTEGER_REGEX)$pathSeparator(?<testType>pos|neg)$pathSeparator(?<sentenceNumber>$INTEGER_REGEX)\.(?<testNumber>$INTEGER_REGEX)\.kt$""")
        val testInfoPattern: Pattern =
            Pattern.compile(MULTILINE_COMMENT_REGEX.format("""KOTLIN $testAreaRegex SPEC TEST \($testTypeRegex\)$lineSeparator(?<infoElements>[\s\S]*?$lineSeparator)"""))
        val testCaseInfoSinglelinePattern: Pattern = Pattern.compile(SINGLELINE_COMMENT_REGEX.format(testCaseInfoRegex))
        val testCaseInfoMultilinePattern: Pattern = Pattern.compile(MULTILINE_COMMENT_REGEX.format(testCaseInfoRegex))

        private fun getTestInfo(
            testInfoMatcher: Matcher,
            testInfoElements: SpecTestInfoElements<SpecTestInfoElementType>,
            testCases: List<SpecTestCase>,
            unexpectedBehavior: Boolean = false,
            issues: Set<String>? = null
        ): SpecTest {
            val sectionMatcher = testInfoElements[SpecTestFileInfoElementType.SECTION]!!.additionalMatcher!!
            val sentenceMatcher = testInfoElements[SpecTestFileInfoElementType.SENTENCE]!!.additionalMatcher!!

            return SpecTest(
                TestArea.valueOf(testInfoMatcher.group("testArea").toUpperCase()),
                TestType.valueOf(testInfoMatcher.group("testType")),
                sectionMatcher.group("number"),
                sectionMatcher.group("name"),
                testInfoElements[SpecTestFileInfoElementType.PARAGRAPH]!!.content.toInt(),
                sentenceMatcher.group("number").toInt(),
                sentenceMatcher.group("text"),
                testInfoElements[SpecTestFileInfoElementType.NUMBER]!!.content.toInt(),
                testInfoElements[SpecTestFileInfoElementType.DESCRIPTION]!!.content,
                testCases,
                unexpectedBehavior,
                issues
            )
        }

        private fun getTestInfo(testInfoMatcher: Matcher): SpecTest {
            return SpecTest(
                TestArea.valueOf(testInfoMatcher.group("testArea").toUpperCase()),
                TestType.fromValue(testInfoMatcher.group("testType"))!!,
                testInfoMatcher.group("sectionNumber"),
                testInfoMatcher.group("sectionName"),
                testInfoMatcher.group("paragraphNumber").toInt(),
                testInfoMatcher.group("sentenceNumber").toInt(),
                testNumber = testInfoMatcher.group("testNumber").toInt()
            )
        }

        private fun parseIssues(issues: SpecTestInfoElementContent?) = issues?.content?.split(",")

        private fun getSingleTestCase(testInfoElements: SpecTestInfoElements<SpecTestInfoElementType>) = SpecTestCase(
            1,
            description = testInfoElements[SpecTestFileInfoElementType.DESCRIPTION]!!.content,
            unexpectedBehavior = testInfoElements.contains(SpecTestFileInfoElementType.UNEXPECTED_BEHAVIOUR),
            issues = parseIssues(testInfoElements[SpecTestFileInfoElementType.ISSUES])
        )

        private fun getTestCasesInfo(testCaseInfoMatcher: Matcher, infoElements: SpecTestInfoElements<SpecTestInfoElementType>): List<SpecTestCase> {
            val testCases = mutableListOf<SpecTestCase>()
            var testCasesCounter = 1

            while (testCaseInfoMatcher.find()) {
                val caseInfoElements = getTestInfoElements<SpecTestCaseInfoElementType>(testCaseInfoMatcher.group("infoElements"))

                testCases.add(
                    SpecTestCase(
                        testCasesCounter++,
                        caseInfoElements[SpecTestCaseInfoElementType.CASE_DESCRIPTION]!!.content,
                        caseInfoElements.contains(SpecTestCaseInfoElementType.UNEXPECTED_BEHAVIOUR),
                        parseIssues(caseInfoElements[SpecTestCaseInfoElementType.ISSUES])
                    )
                )
            }

            if (testCases.isEmpty()) testCases.add(getSingleTestCase(infoElements))

            return testCases
        }

        private inline fun <reified T : Enum<T>> getTestInfoElements(testInfoElements: String): SpecTestInfoElements<SpecTestInfoElementType> {
            val testInfoElementsMap = mutableMapOf<SpecTestInfoElementType, SpecTestInfoElementContent>()
            val testInfoElementMatcher = testInfoElementPattern.matcher(testInfoElements)

            while (testInfoElementMatcher.find()) {
                val testInfoOriginalElementName = testInfoElementMatcher.group("name")
                val testInfoElementName: SpecTestInfoElementType
                try {
                    testInfoElementName = enumValueOf<T>(testInfoOriginalElementName.replace(" ", "_")) as SpecTestInfoElementType
                } catch (e: IllegalArgumentException) {
                    throw SpecTestValidationException(
                        SpecTestValidationFailedReason.TESTINFO_NOT_VALID,
                        "Unknown '$testInfoOriginalElementName' test info element name."
                    )
                }
                val testInfoElementValue = testInfoElementMatcher.group("value")
                val testInfoElementValueMatcher = testInfoElementName.valuePattern?.matcher(testInfoElementValue)

                if (testInfoElementValueMatcher != null && !testInfoElementValueMatcher.find())
                    throw SpecTestValidationException(
                        SpecTestValidationFailedReason.TESTINFO_NOT_VALID,
                        "'$testInfoElementValue' in '$testInfoElementName' is not parsed."
                    )

                testInfoElementsMap[testInfoElementName] = SpecTestInfoElementContent(testInfoElementValue, testInfoElementValueMatcher)
            }

            enumValues<T>().forEach {
                it as SpecTestInfoElementType
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
    }

    fun parseTestInfo() {
        val testInfoByFilenameMatcher = testPathPattern.matcher(testDataFile.path)

        if (!testInfoByFilenameMatcher.find())
            throw SpecTestValidationException(SpecTestValidationFailedReason.FILENAME_NOT_VALID)

        val fileContent = testDataFile.readText()
        val testInfoByContentMatcher = testInfoPattern.matcher(fileContent)

        if (!testInfoByContentMatcher.find())
            throw SpecTestValidationException(SpecTestValidationFailedReason.TESTINFO_NOT_VALID)

        val testInfoElements = getTestInfoElements<SpecTestFileInfoElementType>(testInfoByContentMatcher.group("infoElements"))
        val testCases = getTestCasesInfo(testCaseInfoSinglelinePattern.matcher(fileContent), testInfoElements) +
                getTestCasesInfo(testCaseInfoMultilinePattern.matcher(fileContent), testInfoElements)

        testInfoByFilename = getTestInfo(testInfoByFilenameMatcher)
        testInfoByContent = getTestInfo(
            testInfoByContentMatcher,
            testInfoElements,
            testCases = testCases,
            unexpectedBehavior = testInfoElements.contains(SpecTestFileInfoElementType.UNEXPECTED_BEHAVIOUR) || testCases.any { it.unexpectedBehavior },
            issues = getIssues(testCases, parseIssues(testInfoElements[SpecTestFileInfoElementType.ISSUES]))
        )

        if (!testInfoByFilename.checkConsistency(testInfoByContent))
            throw SpecTestValidationException(SpecTestValidationFailedReason.FILEPATH_AND_TESTINFO_IN_FILE_NOT_CONSISTENCY)
    }

    protected fun validateTestType(computedTestType: TestType) {
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

    fun printTestInfo() {
        println("--------------------------------------------------")
        if (testInfoByContent.unexpectedBehavior!!)
            println("(!!!) HAS UNEXPECTED BEHAVIOUR (!!!)")
        println("$testArea ${testInfoByFilename.testType} SPEC TEST")
        println("SECTION: ${testInfoByFilename.sectionNumber} ${testInfoByContent.sectionName} (paragraph: ${testInfoByFilename.paragraphNumber})")
        println("SENTENCE ${testInfoByContent.sentenceNumber}: ${testInfoByContent.sentence}")
        println("TEST NUMBER: ${testInfoByContent.testNumber}")
        println("NUMBER OF TEST CASES: ${testInfoByContent.cases!!.size}")
        println("DESCRIPTION: ${testInfoByContent.description}")
        if (testInfoByContent.issues!!.isNotEmpty()) {
            println("LINKED ISSUES: ${testInfoByContent.issues!!.joinToString(", ")}")
        }
    }
}