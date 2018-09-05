/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.validators

import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern

enum class NotLinkedSpecTestFileInfoElementType(
    override val valuePattern: Pattern? = null,
    override val required: Boolean = false
) : SpecTestInfoElementType {
    SECTION(required = true),
    CATEGORY(
        valuePattern = Pattern.compile("""\w+(,\s+\w+)*"""),
        required = true
    ),
    NUMBER(required = true),
    DESCRIPTION(required = true),
    ISSUES(valuePattern = LinkedSpecTestFileInfoElementType.ISSUES.valuePattern),
    UNEXPECTED_BEHAVIOUR,
    DISCUSSION,
    NOTE
}

class NotLinkedSpecTest(
    testArea: TestArea,
    testType: TestType,
    section: String,
    val categories: List<String>,
    testNumber: Int,
    description: String? = null,
    cases: List<SpecTestCase>? = null,
    unexpectedBehavior: Boolean? = null,
    issues: Set<String>? = null
) : AbstractSpecTest(testArea, testType, section, testNumber, description, cases, unexpectedBehavior, issues) {
    override fun checkConsistency(other: AbstractSpecTest) =
        other is NotLinkedSpecTest
                && this.section == other.section
                && this.testArea == other.testArea
                && this.testType == other.testType
                && this.categories.joinToString(",") == other.categories.joinToString(",")
                && this.testNumber == other.testNumber
}

class NotLinkedSpecTestValidator(testDataFile: File) : AbstractSpecTestValidator<NotLinkedSpecTest>(testDataFile) {
    override val testPathPattern = getPathPattern()
    override val testInfoPattern: Pattern =
        Pattern.compile(MULTILINE_COMMENT_REGEX.format("""KOTLIN $testAreaRegex NOT LINKED SPEC TEST \($testTypeRegex\)$lineSeparator(?<infoElements>[\s\S]*?$lineSeparator)"""))

    companion object : SpecTestValidatorHelperObject {
        override val pathPartRegex =
            """${dirsByLinkedType[SpecTestLinkedType.NOT_LINKED]}$pathSeparator(?<section>[\w-]+)$pathSeparator(?<categories>(?:[\w-]+)(?:/[\w-]+)*?)"""
        override val filenameRegex = """(?<testNumber>$INTEGER_REGEX)\.kt"""
        override fun getPathPattern(): Pattern = Pattern.compile(testPathRegexTemplate.format(pathPartRegex, filenameRegex))
    }

    override fun getTestInfo(
        testInfoMatcher: Matcher,
        testInfoElements: SpecTestInfoElements<SpecTestInfoElementType>,
        testCases: List<SpecTestCase>,
        unexpectedBehavior: Boolean,
        issues: Set<String>?
    ) =
        NotLinkedSpecTest(
            TestArea.valueOf(testInfoMatcher.group("testArea").toUpperCase()),
            TestType.valueOf(testInfoMatcher.group("testType")),
            testInfoElements[NotLinkedSpecTestFileInfoElementType.SECTION]!!.content,
            testInfoElements[NotLinkedSpecTestFileInfoElementType.CATEGORY]!!.content.split(Regex(""",\s*""")),
            testInfoElements[NotLinkedSpecTestFileInfoElementType.NUMBER]!!.content.toInt(),
            testInfoElements[NotLinkedSpecTestFileInfoElementType.DESCRIPTION]!!.content,
            testCases,
            unexpectedBehavior,
            issues
        )

    override fun getTestInfo(testInfoMatcher: Matcher) =
        NotLinkedSpecTest(
            TestArea.valueOf(testInfoMatcher.group("testArea").toUpperCase()),
            TestType.fromValue(testInfoMatcher.group("testType"))!!,
            testInfoMatcher.group("section"),
            testInfoMatcher.group("categories").split("/"),
            testNumber = testInfoMatcher.group("testNumber").toInt()
        )

    override fun parseTestInfo() = parseTestInfo(NotLinkedSpecTestFileInfoElementType.values())

    override fun printTestInfo() {
        println("--------------------------------------------------")
        if (testInfoByContent.unexpectedBehavior!!)
            println("(!!!) HAS UNEXPECTED BEHAVIOUR (!!!)")
        println("${testInfoByFilename.testArea} ${testInfoByFilename.testType} NOT LINKED SPEC TEST")
        println("SECTION: ${testInfoByContent.section}")
        println("CATEGORIES: ${testInfoByContent.categories.joinToString(", ")}")
        println("TEST NUMBER: ${testInfoByContent.testNumber}")
        println("NUMBER OF TEST CASES: ${testInfoByContent.cases!!.size}")
        println("DESCRIPTION: ${testInfoByContent.description}")
        if (testInfoByContent.issues!!.isNotEmpty())
            println("LINKED ISSUES: ${testInfoByContent.issues!!.map { ISSUE_TRACKER + it }.joinToString(", ")}")
    }

    override fun getSingleTestCase(testInfoElements: SpecTestInfoElements<SpecTestInfoElementType>) =
        SpecTestCase(
            1,
            description = testInfoElements[NotLinkedSpecTestFileInfoElementType.DESCRIPTION]!!.content,
            unexpectedBehavior = testInfoElements.contains(NotLinkedSpecTestFileInfoElementType.UNEXPECTED_BEHAVIOUR),
            issues = parseIssues(testInfoElements[NotLinkedSpecTestFileInfoElementType.ISSUES])
        )
}