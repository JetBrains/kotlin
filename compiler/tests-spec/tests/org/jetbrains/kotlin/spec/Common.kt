/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec

import org.jetbrains.kotlin.TestsExceptionType
import org.jetbrains.kotlin.spec.models.LinkedSpecTestFileInfoElementType
import org.jetbrains.kotlin.spec.models.NotLinkedSpecTestFileInfoElementType
import org.jetbrains.kotlin.spec.parsers.BasePatterns
import org.jetbrains.kotlin.spec.parsers.LinkedSpecTestPatterns
import org.jetbrains.kotlin.spec.parsers.NotLinkedSpecTestPatterns
import org.jetbrains.kotlin.spec.parsers.CommonParser.withSpaces
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

typealias TestFiles = Map<String, String>
typealias TestCasesByNumbers = MutableMap<Int, SpecTestCase>
typealias TestCasesByFiles = MutableMap<String, TestCasesByNumbers>
typealias TestCasesByRanges = MutableMap<String, NavigableMap<Int, TestCasesByNumbers>>

enum class TestType(val type: String) {
    POSITIVE("pos"),
    NEGATIVE("neg");

    companion object {
        private val map = values().associateBy(TestType::type)
        val joinedValues = values().joinToString("|").withSpaces()

        fun fromValue(type: String) = map[type]
    }
}

enum class TestArea(val testDataPath: String) {
    PSI("psi"),
    DIAGNOSTICS("diagnostics"),
    CODEGEN_BOX("codegen/box");

    companion object {
        val joinedValues = values().joinToString("|").withSpaces()
    }
}

enum class SpecTestLinkedType(
    val testDataPath: String,
    val patterns: Lazy<BasePatterns>,
    val infoElements: Lazy<Array<out SpecTestInfoElementType>>
) {
    LINKED(
        "linked",
        lazy { LinkedSpecTestPatterns },
        lazy { LinkedSpecTestFileInfoElementType.values() }
    ),
    NOT_LINKED(
        "notLinked",
        lazy { NotLinkedSpecTestPatterns },
        lazy { NotLinkedSpecTestFileInfoElementType.values() }
    )
}

interface SpecTestInfoElementType {
    val valuePattern: Pattern?
    val required: Boolean
}

data class SpecTestInfoElementContent(
    val content: String,
    val additionalMatcher: Matcher? = null
)

data class SpecTestCase(
    var code: String,
    var ranges: MutableList<IntRange>,
    var unexpectedBehavior: Boolean,
    var unspecifiedBehavior: Boolean,
    val issues: MutableList<String>?,
    val exception: TestsExceptionType?
)

data class SpecTestCasesSet(
    val byFiles: TestCasesByFiles,
    val byRanges: TestCasesByRanges,
    val byNumbers: TestCasesByNumbers
)