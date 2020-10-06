/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.utils

import org.jetbrains.kotlin.TestsExceptionType
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.LINKED_TESTS_PATH
import org.jetbrains.kotlin.spec.utils.models.LinkedSpecTestFileInfoElementType
import org.jetbrains.kotlin.spec.utils.models.NotLinkedSpecTestFileInfoElementType
import org.jetbrains.kotlin.spec.utils.parsers.BasePatterns
import org.jetbrains.kotlin.spec.utils.parsers.LinkedSpecTestPatterns
import org.jetbrains.kotlin.spec.utils.parsers.NotLinkedSpecTestPatterns
import org.jetbrains.kotlin.spec.utils.parsers.CommonParser.withSpaces
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

enum class TestOrigin(private val testDataPath: String, private val testsPath: String? = null) {
    IMPLEMENTATION(GeneralConfiguration.TESTDATA_PATH),
    SPEC(GeneralConfiguration.SPEC_TESTDATA_PATH, LINKED_TESTS_PATH);

    fun getFilePath(testArea: TestArea) = buildString {
        append("${testDataPath}/${testArea.testDataPath}")
        if (testsPath != null)
            append("/${testsPath}")
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