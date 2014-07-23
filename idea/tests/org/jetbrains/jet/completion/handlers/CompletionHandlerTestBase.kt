/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.completion.handlers

import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElement
import java.io.File
import org.jetbrains.jet.plugin.PluginTestCaseBase
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.junit.Assert
import com.intellij.openapi.application.Result
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture

public abstract class CompletionHandlerTestBase() : JetLightCodeInsightFixtureTestCase() {
    protected abstract val completionType : CompletionType
    protected abstract val testDataRelativePath: String

    protected val fixture: JavaCodeInsightTestFixture
        get() = myFixture

    protected fun doTest() : Unit = doTest(2, "*", null, null, '\n')

    protected fun doTest(time: Int, lookupString: String?, tailText: String?, completionChar: Char) {
        doTest(time, lookupString, null, tailText, completionChar)
    }

    protected fun doTest(time: Int, lookupString: String?, itemText: String?, tailText: String?, completionChar: Char) {
        fixture.configureByFile(fileName())
        doTestWithTextLoaded(time, lookupString, itemText, tailText, completionChar)
    }

    protected fun doTestWithTextLoaded(time: Int, lookupString: String?, itemText: String?, tailText: String?, completionChar: Char) {
        fixture.complete(completionType, time)

        if (lookupString != null || itemText != null || tailText != null) {
            val item = getExistentLookupElement(lookupString, itemText, tailText)
            if (item != null) {
                selectItem(item, completionChar)
            }
        }

        checkResult()
    }

    protected fun checkResult(){
        fixture.checkResultByFile(afterFileName())
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
                            Assert.fail("Several elements satisfy to completion restrictions")
                        }

                        foundElement = lookupElement
                    }
                }
            }
        }

        if (foundElement == null) error("No element satisfy completion restrictions")
        return foundElement
    }

    protected fun afterFileName(): String = getTestName(false) + ".kt.after"

    protected override fun getTestDataPath() : String = File(PluginTestCaseBase.getTestDataPathBase(), testDataRelativePath).getPath() + File.separator

    protected fun selectItem(item: LookupElement?) {
        selectItem(item, 0.toChar())
    }

    protected fun selectItem(item: LookupElement?, completionChar: Char) {
        val lookup = (fixture.getLookup() as LookupImpl)
        lookup.setCurrentItem(item)
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
            fixture.`type`(completionChar)
        }
    }
}
