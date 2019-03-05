/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert

val RELATIVE_COMPLETION_TEST_DATA_BASE_PATH = "idea/idea-completion/testData"

val COMPLETION_TEST_DATA_BASE_PATH = KotlinTestUtils.getHomeDirectory() + "/" + RELATIVE_COMPLETION_TEST_DATA_BASE_PATH

fun testCompletion(
    fileText: String,
    platform: TargetPlatform?,
    complete: (CompletionType, Int) -> Array<LookupElement>?,
    defaultCompletionType: CompletionType = CompletionType.BASIC,
    defaultInvocationCount: Int = 0,
    additionalValidDirectives: Collection<String> = emptyList()
) {
    testWithAutoCompleteSetting(fileText) {
        val completionType = ExpectedCompletionUtils.getCompletionType(fileText) ?: defaultCompletionType
        val invocationCount = ExpectedCompletionUtils.getInvocationCount(fileText) ?: defaultInvocationCount
        val items = complete(completionType, invocationCount) ?: emptyArray()

        ExpectedCompletionUtils.assertDirectivesValid(fileText, additionalValidDirectives)

        val expected = ExpectedCompletionUtils.itemsShouldExist(fileText, platform)
        val unexpected = ExpectedCompletionUtils.itemsShouldAbsent(fileText, platform)
        val itemsNumber = ExpectedCompletionUtils.getExpectedNumber(fileText, platform)
        val nothingElse = ExpectedCompletionUtils.isNothingElseExpected(fileText)

        Assert.assertTrue("Should be some assertions about completion", expected.size != 0 || unexpected.size != 0 || itemsNumber != null || nothingElse)
        ExpectedCompletionUtils.assertContainsRenderedItems(expected, items, ExpectedCompletionUtils.isWithOrder(fileText), nothingElse)
        ExpectedCompletionUtils.assertNotContainsRenderedItems(unexpected, items)

        if (itemsNumber != null) {
            val expectedItems = ExpectedCompletionUtils.listToString(ExpectedCompletionUtils.getItemsInformation(items))
            Assert.assertEquals("Invalid number of completion items: ${expectedItems}", itemsNumber, items.size)
        }
    }
}

private fun testWithAutoCompleteSetting(fileText: String, doTest: () -> Unit) {
    val autoComplete = ExpectedCompletionUtils.getAutocompleteSetting(fileText) ?: false

    val settings = CodeInsightSettings.getInstance()
    val oldValue1 = settings.AUTOCOMPLETE_ON_CODE_COMPLETION
    val oldValue2 = settings.AUTOCOMPLETE_ON_SMART_TYPE_COMPLETION
    try {
        settings.AUTOCOMPLETE_ON_CODE_COMPLETION = autoComplete
        settings.AUTOCOMPLETE_ON_SMART_TYPE_COMPLETION = autoComplete
        doTest()
    }
    finally {
        settings.AUTOCOMPLETE_ON_CODE_COMPLETION = oldValue1
        settings.AUTOCOMPLETE_ON_SMART_TYPE_COMPLETION = oldValue2
    }
}
