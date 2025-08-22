/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.utils.models

import org.jetbrains.kotlin.TestsExceptionType
import org.jetbrains.kotlin.spec.utils.SpecTestCasesSet
import org.jetbrains.kotlin.spec.utils.SpecTestInfoElementType
import org.jetbrains.kotlin.spec.utils.TestArea
import org.jetbrains.kotlin.spec.utils.TestType
import org.jetbrains.kotlin.spec.utils.parsers.CommonParser.splitByPathSeparator
import org.jetbrains.kotlin.spec.utils.parsers.CommonParser.withSpaces
import org.jetbrains.kotlin.spec.utils.parsers.CommonParser.withUnderscores
import org.jetbrains.kotlin.spec.utils.parsers.CommonPatterns.ls
import org.jetbrains.kotlin.spec.utils.parsers.LinkedSpecTestPatterns.mainLinkPattern
import org.jetbrains.kotlin.spec.utils.parsers.LinkedSpecTestPatterns.relevantLinksPattern
import java.util.regex.Matcher
import java.util.regex.Pattern

enum class LinkedSpecTestFileInfoElementType(
    override val valuePattern: Pattern? = null,
    override val required: Boolean = false
) : SpecTestInfoElementType {
    SPEC_VERSION(required = true),
    MAIN_LINK(valuePattern = mainLinkPattern),
    PRIMARY_LINKS(valuePattern = relevantLinksPattern),
    SECONDARY_LINKS(valuePattern = relevantLinksPattern),
    UNSPECIFIED_BEHAVIOR;
}

data class SpecPlace(
    val sections: List<String>,
    val paragraphNumber: Int,
    val sentenceNumber: Int
)

class LinkedSpecTest(
    val specVersion: String,
    testArea: TestArea,
    testType: TestType,
    val mainLink: SpecPlace?,
    val primaryLinks: Set<SpecPlace>?,
    val secondaryLinks: Set<SpecPlace>?,
    testNumber: Int,
    description: String,
    cases: SpecTestCasesSet,
    unexpectedBehavior: Boolean,
    private val unspecifiedBehavior: Boolean,
    issues: Set<String>,
    helpers: Set<String>?,
    exception: TestsExceptionType?
) : AbstractSpecTest(
    testArea,
    testType,
    mainLink?.sections ?: listOf(),
    testNumber,
    description,
    cases,
    unexpectedBehavior,
    issues,
    helpers,
    exception
) {

    override fun checkPathConsistency(pathMatcher: Matcher) =
        testArea == TestArea.valueOf(pathMatcher.group("testArea").withUnderscores())
                && testType == TestType.fromValue(pathMatcher.group("testType"))!!
                && sections == pathMatcher.group("sections").splitByPathSeparator()
                && mainLink?.paragraphNumber == pathMatcher.group("paragraphNumber").toInt()
                && mainLink.sentenceNumber == pathMatcher.group("sentenceNumber").toInt()
                && testNumber == pathMatcher.group("testNumber").toInt()

    private fun getUnspecifiedBehaviourText(): String? {
        val separatedTestCasesUnspecifiedBehaviorNumber = cases.byNumbers.count { it.value.unspecifiedBehavior }
        val testCasesUnspecifiedBehaviorNumber = when {
            unspecifiedBehavior -> cases.byNumbers.size
            separatedTestCasesUnspecifiedBehaviorNumber != 0 -> separatedTestCasesUnspecifiedBehaviorNumber
            else -> 0
        }

        return if (testCasesUnspecifiedBehaviorNumber != 0) {
            "!!! HAS UNSPECIFIED BEHAVIOUR (in $testCasesUnspecifiedBehaviorNumber cases) !!!"
        } else null
    }

    override fun toString() = buildString {
        append("--------------------------------------------------$ls")
        getUnspecifiedBehaviourText()?.let { append(it + ls) }
        super.getUnexpectedBehaviourText()?.let { append(it + ls) }
        append("${testArea.name.withSpaces()} $testType SPEC TEST (${testType.toString().withSpaces()})$ls")
        append("SPEC VERSION: $specVersion$ls")
        mainLink?.let { append("MAIN LINK: ${sections.joinToString()} -> paragraph: ${mainLink.paragraphNumber} -> sentence: ${mainLink.sentenceNumber}$ls") }
        primaryLinks?.let { append("PRIMARY LINKS: ${primaryLinks.buildToString()}$ls") }
        secondaryLinks?.let { append("SECONDARY LINKS: ${secondaryLinks.buildToString()}$ls") }
        append("NUMBER: $testNumber$ls")
        append("TEST CASES: ${cases.byNumbers.size.coerceAtLeast(1)}$ls")
        append("DESCRIPTION: $description$ls")
        super.getIssuesText()?.let { append(it + ls) }
    }

    private fun Set<SpecPlace>.buildToString(): String = buildString {
        this@buildToString.forEach {
            append("${sections.joinToString()} -> paragraph: ${it.paragraphNumber} -> sentence: ${it.sentenceNumber}$ls")
        }
    }

}
