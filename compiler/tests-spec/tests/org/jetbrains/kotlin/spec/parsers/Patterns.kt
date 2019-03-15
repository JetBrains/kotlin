/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.parsers

import org.jetbrains.kotlin.spec.models.SpecTestCaseInfoElementType
import org.jetbrains.kotlin.spec.SpecTestLinkedType
import org.jetbrains.kotlin.spec.TestArea
import org.jetbrains.kotlin.spec.TestType
import org.jetbrains.kotlin.spec.parsers.CommonPatterns.INTEGER_REGEX
import org.jetbrains.kotlin.spec.parsers.CommonPatterns.SINGLE_LINE_COMMENT_REGEX
import org.jetbrains.kotlin.spec.parsers.CommonPatterns.ASTERISK_REGEX
import org.jetbrains.kotlin.spec.parsers.CommonPatterns.directiveRegex
import org.jetbrains.kotlin.spec.parsers.CommonPatterns.MULTILINE_COMMENT_REGEX
import org.jetbrains.kotlin.spec.parsers.CommonPatterns.ps
import org.jetbrains.kotlin.spec.parsers.CommonPatterns.testAreaRegex
import org.jetbrains.kotlin.spec.parsers.CommonPatterns.testPathRegexTemplate
import org.jetbrains.kotlin.spec.parsers.CommonPatterns.testTypeRegex
import org.jetbrains.kotlin.spec.parsers.CommonParser.withSpaces
import org.jetbrains.kotlin.spec.parsers.CommonPatterns.SECTIONS_IN_FILE_REGEX
import org.jetbrains.kotlin.spec.parsers.CommonPatterns.sectionsInPathRegex
import java.io.File
import java.util.regex.Pattern

object CommonPatterns {
    const val ISSUE_TRACKER = "https://youtrack.jetbrains.com/issue/"
    const val INTEGER_REGEX = """[1-9]\d*"""
    const val SINGLE_LINE_COMMENT_REGEX = """\/\/\s*%s"""
    const val ASTERISK_REGEX = """\*"""
    const val SECTIONS_IN_FILE_REGEX = """[\w-]+(,\s+[\w-]+)*"""
    const val MULTILINE_COMMENT_REGEX = """\/\*\s+?%s\s+\*\/(?:\n)*"""

    val ls: String = System.lineSeparator()
    val ps: String = Pattern.quote(File.separator)

    val directiveRegex =
        """${SINGLE_LINE_COMMENT_REGEX.format("""[\w\s]+:""")}|${MULTILINE_COMMENT_REGEX.format(""" $ASTERISK_REGEX [\w\s]+:[\s\S]*?""")}"""
    val testAreaRegex = """(?<testArea>${TestArea.joinedValues})"""
    val testTypeRegex = """(?<testType>${TestType.joinedValues})"""
    val testInfoElementPattern: Pattern = Pattern.compile("""(?: \* )?(?<name>[A-Z ]+?)(?::\s*(?<value>.*?))?\n""")
    val testPathBaseRegexTemplate = """^.*?$ps(?<testArea>diagnostics|psi|(?:codegen${ps}box))$ps%s"""
    val testPathRegexTemplate = """$testPathBaseRegexTemplate$ps(?<testType>pos|neg)$ps%s$"""
    val issuesPattern: Pattern = Pattern.compile("""(KT-[1-9]\d*)(,\s*KT-[1-9]\d*)*""")
    val sectionsInFilePattern: Pattern = Pattern.compile("""(?<sections>$SECTIONS_IN_FILE_REGEX)""")
    val sectionsInPathRegex = """(?<sections>(?:[\w-]+)(?:$ps[\w-]+)*?)"""
    val packagePattern: Pattern = Pattern.compile("""(?:^|\n)package (?<packageName>.*?)(?:;|\n)""")
}

interface BasePatterns {
    val pathPartRegex: String
    val testPathPattern: Pattern
    val testInfoPattern: Pattern
}

object NotLinkedSpecTestPatterns : BasePatterns {
    private const val FILENAME_REGEX = """(?<testNumber>$INTEGER_REGEX)\.kt"""

    override val pathPartRegex = SpecTestLinkedType.NOT_LINKED.testDataPath + ps + sectionsInPathRegex
    override val testPathPattern: Pattern =
        Pattern.compile(testPathRegexTemplate.format(pathPartRegex, FILENAME_REGEX))
    override val testInfoPattern: Pattern =
        Pattern.compile(MULTILINE_COMMENT_REGEX.format(""" $ASTERISK_REGEX KOTLIN $testAreaRegex NOT LINKED SPEC TEST \($testTypeRegex\)\n(?<infoElements>[\s\S]*?\n)"""))
}

object LinkedSpecTestPatterns : BasePatterns {
    private const val FILENAME_REGEX = """(?<sentenceNumber>$INTEGER_REGEX)\.(?<testNumber>$INTEGER_REGEX)\.kt"""

    override val pathPartRegex =
        """${SpecTestLinkedType.LINKED.testDataPath}$ps$sectionsInPathRegex${ps}p-(?<paragraphNumber>$INTEGER_REGEX)"""
    override val testPathPattern: Pattern =
        Pattern.compile(testPathRegexTemplate.format(pathPartRegex, FILENAME_REGEX))
    override val testInfoPattern: Pattern =
        Pattern.compile(MULTILINE_COMMENT_REGEX.format(""" $ASTERISK_REGEX KOTLIN $testAreaRegex SPEC TEST \($testTypeRegex\)\n(?<infoElements>[\s\S]*?\n)"""))

    val placePattern: Pattern =
        Pattern.compile("""(?<sections>$SECTIONS_IN_FILE_REGEX) -> paragraph (?<paragraphNumber>$INTEGER_REGEX) -> sentence (?<sentenceNumber>$INTEGER_REGEX)""")

    val relevantPlacesPattern: Pattern =
        Pattern.compile("""(( $ASTERISK_REGEX )?\s*((?<sections>$SECTIONS_IN_FILE_REGEX) -> )?(paragraph (?<paragraphNumber>$INTEGER_REGEX) -> )?sentence (?<sentenceNumber>$INTEGER_REGEX))+""")
}

object TestCasePatterns {
    private const val TEST_CASE_CODE_REGEX = """(?<%s>[\s\S]*?)"""

    private val testCaseInfoElementsRegex = """(?<%s>%s${SpecTestCaseInfoElementType.TESTCASE_NUMBER.name.withSpaces()}:%s*?\n)"""
    private val testCaseInfoRegex = """$TEST_CASE_CODE_REGEX(?<%s>(?:$directiveRegex)|$)"""
    private val testCaseInfoSingleLineRegex =
        SINGLE_LINE_COMMENT_REGEX.format(
            testCaseInfoElementsRegex.format("infoElementsSL", "", """\s*.""")
        ) + testCaseInfoRegex.format("codeSL", "nextDirectiveSL")
    private val testCaseInfoMultilineRegex =
        MULTILINE_COMMENT_REGEX.format(
            testCaseInfoElementsRegex.format("infoElementsML", """ $ASTERISK_REGEX """, """[\s\S]""")
        ) + testCaseInfoRegex.format("codeML", "nextDirectiveML")

    val testCaseInfoPattern: Pattern = Pattern.compile("(?:$testCaseInfoSingleLineRegex)|(?:$testCaseInfoMultilineRegex)")
    val testCaseNumberPattern: Pattern = Pattern.compile("""([1-9]\d*)(,\s*[1-9]\d*)*""")
}
