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

public class CompletionHandlerTest() : JetLightCodeInsightFixtureTestCase() {
    fun testClassCompletionImport() = doTest(CompletionType.BASIC, 2, "SortedSet", null, '\n')

    fun testDoNotInsertImportForAlreadyImported() = doTest()

    fun testDoNotInsertDefaultJsImports() = doTest()

    fun testDoNotInsertImportIfResolvedIntoJavaConstructor() = doTest()

    fun testNonStandardArray() = doTest(CompletionType.BASIC, 2, "Array", "java.lang.reflect", '\n')

    fun testNoParamsFunction() = doTest()

    fun testParamsFunction() = doTest()

    fun testInsertJavaClassImport() = doTest()

    fun testInsertVoidJavaMethod() = doTest()

    fun testPropertiesSetter() = doTest()

    fun testExistingSingleBrackets() = doTest()

    fun testExtFunction() = doTest()

    fun testFunctionLiteralInsertOnSpace() = doTest(CompletionType.BASIC, 2, null, null, ' ')

    fun testInsertImportOnTab() = doTest(CompletionType.BASIC, 2, "ArrayList", null, '\t')

    fun testHigherOrderFunction() = doTest()

    fun testInsertFqnForJavaClass() = doTest(CompletionType.BASIC, 2, "SortedSet", "java.util", '\n')

    fun testHigherOrderFunctionWithArg() = doTest(CompletionType.BASIC, 2, "filterNot", null, '\n')

    fun testForceParenthesisForTabChar() = doTest(CompletionType.BASIC, 0, "some", null, '\t')

    var fixture by Delegates.notNull<JavaCodeInsightTestFixture>()

    protected override fun setUp() {
        super.setUp()
        fixture = myFixture!!
    }

    fun testSingleBrackets() {
        fixture.configureByFile(fileName())
        fixture.`type`('(')
        fixture.checkResultByFile(afterFileName())
    }

    fun testInsertFunctionWithBothParentheses() {
        fixture.configureByFile(fileName())
        fixture.`type`("test()")
        fixture.checkResultByFile(afterFileName())
    }

    fun testFunctionLiteralInsertWhenNoSpacesForBraces() {
        val settings = CodeStyleSettingsManager.getSettings(getProject())
        val jetSettings = settings.getCustomSettings(javaClass<JetCodeStyleSettings>())!!

        try {
            jetSettings.INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD = false
            doTest()
        }
        finally {
            jetSettings.INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD = true
        }
    }

    fun doTest() = doTest(CompletionType.BASIC, 2, null, null, '\n')

    private fun doTest(`type` : CompletionType, time : Int, lookupString : String?, tailText : String?, completionChar : Char) : Unit {
        fixture.configureByFile(fileName())
        if (lookupString != null || tailText != null) {
            fixture.complete(`type`, time)
            val item = getExistentLookupElement(lookupString, tailText)
            if (item != null) {
                selectItem(item, completionChar)
            }
        }
        else {
            forceCompleteFirst(`type`, time)
        }
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

    fun afterFileName() = getTestName(false) + ".kt.after"

    fun forceCompleteFirst(`type` : CompletionType, time : Int) {
        fixture.complete(`type`, time)
        val items : Array<LookupElement>? = fixture.getLookupElements()
        if (items != null && items.isNotEmpty()) {
            selectItem(items[0])
        }
    }

    protected override fun getTestDataPath() : String = File(PluginTestCaseBase.getTestDataPathBase(), "/completion/handlers/").getPath() + File.separator

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
            }).execute().throwException()
        }
        else {
            fixture.`type`(completionChar)
        }
    }
}
