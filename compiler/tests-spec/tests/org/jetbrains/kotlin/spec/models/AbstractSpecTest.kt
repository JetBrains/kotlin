/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.models

import org.jetbrains.kotlin.TestsExceptionType
import org.jetbrains.kotlin.spec.*
import org.jetbrains.kotlin.spec.parsers.CommonPatterns
import org.jetbrains.kotlin.spec.parsers.CommonPatterns.issuesPattern
import org.jetbrains.kotlin.spec.parsers.LinkedSpecTestPatterns.relevantPlacesPattern
import org.jetbrains.kotlin.spec.parsers.TestCasePatterns.testCaseNumberPattern
import java.util.regex.Matcher
import java.util.regex.Pattern

typealias SpecTestInfoElements<T> = Map<T, SpecTestInfoElementContent>

enum class CommonInfoElementType(
    override val valuePattern: Pattern? = null,
    override val required: Boolean = false
) : SpecTestInfoElementType {
    UNEXPECTED_BEHAVIOUR,
    ISSUES(valuePattern = issuesPattern),
    DISCUSSION,
    NOTE,
    EXCEPTION
}

enum class CommonSpecTestFileInfoElementType(
    override val valuePattern: Pattern? = null,
    override val required: Boolean = false
) : SpecTestInfoElementType {
    NUMBER(required = true),
    DESCRIPTION(required = true),
    HELPERS
}

enum class SpecTestCaseInfoElementType(
    override val valuePattern: Pattern? = null,
    override val required: Boolean = false
) : SpecTestInfoElementType {
    TESTCASE_NUMBER(valuePattern = testCaseNumberPattern, required = true),
    RELEVANT_PLACES(valuePattern = relevantPlacesPattern),
    UNSPECIFIED_BEHAVIOR
}

abstract class AbstractSpecTest(
    val testArea: TestArea,
    val testType: TestType,
    val sections: List<String>,
    val testNumber: Int,
    val description: String,
    val cases: SpecTestCasesSet,
    val unexpectedBehavior: Boolean,
    val issues: Set<String>,
    val helpers: Set<String>?,
    val exception: TestsExceptionType?
) {
    companion object {
        private fun issuesToString(issues: Set<String>) = issues.joinToString(", ") { CommonPatterns.ISSUE_TRACKER + it }
    }

    abstract fun checkPathConsistency(pathMatcher: Matcher): Boolean

    protected fun getIssuesText(): String? {
        val testCaseIssues = cases.byNumbers.flatMap { it.value.issues!! }

        return if (issues.isNotEmpty() || testCaseIssues.isNotEmpty()) {
            "LINKED ISSUES: ${issuesToString(issues + testCaseIssues)}"
        } else null
    }

    protected fun getUnexpectedBehaviourText(): String? {
        val separatedTestCasesUnexpectedBehaviorNumber = cases.byNumbers.count { it.value.unexpectedBehavior }
        val testCasesUnexpectedBehaviorNumber = when {
            unexpectedBehavior -> cases.byNumbers.size
            separatedTestCasesUnexpectedBehaviorNumber != 0 -> separatedTestCasesUnexpectedBehaviorNumber
            else -> 0
        }

        return if (testCasesUnexpectedBehaviorNumber != 0) {
            "!!! HAS UNEXPECTED BEHAVIOUR (in $testCasesUnexpectedBehaviorNumber cases) !!!"
        } else null
    }
}
