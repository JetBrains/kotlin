/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.completion.handlers

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.application.Result
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import org.jetbrains.kotlin.idea.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.test.JetTestUtils
import org.junit.Assert

public abstract class CompletionHandlerTestBase() : JetLightCodeInsightFixtureTestCase() {
    protected val fixture: JavaCodeInsightTestFixture
        get() = myFixture

    protected fun doTestWithTextLoaded(
            completionType: CompletionType,
            time: Int,
            lookupString: String?,
            itemText: String?,
            tailText: String?,
            completionChar: Char,
            afterFilePath: String
    ) {
        fixture.complete(completionType, time)

        if (lookupString != null || itemText != null || tailText != null) {
            val item = getExistentLookupElement(lookupString, itemText, tailText)
            if (item != null) {
                selectItem(item, completionChar)
            }
        }

        fixture.checkResultByFile(afterFilePath)
    }

    private fun getExistentLookupElement(lookupString: String?, itemText: String?, tailText: String?): LookupElement? {
        val lookup = LookupManager.getInstance(getProject())?.getActiveLookup() as LookupImpl?
        if (lookup == null) return null
        val items = lookup.getItems()

        if (lookupString == "*") {
            assert(itemText == null)
            assert(tailText == null)
            return items.firstOrNull()
        }

        var foundElement : LookupElement? = null
        val presentation = LookupElementPresentation()
        for (lookupElement in items) {
            val lookupOk = if (lookupString != null) lookupElement.getLookupString().contains(lookupString) else true

            if (lookupOk) {
                lookupElement.renderElement(presentation)

                val textOk = if (itemText != null) {
                    val itemItemText = presentation.getItemText()
                    itemItemText != null && itemItemText.contains(itemText)
                }
                else {
                    true
                }

                if (textOk) {
                    val tailOk = if (tailText != null) {
                        val itemTailText = presentation.getTailText()
                        itemTailText != null && itemTailText.contains(tailText)
                    }
                    else {
                        true
                    }

                    if (tailOk) {
                        if (foundElement != null) {
                            Assert.fail("Several elements satisfy to completion restrictions: \n    $foundElement\n    $lookupElement")
                        }

                        foundElement = lookupElement
                    }
                }
            }
        }

        if (foundElement == null) error("No element satisfy completion restrictions")
        return foundElement
    }

    override fun getTestDataPath() = JetTestUtils.getHomeDirectory()

    protected fun selectItem(item: LookupElement?, completionChar: Char) {
        val lookup = (fixture.getLookup() as LookupImpl)
        if (lookup.getCurrentItem() != item) { // do not touch selection if not changed - important for char filter tests
            lookup.setCurrentItem(item)
        }
        lookup.setFocusDegree(LookupImpl.FocusDegree.FOCUSED);
        if (LookupEvent.isSpecialCompletionChar(completionChar)) {
            (object : WriteCommandAction.Simple<Any>(getProject()) {
                protected override fun run(result: Result<Any>) {
                    run()
                }
                protected override fun run() {
                    lookup.finishLookup(completionChar)
                }
            }).execute().throwException()
        }
        else {
            fixture.type(completionChar)
        }
    }
}
