/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.liveTemplates

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.codeInsight.template.impl.TemplateState
import com.intellij.ide.DataManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.util.ArrayUtil
import com.intellij.util.ui.UIUtil
import junit.framework.TestCase
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith
import java.io.File
import java.util.*

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class LiveTemplatesTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.testDataPath = File(TEST_DATA_BASE_PATH).path + File.separator
        setTemplateTestingCompat(module.project, testRootDisposable)
    }

    fun testSout() {
        parameterless()
    }

    fun testSout_BeforeCall() {
        parameterless()
    }

    fun testSout_BeforeCallSpace() {
        parameterless()
    }

    fun testSout_BeforeBinary() {
        parameterless()
    }

    fun testSout_InCallArguments() {
        parameterless()
    }

    fun testSout_BeforeQualifiedCall() {
        parameterless()
    }

    fun testSout_AfterSemicolon() {
        parameterless()
    }

    fun testSoutf() {
        parameterless()
    }

    fun testSoutf_InCompanion() {
        parameterless()
    }

    fun testSerr() {
        parameterless()
    }

    fun testMain() {
        parameterless()
    }

    fun testMaina() {
        parameterless()
    }

    fun testSoutv() {
        start()

        assertStringItems("DEFAULT_BUFFER_SIZE", "args", "x", "y")
        typeAndNextTab("y.plus(\"test\")")

        checkAfter()
    }

    fun testSoutp() {
        parameterless()
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

        assertStringItems("args", "myList", "o", "str")
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

    private fun parameterless() {
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
                null
            )
        })
    }

    private fun nextTab(times: Int) {
        for (i in 0 until times) {
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
        actionHandler.execute(
            myFixture.editor, myFixture.editor.caretModel.currentCaret,
            DataManager.getInstance().getDataContext(myFixture.editor.component)
        )
    }

    private fun assertStringItems(@NonNls vararg items: String) {
        TestCase.assertEquals(listOf(*items), listOf(*itemStringsSorted))
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
