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

enum class TestArea(val title: String) {
    PARSING("PARSING"),
    DIAGNOSTIC("DIAGNOSTIC"),
    BLACKBOX("BLACK BOX")
}

data class TestCase(
    val number: Int,
    val description: String,
    val unexpectedBehavior: Boolean,
    val issues: List<String>?
)

data class TestInfo(
    val testType: TestType,
    val sectionNumber: String,
    val sectionName: String,
    val paragraphNumber: Int,
    val sentenceNumber: Int,
    val sentence: String?,
    val testNumber: Int,
    val description: String?,
    val cases: List<TestCase>?,
    val unexpectedBehavior: Boolean,
    val issues: List<String>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TestInfo) return false

        return this.testType == other.testType
                && this.sectionNumber == other.sectionNumber
                && this.testNumber == other.testNumber
                && this.paragraphNumber == other.paragraphNumber
                && this.sentenceNumber == other.sentenceNumber
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

enum class SpecTestValidationFailedReason(val description: String) {
    FILENAME_NOT_VALID(
        "Incorrect test filename or folder name.\n" +
                "It must match the following path pattern: " +
                "testsSpec/s<sectionNumber>_<sectionName>/p<paragraph>s<sentence>_<pos|neg>.kt " +
                "(example: testsSpec/s16.30:when-expression/1:1-pos.kt)"
    ),
    METAINFO_NOT_VALID("Incorrect meta info in test file."),
    FILENAME_AND_METAINFO_NOT_CONSISTENCY("Test info from filename and file content is not consistency"),
    NOT_PARSED("Test info not parsed. You must call parseTestInfo before test info printing."),
    TEST_IS_NOT_POSITIVE("Test is not positive because it contains diagnostics with ERROR severity."),
    TEST_IS_NOT_NEGATIVE("Test is not negative because it not contains diagnostics with ERROR severity."),
    UNKNOWN("Unknown validation error.")
}

class SpecTestValidationException(val reason: SpecTestValidationFailedReason) : Exception()

abstract class SpecTestValidator(private val testDataFile: File, private val testArea: TestArea) {
    private lateinit var testInfoByFilename: TestInfo
    private lateinit var testInfoByContent: TestInfo
    protected val testInfo by lazy { testInfoByContent }

    companion object {
        const val specUrl = "https://jetbrains.github.io/kotlin-spec/"

        private const val integerRegex = "[1-9]\\d*"
        private const val testPathRegex =
            "^.*?/s-(?<sectionNumber>(?:$integerRegex)(?:\\.$integerRegex)*)_(?<sectionName>[\\w-]+)/p-(?<paragraphNumber>$integerRegex)/(?<testType>pos|neg)/(?<sentenceNumber>$integerRegex)\\.(?<testNumber>$integerRegex)\\.kt$"
        private const val testUnexpectedBehaviour = "(?:\n\\s*(?<unexpectedBehaviour>UNEXPECTED BEHAVIOUR))"
        private const val testIssues = "(?:\n\\s*ISSUES:\\s*(?<issues>(KT-[1-9]\\d*)(,\\s*KT-[1-9]\\d*)*))"
        private const val testContentMetaInfoRegex =
            "\\/\\*\\s+KOTLIN SPEC TEST \\((?<testType>POSITIVE|NEGATIVE)\\)\\s+SECTION (?<sectionNumber>(?:$integerRegex)(?:\\.$integerRegex)*):\\s*(?<sectionName>.*?)\\s+PARAGRAPH:\\s*(?<paragraphNumber>$integerRegex)\\s+SENTENCE\\s*(?<sentenceNumber>$integerRegex):\\s*(?<sentence>.*?)\\s+NUMBER:\\s*(?<testNumber>$integerRegex)\\s+DESCRIPTION:\\s*(?<testDescription>.*?)$testUnexpectedBehaviour?$testIssues?\\s+\\*\\/\\s+"
        private const val testCaseInfo =
            "(?:(?:\\/\\*\n\\s*)|(?:\\/\\/\\s*))CASE DESCRIPTION:\\s*(?<testCaseDescription>.*?)$testUnexpectedBehaviour?$testIssues?\n(\\s\\*\\/)?"

        private fun getTestInfo(
            testInfoMatcher: Matcher,
            directMappedTestTypeEnum: Boolean = false,
            withDetails: Boolean = false,
            testCases: List<TestCase>? = null,
            unexpectedBehavior: Boolean = false,
            issues: List<String>? = null
        ): TestInfo {
            val testDescription = if (withDetails) testInfoMatcher.group("testDescription") else null

            return TestInfo(
                if (directMappedTestTypeEnum)
                    TestType.valueOf(testInfoMatcher.group("testType")) else
                    TestType.fromValue(testInfoMatcher.group("testType"))!!,
                testInfoMatcher.group("sectionNumber"),
                testInfoMatcher.group("sectionName"),
                testInfoMatcher.group("paragraphNumber").toInt(),
                testInfoMatcher.group("sentenceNumber").toInt(),
                if (withDetails) testInfoMatcher.group("sentence") else null,
                testInfoMatcher.group("testNumber").toInt(),
                testDescription,
                testCases,
                unexpectedBehavior,
                issues
            )
        }

        private fun getSingleTestCase(testInfoMatcher: Matcher): TestCase {
            val testDescription = testInfoMatcher.group("testDescription")
            val unexpectedBehaviour = testInfoMatcher.group("unexpectedBehaviour") != null
            val issues = testInfoMatcher.group("issues")?.split(",")

            return TestCase(
                1,
                testDescription,
                unexpectedBehaviour,
                issues
            )
        }

        private fun getTestCasesInfo(testCaseInfoMatcher: Matcher, testInfoMatcher: Matcher): List<TestCase> {
            val testCases = mutableListOf<TestCase>()
            var testCasesCounter = 1

            while (testCaseInfoMatcher.find()) {
                val unexpectedBehaviour = testCaseInfoMatcher.group("unexpectedBehaviour") != null
                val issues = testCaseInfoMatcher.group("issues")?.split(Regex(",\\s*"))

                testCases.add(
                    TestCase(
                        testCasesCounter++,
                        testCaseInfoMatcher.group("testCaseDescription"),
                        unexpectedBehaviour,
                        issues
                    )
                )
            }

            if (testCases.isEmpty()) {
                testCases.add(getSingleTestCase(testInfoMatcher))
            }

            return testCases
        }
    }

    private fun hasUnexpectedBehavior(testCases: List<TestCase>, testInfoMatcher: Matcher): Boolean {
        testCases.forEach {
            if (it.unexpectedBehavior) return true
        }

        return testInfoMatcher.group("unexpectedBehaviour") != null
    }

    private fun getIssues(testCases: List<TestCase>, testInfoMatcher: Matcher): List<String> {
        val issues = mutableListOf<String>()

        testCases.forEach {
            if (it.issues != null) issues.addAll(it.issues)
        }

        val testIssues = testInfoMatcher.group("issues")?.split(Regex(",\\s*"))

        if (testIssues != null) issues.addAll(testIssues)

        return issues.distinct()
    }

    private fun parseTestInfo() {
        val testInfoByFilenameMatcher = Pattern.compile(testPathRegex).matcher(testDataFile.path)

        if (!testInfoByFilenameMatcher.find()) {
            throw SpecTestValidationException(SpecTestValidationFailedReason.FILENAME_NOT_VALID)
        }

        val fileContent = testDataFile.readText()
        val testInfoByContentMatcher = Pattern.compile(testContentMetaInfoRegex).matcher(fileContent)

        if (!testInfoByContentMatcher.find()) {
            throw SpecTestValidationException(SpecTestValidationFailedReason.METAINFO_NOT_VALID)
        }

        val testCasesMatcher = Pattern.compile(testCaseInfo).matcher(fileContent)
        val testCases = getTestCasesInfo(testCasesMatcher, testInfoByContentMatcher)

        testInfoByFilename = getTestInfo(testInfoByFilenameMatcher)
        testInfoByContent = getTestInfo(
            testInfoByContentMatcher,
            withDetails = true,
            directMappedTestTypeEnum = true,
            testCases = testCases,
            unexpectedBehavior = hasUnexpectedBehavior(testCases, testInfoByContentMatcher),
            issues = getIssues(testCases, testInfoByContentMatcher)
        )

        if (testInfoByFilename != testInfoByContent) {
            throw SpecTestValidationException(SpecTestValidationFailedReason.FILENAME_AND_METAINFO_NOT_CONSISTENCY)
        }
    }

    fun validateByTestInfo() {
        this.parseTestInfo()
    }

    fun printTestInfo() {
        if (!this::testInfoByContent.isInitialized || !this::testInfoByFilename.isInitialized) {
            throw SpecTestValidationException(SpecTestValidationFailedReason.NOT_PARSED)
        }

        val specSentenceUrl =
            "$specUrl#${testInfoByFilename.sectionName}:${testInfoByFilename.paragraphNumber}:${testInfoByFilename.sentenceNumber}"

        println("--------------------------------------------------")
        if (testInfoByContent.unexpectedBehavior) {
            println("(!!!) HAS UNEXPECTED BEHAVIOUR (!!!)")
        }
        println("${testInfoByFilename.testType} ${testArea.title} SPEC TEST")
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