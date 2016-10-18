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

package org.jetbrains.kotlin.idea.liveTemplates

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.codeInsight.template.impl.TemplateState
import com.intellij.ide.DataManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.util.ArrayUtil
import com.intellij.util.ui.UIUtil
import junit.framework.TestCase
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import java.io.File
import java.util.*

class LiveTemplatesTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.testDataPath = File(TEST_DATA_BASE_PATH).path + File.separator
        (TemplateManager.getInstance(project) as TemplateManagerImpl).setTemplateTesting(true)
    }

    override fun tearDown() {
        (TemplateManager.getInstance(project) as TemplateManagerImpl).setTemplateTesting(false)
        super.tearDown()
    }

    fun testSout() {
        paremeterless()
    }

    fun testSout_BeforeCall() {
        paremeterless()
    }

    fun testSout_BeforeCallSpace() {
        paremeterless()
    }

    fun testSout_BeforeBinary() {
        paremeterless()
    }

    fun testSout_InCallArguments() {
        paremeterless()
    }

    fun testSout_BeforeQualifiedCall() {
        paremeterless()
    }

    fun testSout_AfterSemicolon() {
        paremeterless()
    }

    fun testSerr() {
        paremeterless()
    }

    fun testMain() {
        paremeterless()
    }

    fun testSoutv() {
        start()

        assertStringItems("ASSERTIONS_ENABLED", "args", "defaultBlockSize", "defaultBufferSize", "minimumBlockSize", "x", "y")
        typeAndNextTab("y")

        checkAfter()
    }

    fun testSoutp() {
        paremeterless()
    }

    fun testFun0() {
        start()

        type("foo")
        nextTab(2)

        checkAfter()
    }

    fun testFun1() {
        start()

        type("foo")
        nextTab(4)

        checkAfter()
    }

    fun testFun2() {
        start()

        type("foo")
        nextTab(6)

        checkAfter()
    }

    fun testExfun() {
        start()

        typeAndNextTab("Int")
        typeAndNextTab("foo")
        typeAndNextTab("arg : Int")
        nextTab()

        checkAfter()
    }

    fun testExval() {
        start()

        typeAndNextTab("Int")
        nextTab()
        typeAndNextTab("Int")

        checkAfter()
    }

    fun testExvar() {
        start()

        typeAndNextTab("Int")
        nextTab()
        typeAndNextTab("Int")

        checkAfter()
    }

    fun testClosure() {
        start()

        typeAndNextTab("param")
        nextTab()

        checkAfter()
    }

    fun testInterface() {
        start()

        typeAndNextTab("SomeTrait")

        checkAfter()
    }

    fun testSingleton() {
        start()

        typeAndNextTab("MySingleton")

        checkAfter()
    }

    fun testVoid() {
        start()

        typeAndNextTab("foo")
        typeAndNextTab("x : Int")

        checkAfter()
    }

    fun testIter() {
        start()

        assertStringItems("args", "myList", "o", "str", "stream")
        type("args")
        nextTab(2)

        checkAfter()
    }

    fun testAnonymous_1() {
        start()

        typeAndNextTab("Runnable")

        checkAfter()
    }

    fun testAnonymous_2() {
        start()

        typeAndNextTab("Thread")

        checkAfter()
    }

    fun testObject_ForClass() {
        start()

        typeAndNextTab("A")

        checkAfter()
    }

    private fun doTestIfnInn() {
        start()

        assertStringItems("b", "t", "y")
        typeAndNextTab("b")

        checkAfter()
    }

    fun testIfn() {
        doTestIfnInn()
    }

    fun testInn() {
        doTestIfnInn()
    }

    private fun paremeterless() {
        start()

        checkAfter()
    }

    private fun start() {
        myFixture.configureByFile(getTestName(true) + ".kt")
        myFixture.type(templateName)

        doAction("ExpandLiveTemplateByTab")
    }

    private val templateName: String
        get() {
            val testName = getTestName(true)
            if (testName.contains("_")) {
                return testName.substring(0, testName.indexOf("_"))
            }
            return testName
        }

    private fun checkAfter() {
        TestCase.assertNull(templateState)
        myFixture.checkResultByFile(getTestName(true) + ".exp.kt", true)
    }

    private fun typeAndNextTab(s: String) {
        type(s)
        nextTab()
    }

    private fun type(s: String) {
        myFixture.type(s)
    }

    private fun nextTab() {
        val project = project
        UIUtil.invokeAndWaitIfNeeded(Runnable {
            CommandProcessor.getInstance().executeCommand(
                    project,
                    {
                        templateState!!.nextTab()
                    },
                    "nextTab",
                    null)
        })
    }

    private fun nextTab(times: Int) {
        for (i in 0..times - 1) {
            nextTab()
        }
    }

    private val templateState: TemplateState?
        get() = TemplateManagerImpl.getTemplateState(myFixture.editor)

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
    }

    private fun doAction(actionId: String) {
        val actionManager = EditorActionManager.getInstance()
        val actionHandler = actionManager.getActionHandler(actionId)
        actionHandler.execute(myFixture.editor, DataManager.getInstance().getDataContext(myFixture.editor.component))
    }

    private fun assertStringItems(@NonNls vararg items: String) {
        TestCase.assertEquals(Arrays.asList(*items), Arrays.asList(*itemStringsSorted))
    }

    private val itemStrings: Array<String>
        get() {
            val lookup = LookupManager.getActiveLookup(myFixture.editor)!!
            val result = ArrayList<String>()
            for (element in lookup.items) {
                result.add(element.lookupString)
            }
            return ArrayUtil.toStringArray(result)
        }

    private val itemStringsSorted: Array<String>
        get() {
            val items = itemStrings
            Arrays.sort(items)
            return items
        }
}
