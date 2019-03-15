/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.models

import org.jetbrains.kotlin.TestsExceptionType
import org.jetbrains.kotlin.spec.SpecTestCasesSet
import org.jetbrains.kotlin.spec.SpecTestInfoElementType
import org.jetbrains.kotlin.spec.TestArea
import org.jetbrains.kotlin.spec.TestType
import org.jetbrains.kotlin.spec.parsers.CommonParser.splitByPathSeparator
import org.jetbrains.kotlin.spec.parsers.CommonParser.withUnderscores
import org.jetbrains.kotlin.spec.parsers.CommonParser.withSpaces
import org.jetbrains.kotlin.spec.parsers.CommonPatterns.ls
import org.jetbrains.kotlin.spec.parsers.CommonPatterns.sectionsInFilePattern
import java.util.regex.Matcher
import java.util.regex.Pattern

enum class NotLinkedSpecTestFileInfoElementType(
    override val valuePattern: Pattern? = null,
    override val required: Boolean = false
) : SpecTestInfoElementType {
    SECTIONS(valuePattern = sectionsInFilePattern, required = true)
}

class NotLinkedSpecTest(
    testArea: TestArea,
    testType: TestType,
    sections: List<String>,
    testNumber: Int,
    description: String,
    cases: SpecTestCasesSet,
    unexpectedBehavior: Boolean,
    issues: Set<String>,
    helpers: Set<String>?,
    exception: TestsExceptionType?
) : AbstractSpecTest(testArea, testType, sections, testNumber, description, cases, unexpectedBehavior, issues, helpers, exception) {
    override fun checkPathConsistency(pathMatcher: Matcher) =
        testArea == TestArea.valueOf(pathMatcher.group("testArea").withUnderscores())
                && testType == TestType.fromValue(pathMatcher.group("testType"))!!
                && sections == pathMatcher.group("sections").splitByPathSeparator()
                && testNumber == pathMatcher.group("testNumber").toInt()

    override fun toString() = buildString {
        append("--------------------------------------------------$ls")
        super.getUnexpectedBehaviourText()?.let { append(it + ls) }
        append("${testArea.name.withSpaces()} $testType SPEC TEST (${testType.type.withSpaces()})$ls")
        append("SECTIONS: ${sections.joinToString()}$ls")
        append("NUMBER: $testNumber$ls")
        append("TEST CASES: ${cases.byNumbers.size.coerceAtLeast(1)}$ls")
        append("DESCRIPTION: $description$ls")
        super.getIssuesText()?.let { append(it + ls) }
    }
}
