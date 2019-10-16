/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test

import org.jetbrains.kotlin.idea.debugger.KotlinEditorTextProvider
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.invalidateLibraryCache
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.junit.Assert

abstract class AbstractSelectExpressionForDebuggerTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        invalidateLibraryCache(project)
    }

    fun doTest(path: String) {
        doTest(path, true)
    }

    fun doTestWoMethodCalls(path: String) {
        doTest(path, false)
    }

    fun doTest(unused: String, allowMethodCalls: Boolean) {
        myFixture.configureByFile(fileName())

        val elementAt = myFixture.file?.findElementAt(myFixture.caretOffset)!!
        val selectedExpression = KotlinEditorTextProvider.findExpressionInner(elementAt, allowMethodCalls)

        val expected = InTextDirectivesUtils.findStringWithPrefixes(myFixture.file?.text!!, "// EXPECTED: ")

        val actualResult = if (selectedExpression != null) {
            KotlinEditorTextProvider.getElementInfo(selectedExpression) { it.text }
        } else {
            "null"
        }

        Assert.assertEquals("Another expression should be selected", expected, actualResult)
    }

    override fun getProjectDescriptor() = KotlinLightProjectDescriptor.INSTANCE
}
