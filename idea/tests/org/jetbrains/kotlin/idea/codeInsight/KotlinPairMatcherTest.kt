/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinPairMatcher
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import java.lang.IllegalArgumentException

class KotlinPairMatcherTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): KotlinLightProjectDescriptor = KotlinLightProjectDescriptor.INSTANCE

    private fun doTest(testData: String) {
        var docText = testData
        val startPos = docText.indexOf("<start>")
        if (startPos == -1) {
            throw IllegalArgumentException("<start> marker not found in testdata")
        }
        docText = docText.replace("<start>", "")
        val bracePos = docText.indexOf("<brace>")
        if (bracePos == -1) {
            throw IllegalArgumentException("<brace> marker not found in testdata")
        }
        docText = docText.replace("<brace>", "")
        myFixture.configureByText(KotlinFileType.INSTANCE, docText)
        val pos = KotlinPairMatcher().getCodeConstructStart(myFixture.file, bracePos)
        assertEquals(startPos, pos)
    }

    fun testClass() {
        doTest("/* Doc comment */ <start>class Foo : Bar, Baz <brace>{ fun xyzzy() { } }")
    }

    fun testFun() {
        doTest("/* Doc comment */ <start>fun xyzzy(x: Int, y: String): Any <brace>{ }")
    }

    fun testFor() {
        doTest("fun xyzzy() { for (x in 0..1)<start><brace>{ } }")
    }
}
