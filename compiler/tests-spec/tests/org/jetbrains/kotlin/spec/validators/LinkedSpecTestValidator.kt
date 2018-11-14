/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.validators

import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern

enum class LinkedSpecTestFileInfoElementType(
    override val valuePattern: Pattern? = null,
    override val required: Boolean = false
) : SpecTestInfoElementType {
    SECTIONS(
        Pattern.compile("""\w+(,\s+\w+)*"""),
        required = true
    ),
    PARAGRAPH(required = true),
    SENTENCE(
        valuePattern = Pattern.compile("""\[(?<number>${AbstractSpecTestValidator.INTEGER_REGEX})\](?<text>.*?)"""),
        required = true
    ),
    NUMBER(required = true),
    DESCRIPTION(required = true),
    ISSUES(valuePattern = Pattern.compile("""(KT-[1-9]\d*)(,\s*KT-[1-9]\d*)*""")),
    UNEXPECTED_BEHAVIOUR,
    DISCUSSION,
    NOTE
}

class LinkedSpecTest(
    testArea: TestArea,
    testType: TestType,
    val sections: List<String>,
    val paragraphNumber: Int,
    val sentenceNumber: Int,
    val sentence: String? = null,
    testNumber: Int,
    description: String? = null,
    cases: List<SpecTestCase>? = null,
    unexpectedBehavior: Boolean? = null,
    issues: Set<String>? = null
) : AbstractSpecTest(testArea, testType, sections[0], testNumber, description, cases, unexpectedBehavior, issues) {
    override fun checkConsistency(other: AbstractSpecTest) =
        other is LinkedSpecTest
                && this.sections == other.sections
                && this.testArea == other.testArea
                && this.testType == other.testType
                && this.testNumber == other.testNumber
                && this.paragraphNumber == other.paragraphNumber
                && this.sentenceNumber == other.sentenceNumber
}

class LinkedSpecTestValidator(testDataFile: File) : AbstractSpecTestValidator<LinkedSpecTest>(testDataFile) {
    override val testPathPattern = getPathPattern()
    override val testInfoPattern: Pattern =
        Pattern.compile(multilineCommentRegex.format("""KOTLIN $testAreaRegex SPEC TEST \($testTypeRegex\)$lineSeparator(?<infoElements>[\s\S]*?$lineSeparator)"""))

    companion object : SpecTestValidatorHelperObject {
        override val pathPartRegex =
            """${SpecTestLinkedType.LINKED.testDataPath}$pathSeparator(?<sections>(?:[\w-]+)(?:$pathSeparator[\w-]+)*?)${pathSeparator}p-(?<paragraphNumber>$INTEGER_REGEX)"""
        override val filenameRegex = """(?<sentenceNumber>$INTEGER_REGEX)\.(?<testNumber>$INTEGER_REGEX)\.kt"""
        override fun getPathPattern(): Pattern = Pattern.compile(testPathRegexTemplate.format(pathPartRegex, filenameRegex))
    }

    override fun getTestInfo(
        testInfoMatcher: Matcher,
        testInfoElements: SpecTestInfoElements<SpecTestInfoElementType>,
        testCases: List<SpecTestCase>,
        unexpectedBehavior: Boolean,
        issues: Set<String>?
    ): LinkedSpecTest {
        val sentenceMatcher = testInfoElements[LinkedSpecTestFileInfoElementType.SENTENCE]!!.additionalMatcher!!

        return LinkedSpecTest(
            TestArea.valueOf(testInfoMatcher.group("testArea").replace(" ", "_").toUpperCase()),
            TestType.valueOf(testInfoMatcher.group("testType")),
            testInfoElements[LinkedSpecTestFileInfoElementType.SECTIONS]!!.content.split(Regex(""",\s*""")),
            testInfoElements[LinkedSpecTestFileInfoElementType.PARAGRAPH]!!.content.toInt(),
            sentenceMatcher.group("number").toInt(),
            sentenceMatcher.group("text"),
            testInfoElements[LinkedSpecTestFileInfoElementType.NUMBER]!!.content.toInt(),
            testInfoElements[LinkedSpecTestFileInfoElementType.DESCRIPTION]!!.content,
            testCases,
            unexpectedBehavior,
            issues
        )
    }

    override fun getTestInfo(testInfoMatcher: Matcher) =
        LinkedSpecTest(
            TestArea.valueOf(testInfoMatcher.group("testArea").replace(File.separator, "_").toUpperCase()),
            TestType.fromValue(testInfoMatcher.group("testType"))!!,
            testInfoMatcher.group("sections").split(File.separator),
            testInfoMatcher.group("paragraphNumber").toInt(),
            testInfoMatcher.group("sentenceNumber").toInt(),
            testNumber = testInfoMatcher.group("testNumber").toInt()
        )

    override fun parseTestInfo() = parseTestInfo(LinkedSpecTestFileInfoElementType.values())

    override fun printTestInfo() {
        println("--------------------------------------------------")
        if (testInfoByContent.unexpectedBehavior!!)
            println("(!!!) HAS UNEXPECTED BEHAVIOUR (!!!)")
        println("${testInfoByFilename.testArea.name.replace("_", " ")} ${testInfoByFilename.testType} SPEC TEST")
        println("SECTIONS: ${testInfoByContent.sections} (paragraph: ${testInfoByFilename.paragraphNumber})")
        println("SENTENCE ${testInfoByContent.sentenceNumber}: ${testInfoByContent.sentence}")
        println("TEST NUMBER: ${testInfoByContent.testNumber}")
        println("NUMBER OF TEST CASES: ${testInfoByContent.cases!!.size}")
        println("DESCRIPTION: ${testInfoByContent.description}")
        if (testInfoByContent.issues!!.isNotEmpty())
            println("LINKED ISSUES: ${testInfoByContent.issues!!.joinToString { "${ISSUE_TRACKER + it}," }}")
    }

    override fun getSingleTestCase(testInfoElements: SpecTestInfoElements<SpecTestInfoElementType>) =
        SpecTestCase(
            1,
            description = testInfoElements[LinkedSpecTestFileInfoElementType.DESCRIPTION]!!.content,
            unexpectedBehavior = testInfoElements.contains(LinkedSpecTestFileInfoElementType.UNEXPECTED_BEHAVIOUR),
            issues = parseIssues(testInfoElements[LinkedSpecTestFileInfoElementType.ISSUES])
        )
}
