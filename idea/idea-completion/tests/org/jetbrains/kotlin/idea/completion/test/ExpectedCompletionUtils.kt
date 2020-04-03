/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test

import com.google.common.collect.ImmutableList
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.ui.JBColor
import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.idea.completion.LookupElementFactory
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject
import org.jetbrains.kotlin.idea.test.AstAccessControl
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.junit.Assert
import java.util.*

/**
 * Extract a number of statements about completion from the given text. Those statements
 * should be asserted during test execution.
 */
object ExpectedCompletionUtils {

    class CompletionProposal {
        private val map: Map<String, String?>

        constructor(lookupString: String) {
            map = HashMap<String, String?>()
            map.put(LOOKUP_STRING, lookupString)
        }

        constructor(map: MutableMap<String, String?>) {
            this.map = map
            for (key in map.keys) {
                if (key !in validKeys) {
                    throw RuntimeException("Invalid key '$key'")
                }
            }
        }

        constructor(json: JsonObject) {
            map = HashMap<String, String?>()
            for (entry in json.entrySet()) {
                val key = entry.key
                if (key !in validKeys) {
                    throw RuntimeException("Invalid json property '$key'")
                }
                val value = entry.value
                map.put(key, if (value !is JsonNull) value.asString else null)
            }
        }

        fun matches(expectedProposal: CompletionProposal): Boolean = expectedProposal.map.entries.none { it.value != map[it.key] }

        override fun toString(): String {
            val jsonObject = JsonObject()
            for ((key, value) in map) {
                jsonObject.addProperty(key, value)
            }
            return jsonObject.toString()
        }

        companion object {
            const val LOOKUP_STRING: String = "lookupString"
            const val ALL_LOOKUP_STRINGS: String = "allLookupStrings"
            const val PRESENTATION_ITEM_TEXT: String = "itemText"
            const val PRESENTATION_TYPE_TEXT: String = "typeText"
            const val PRESENTATION_TAIL_TEXT: String = "tailText"
            const val PRESENTATION_TEXT_ATTRIBUTES: String = "attributes"
            const val MODULE_NAME: String = "module"
            val validKeys: Set<String> = setOf(
                LOOKUP_STRING, ALL_LOOKUP_STRINGS, PRESENTATION_ITEM_TEXT, PRESENTATION_TYPE_TEXT,
                PRESENTATION_TAIL_TEXT, PRESENTATION_TEXT_ATTRIBUTES, MODULE_NAME
            )
        }
    }

    private val UNSUPPORTED_PLATFORM_MESSAGE =
        "Only ${JvmPlatforms.unspecifiedJvmPlatform} and ${JsPlatforms.defaultJsPlatform} platforms are supported"

    private const val EXIST_LINE_PREFIX = "EXIST:"

    private const val ABSENT_LINE_PREFIX = "ABSENT:"
    private const val ABSENT_JS_LINE_PREFIX = "ABSENT_JS:"
    private const val ABSENT_JAVA_LINE_PREFIX = "ABSENT_JAVA:"

    private const val EXIST_JAVA_ONLY_LINE_PREFIX = "EXIST_JAVA_ONLY:"
    private const val EXIST_JS_ONLY_LINE_PREFIX = "EXIST_JS_ONLY:"

    private const val NUMBER_LINE_PREFIX = "NUMBER:"
    private const val NUMBER_JS_LINE_PREFIX = "NUMBER_JS:"
    private const val NUMBER_JAVA_LINE_PREFIX = "NUMBER_JAVA:"

    private const val NOTHING_ELSE_PREFIX = "NOTHING_ELSE"
    private const val RUN_HIGHLIGHTING_BEFORE_PREFIX = "RUN_HIGHLIGHTING_BEFORE"

    private const val INVOCATION_COUNT_PREFIX = "INVOCATION_COUNT:"
    private const val WITH_ORDER_PREFIX = "WITH_ORDER"
    private const val AUTOCOMPLETE_SETTING_PREFIX = "AUTOCOMPLETE_SETTING:"

    const val RUNTIME_TYPE: String = "RUNTIME_TYPE:"

    private const val COMPLETION_TYPE_PREFIX = "COMPLETION_TYPE:"

    val KNOWN_PREFIXES: List<String> = ImmutableList.of(
        "LANGUAGE_VERSION:",
        EXIST_LINE_PREFIX,
        ABSENT_LINE_PREFIX,
        ABSENT_JS_LINE_PREFIX,
        ABSENT_JAVA_LINE_PREFIX,
        EXIST_JAVA_ONLY_LINE_PREFIX,
        EXIST_JS_ONLY_LINE_PREFIX,
        NUMBER_LINE_PREFIX,
        NUMBER_JS_LINE_PREFIX,
        NUMBER_JAVA_LINE_PREFIX,
        INVOCATION_COUNT_PREFIX,
        WITH_ORDER_PREFIX,
        AUTOCOMPLETE_SETTING_PREFIX,
        NOTHING_ELSE_PREFIX,
        RUN_HIGHLIGHTING_BEFORE_PREFIX,
        RUNTIME_TYPE,
        COMPLETION_TYPE_PREFIX,
        LightClassComputationControl.LIGHT_CLASS_DIRECTIVE,
        AstAccessControl.ALLOW_AST_ACCESS_DIRECTIVE
    )

    fun itemsShouldExist(fileText: String, platform: TargetPlatform?): Array<CompletionProposal> = when {
        platform.isJvm() -> processProposalAssertions(fileText, EXIST_LINE_PREFIX, EXIST_JAVA_ONLY_LINE_PREFIX)
        platform.isJs() -> processProposalAssertions(fileText, EXIST_LINE_PREFIX, EXIST_JS_ONLY_LINE_PREFIX)
        platform == null -> processProposalAssertions(fileText, EXIST_LINE_PREFIX)
        else -> throw IllegalArgumentException(UNSUPPORTED_PLATFORM_MESSAGE)
    }

    fun itemsShouldAbsent(fileText: String, platform: TargetPlatform?): Array<CompletionProposal> = when {
        platform.isJvm() -> processProposalAssertions(fileText, ABSENT_LINE_PREFIX, ABSENT_JAVA_LINE_PREFIX, EXIST_JS_ONLY_LINE_PREFIX)
        platform.isJs() -> processProposalAssertions(fileText, ABSENT_LINE_PREFIX, ABSENT_JS_LINE_PREFIX, EXIST_JAVA_ONLY_LINE_PREFIX)
        platform == null -> processProposalAssertions(fileText, ABSENT_LINE_PREFIX)
        else -> throw IllegalArgumentException(UNSUPPORTED_PLATFORM_MESSAGE)
    }

    fun processProposalAssertions(fileText: String, vararg prefixes: String): Array<CompletionProposal> {
        val proposals = ArrayList<CompletionProposal>()
        for (proposalStr in InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, *prefixes)) {
            if (proposalStr.startsWith("{")) {
                val parser = JsonParser()
                val json: JsonElement? = try {
                    parser.parse(proposalStr)
                } catch (t: Throwable) {
                    throw RuntimeException("Error parsing '$proposalStr'", t)
                }
                proposals.add(CompletionProposal(json as JsonObject))
            } else if (proposalStr.startsWith("\"") && proposalStr.endsWith("\"")) {
                proposals.add(CompletionProposal(proposalStr.substring(1, proposalStr.length - 1)))
            } else {
                for (item in proposalStr.split(",")) {
                    proposals.add(CompletionProposal(item.trim()))
                }
            }
        }

        return ArrayUtil.toObjectArray(proposals, CompletionProposal::class.java)
    }

    fun getExpectedNumber(fileText: String, platform: TargetPlatform?): Int? = when {
        platform == null -> InTextDirectivesUtils.getPrefixedInt(fileText, NUMBER_LINE_PREFIX)
        platform.isJvm() -> getPlatformExpectedNumber(fileText, NUMBER_JAVA_LINE_PREFIX)
        platform.isJs() -> getPlatformExpectedNumber(fileText, NUMBER_JS_LINE_PREFIX)
        else -> throw IllegalArgumentException(UNSUPPORTED_PLATFORM_MESSAGE)
    }

    fun isNothingElseExpected(fileText: String): Boolean =
        InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, NOTHING_ELSE_PREFIX).isNotEmpty()

    fun shouldRunHighlightingBeforeCompletion(fileText: String): Boolean =
        InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, RUN_HIGHLIGHTING_BEFORE_PREFIX).isNotEmpty()

    fun getInvocationCount(fileText: String): Int? = InTextDirectivesUtils.getPrefixedInt(fileText, INVOCATION_COUNT_PREFIX)

    fun getCompletionType(fileText: String): CompletionType? =
        when (val completionTypeString = InTextDirectivesUtils.findStringWithPrefixes(fileText, COMPLETION_TYPE_PREFIX)) {
            "BASIC" -> CompletionType.BASIC
            "SMART" -> CompletionType.SMART
            null -> null
            else -> error("Unknown completion type: $completionTypeString")
        }

    fun getAutocompleteSetting(fileText: String): Boolean? = InTextDirectivesUtils.getPrefixedBoolean(fileText, AUTOCOMPLETE_SETTING_PREFIX)

    fun isWithOrder(fileText: String): Boolean =
        InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, WITH_ORDER_PREFIX).isNotEmpty()

    fun assertDirectivesValid(fileText: String, additionalValidDirectives: Collection<String> = emptyList()) {
        InTextDirectivesUtils.assertHasUnknownPrefixes(fileText, KNOWN_PREFIXES + additionalValidDirectives)
    }

    fun assertContainsRenderedItems(
        expected: Array<CompletionProposal>,
        items: Array<LookupElement>,
        checkOrder: Boolean,
        nothingElse: Boolean
    ) {
        val itemsInformation = getItemsInformation(items)
        val allItemsString = listToString(itemsInformation)

        val leftItems = if (nothingElse) LinkedHashSet(itemsInformation) else null

        var indexOfPrevious = Integer.MIN_VALUE

        for (expectedProposal in expected) {
            var isFound = false

            for (index in itemsInformation.indices) {
                val proposal = itemsInformation[index]

                if (proposal.matches(expectedProposal)) {
                    isFound = true

                    Assert.assertTrue(
                        "Invalid order of existent elements in $allItemsString",
                        !checkOrder || index > indexOfPrevious
                    )
                    indexOfPrevious = index

                    leftItems?.remove(proposal)

                    break
                }
            }

            if (!isFound) {
                if (allItemsString.isEmpty()) {
                    Assert.fail("Completion is empty but $expectedProposal is expected")
                } else {
                    Assert.fail("Expected $expectedProposal not found in:\n$allItemsString")
                }
            }
        }

        if (leftItems != null && leftItems.isNotEmpty()) {
            Assert.fail("No items not mentioned in EXIST directives expected but some found:\n" + listToString(leftItems))
        }
    }

    private fun getPlatformExpectedNumber(fileText: String, platformNumberPrefix: String): Int? {
        val prefixedInt = InTextDirectivesUtils.getPrefixedInt(fileText, platformNumberPrefix)
        if (prefixedInt != null) {
            Assert.assertNull(
                "There shouldn't be $NUMBER_LINE_PREFIX and $platformNumberPrefix prefixes set in same time",
                InTextDirectivesUtils.getPrefixedInt(fileText, NUMBER_LINE_PREFIX)
            )
            return prefixedInt
        }

        return InTextDirectivesUtils.getPrefixedInt(fileText, NUMBER_LINE_PREFIX)
    }

    fun assertNotContainsRenderedItems(unexpected: Array<CompletionProposal>, items: Array<LookupElement>) {
        val itemsInformation = getItemsInformation(items)
        val allItemsString = listToString(itemsInformation)

        for (unexpectedProposal in unexpected) {
            for (proposal in itemsInformation) {
                Assert.assertFalse(
                    "Unexpected '$unexpectedProposal' presented in\n$allItemsString",
                    proposal.matches(unexpectedProposal)
                )
            }
        }
    }

    fun getItemsInformation(items: Array<LookupElement>): List<CompletionProposal> {
        val presentation = LookupElementPresentation()

        val result = ArrayList<CompletionProposal>(items.size)
        for (item in items) {
            item.renderElement(presentation)

            val map = HashMap<String, String?>()
            map[CompletionProposal.LOOKUP_STRING] = item.lookupString

            map[CompletionProposal.ALL_LOOKUP_STRINGS] = item.allLookupStrings.sorted().joinToString()

            if (presentation.itemText != null) {
                map[CompletionProposal.PRESENTATION_ITEM_TEXT] = presentation.itemText
                map[CompletionProposal.PRESENTATION_TEXT_ATTRIBUTES] = textAttributes(presentation)
            }

            if (presentation.typeText != null) {
                map[CompletionProposal.PRESENTATION_TYPE_TEXT] = presentation.typeText
            }

            if (presentation.tailText != null) {
                map[CompletionProposal.PRESENTATION_TAIL_TEXT] = presentation.tailText
            }
            item.moduleName?.let {
                map.put(CompletionProposal.MODULE_NAME, it)
            }

            result.add(CompletionProposal(map))
        }
        return result
    }

    private val LookupElement.moduleName: String?
        get() {
            return (`object` as? DeclarationLookupObject)?.psiElement?.module?.name
        }

    private fun textAttributes(presentation: LookupElementPresentation): String {
        return buildString {
            if (presentation.isItemTextBold) {
                append("bold")
            }
            if (presentation.isItemTextUnderlined) {
                if (length > 0) append(" ")
                append("underlined")
            }
            val foreground = presentation.itemTextForeground
            if (foreground != JBColor.foreground()) {
                assert(foreground == LookupElementFactory.CAST_REQUIRED_COLOR)
                if (length > 0) append(" ")
                append("grayed")
            }
            if (presentation.isStrikeout) {
                if (length > 0) append(" ")
                append("strikeout")
            }
        }
    }

    fun listToString(items: Collection<CompletionProposal>): String = items.joinToString("\n")
}
