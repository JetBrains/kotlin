/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test.handlers

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import org.jetbrains.kotlin.idea.completion.test.ExpectedCompletionUtils
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase

abstract class CompletionHandlerTestBase : KotlinLightCodeInsightFixtureTestCase() {
    protected val fixture: JavaCodeInsightTestFixture
        get() = myFixture

    protected fun doTestWithTextLoaded(
        completionType: CompletionType,
        time: Int,
        lookupString: String?,
        itemText: String?,
        tailText: String?,
        completionChars: String,
        afterFilePath: String
    ) {
        for (idx in 0 until completionChars.length - 1) {
            fixture.type(completionChars[idx])
        }

        fixture.complete(completionType, time)

        if (lookupString != null || itemText != null || tailText != null) {
            val item = getExistentLookupElement(lookupString, itemText, tailText)
            if (item != null) {
                selectItem(item, completionChars.last())
            }
        }
        fixture.checkResultByFile(afterFilePath)
    }

    protected fun completionChars(char: String?, chars: String?): String =
        when (char) {
            null -> when (chars) {
                null -> "\n"
                else -> chars.replace("\\n", "\n").replace("\\t", "\t")
            }
            "\\n" -> "\n"
            "\\t" -> "\t"
            else -> char.single().toString() ?: error("Incorrect completion char: \"$char\"")
        }

    protected fun getExistentLookupElement(lookupString: String?, itemText: String?, tailText: String?): LookupElement? {
        val lookup = LookupManager.getInstance(project)?.activeLookup as LookupImpl? ?: return null
        val items = lookup.items

        if (lookupString == "*") {
            assert(itemText == null)
            assert(tailText == null)
            return items.firstOrNull()
        }

        var foundElement : LookupElement? = null
        val presentation = LookupElementPresentation()
        for (lookupElement in items) {
            val lookupOk = if (lookupString != null) lookupElement.lookupString == lookupString else true

            if (lookupOk) {
                lookupElement.renderElement(presentation)

                val textOk = if (itemText != null) {
                    val itemItemText = presentation.itemText
                    itemItemText != null && itemItemText == itemText
                }
                else {
                    true
                }

                if (textOk) {
                    val tailOk = if (tailText != null) {
                        val itemTailText = presentation.tailText
                        itemTailText != null && itemTailText == tailText
                    }
                    else {
                        true
                    }

                    if (tailOk) {
                        if (foundElement != null) {
                            val dump = ExpectedCompletionUtils.listToString(ExpectedCompletionUtils.getItemsInformation(arrayOf(foundElement, lookupElement)))
                            fail("Several elements satisfy to completion restrictions:\n$dump")
                        }

                        foundElement = lookupElement
                    }
                }
            }
        }

        if (foundElement == null) {
            val dump = ExpectedCompletionUtils.listToString(ExpectedCompletionUtils.getItemsInformation(items.toTypedArray()))
            error("No element satisfy completion restrictions in:\n$dump")
        }
        return foundElement
    }

    protected fun selectItem(item: LookupElement?, completionChar: Char) {
        val lookup = (fixture.lookup as LookupImpl)
        if (lookup.currentItem != item) { // do not touch selection if not changed - important for char filter tests
            lookup.currentItem = item
        }
        lookup.focusDegree = LookupImpl.FocusDegree.FOCUSED
        if (LookupEvent.isSpecialCompletionChar(completionChar)) {
            lookup.finishLookup(completionChar)
        }
        else {
            fixture.type(completionChar)
        }
    }
}
