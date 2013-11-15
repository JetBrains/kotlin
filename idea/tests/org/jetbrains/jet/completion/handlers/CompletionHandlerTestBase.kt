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
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.jetbrains.jet.plugin.formatter.JetCodeStyleSettings
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
import kotlin.properties.Delegates
import com.intellij.openapi.util.io.FileUtil

public abstract class CompletionHandlerTestBase() : JetLightCodeInsightFixtureTestCase() {
    protected abstract val completionType : CompletionType
    protected abstract val testDataRelativePath: String

    protected var fixture: JavaCodeInsightTestFixture by Delegates.notNull<JavaCodeInsightTestFixture>()

    protected override fun setUp() {
        super.setUp()
        fixture = myFixture!!
    }

    protected fun doTest() : Unit = doTest(2, null, null, '\n')

    protected fun doTest(time : Int, lookupString : String?, tailText : String?, completionChar : Char) : Unit {
        fixture.configureByFile(fileName())
        if (lookupString != null || tailText != null) {
            fixture.complete(completionType, time)
            val item = getExistentLookupElement(lookupString, tailText)
            if (item != null) {
                selectItem(item, completionChar)
            }
        }
        else {
            forceCompleteFirst(completionType, time)
        }
        checkResult()
    }

    protected fun checkResult(){
        fixture.checkResultByFile(afterFileName())
    }

    private fun getExistentLookupElement(lookupString : String?, tailText : String?) : LookupElement? {
        val lookup = LookupManager.getInstance(getProject())?.getActiveLookup() as LookupImpl?

        var foundElement : LookupElement? = null
        if (lookup != null) {
            val presentation = LookupElementPresentation()
            for (lookupElement in lookup.getItems()!!) {

                val lookupOk : Boolean
                if (lookupString != null) {
                    lookupOk = lookupElement.getLookupString().contains(lookupString)
                }
                else {
                    lookupOk = true
                }

                val tailOk : Boolean
                if (tailText != null) {
                    lookupElement.renderElement(presentation)
                    val itemTailText : String? = presentation.getTailText()
                    tailOk = itemTailText != null && (itemTailText.contains(tailText))
                }
                else {
                    tailOk = true
                }

                if (lookupOk && tailOk) {
                    if (foundElement != null) {
                        Assert.fail("Several elements satisfy to completion restrictions")
                    }

                    foundElement = lookupElement
                }
            }
        }

        return foundElement
    }

    protected fun afterFileName(): String = getTestName(false) + ".kt.after"

    private fun forceCompleteFirst(`type` : CompletionType, time : Int) {
        fixture.complete(`type`, time)
        val items : Array<LookupElement>? = fixture.getLookupElements()
        if (items != null && items.isNotEmpty()) {
            selectItem(items[0])
        }
    }

    protected override fun getTestDataPath() : String = File(PluginTestCaseBase.getTestDataPathBase(), testDataRelativePath).getPath() + File.separator

    protected fun selectItem(item : LookupElement?) {
        selectItem(item, 0.toChar())
    }

    protected fun selectItem(item : LookupElement?, completionChar : Char) {
        val lookup = (fixture.getLookup() as LookupImpl)
        lookup.setCurrentItem(item)
        if (LookupEvent.isSpecialCompletionChar(completionChar)) {
            (object : WriteCommandAction.Simple(getProject()) {
                protected override fun run(result: Result<Any?>?) {
                    run()
                }
                protected override fun run() {
                    lookup.finishLookup(completionChar)
                }
            }).execute()!!.throwException()
        }
        else {
            fixture.`type`(completionChar)
        }
    }
}
