/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.codeInsight.smartEnter

import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase
import com.intellij.openapi.actionSystem.IdeActions
import org.jetbrains.jet.plugin.JetFileType
import org.jetbrains.jet.test.util.trimIndent
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase

class SmartEnterTest : JetLightCodeInsightFixtureTestCase() {
    fun testIfCondition() = doFunTest(
            """
            if <caret>
            """
            ,
            """
            if (<caret>) {
            }
            """
    )

    fun testIfCondition2() = doFunTest(
            """
            if<caret>
            """
            ,
            """
            if (<caret>) {
            }
            """
    )

    fun testIfWithFollowingCode() = doFunTest(
            """
            if<caret>

            return true
            """
            ,
            """
            if (<caret>) {
            }

            return true
            """
    )

    fun testIfCondition3() = doFunTest(
            """
            if (<caret>
            """
            ,
            """
            if (<caret>) {
            }
            """
    )

    fun testIfCondition4() = doFunTest(
            """
            if (true<caret>) {
            }
            """
            ,
            """
            if (true) {
                <caret>
            }
            """
    )

    fun testIfCondition5() = doFunTest(
            """
            if (true) {<caret>
            """
            ,
            """
            if (true) {
                <caret>
            }
            """
    )

    fun testIfCondition6() = doFunTest(
            """
            if (true<caret>) {
                println()
            }
            """
            ,
            """
            if (true) {
                <caret>
                println()
            }
            """
    )

    fun testIfThenOneLine1() = doFunTest(
            """
            if (true) println()<caret>
            """
            ,
            """
            if (true) println()
            <caret>
            """
    )

    fun testIfThenOneLine2() = doFunTest(
            """
            if (true) <caret>println()
            """
            ,
            """
            if (true) println()
            <caret>
            """
    )

    fun testIfThenMultiLine1() = doFunTest(
            """
            if (true)
                println()<caret>
            """
            ,
            """
            if (true)
                println()
            <caret>
            """
    )

    fun testIfThenMultiLine2() = doFunTest(
            """
            if (true)
                println()<caret>
            """
            ,
            """
            if (true)
                println()
            <caret>
            """
    )

    // TODO: indent for println
    fun testIfThenMultiLine3() = doFunTest(
            """
            if (true<caret>)
                println()
            """
            ,
            """
            if (true) {
                <caret>
            }
                println()
            """
    )

    fun testIfWithReformat() = doFunTest(
            """
            if     (true<caret>) {
            }
            """,
            """
            if (true) {
                <caret>
            }
            """
    )

    fun testElse() = doFunTest(
            """
            if (true) {
            } else<caret>
            """,
            """
            if (true) {
            } else {
                <caret>
            }
            """
    )

    fun testElseOneLine1() = doFunTest(
            """
            if (true) {
            } else println()<caret>
            """,
            """
            if (true) {
            } else println()
            <caret>
            """
    )

    fun testElseOneLine2() = doFunTest(
            """
            if (true) {
            } else <caret>println()
            """,
            """
            if (true) {
            } else println()
            <caret>
            """
    )

    fun testElseTwoLines1() = doFunTest(
            """
            if (true) {
            } else
                <caret>println()
            """,
            """
            if (true) {
            } else
                println()
            <caret>
            """
    )

    fun testElseTwoLines2() = doFunTest(
            """
            if (true) {
            } else
                println()<caret>
            """,
            """
            if (true) {
            } else
                println()
            <caret>
            """
    )

    // TODO: remove space in expected data
    fun testElseWithSpace() = doFunTest(
            """
            if (true) {
            } else <caret>
            """,
            """
            if (true) {
            } else {
                <caret>
            }${' '}
            """
    )


    fun testWhile() = doFunTest(
            """
            while <caret>
            """
            ,
            """
            while (<caret>) {
            }
            """
    )

    fun testWhile2() = doFunTest(
            """
            while<caret>
            """
            ,
            """
            while (<caret>) {
            }
            """
    )

    fun testWhile3() = doFunTest(
            """
            while (<caret>
            """
            ,
            """
            while (<caret>) {
            }
            """
    )

    fun testWhile4() = doFunTest(
            """
            while (true<caret>) {
            }
            """
            ,
            """
            while (true) {
                <caret>
            }
            """
    )

    fun testWhile5() = doFunTest(
            """
            while (true) {<caret>
            """
            ,
            """
            while (true) {
                <caret>
            }
            """
    )

    fun testWhile6() = doFunTest(
            """
            while (true<caret>) {
                println()
            }
            """
            ,
            """
            while (true) {
                <caret>
                println()
            }
            """
    )

    fun testWhile7() = doFunTest(
            """
            while ()<caret>
            """,
            """
            while (<caret>) {
            }
            """
    )

    fun testWhileSingle() = doFunTest(
            """
            <caret>while    (true) println()
            """,
            """
            while (true) println()
            <caret>
            """
    )

    fun doFunTest(before: String, after: String) {
        fun String.withFunContext(): String {
            val bodyText = "//----${this.trimIndent()}//----"
            val withIndent = bodyText.split("\n").map { "    $it" }.joinToString(separator = "\n")

            return "fun method() {\n${withIndent}\n}"
        }

        doTest(before.withFunContext(), after.withFunContext())
    }

    fun doTest(before: String, after: String) {
        myFixture.configureByText(JetFileType.INSTANCE, before)
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_COMPLETE_STATEMENT)
        myFixture.checkResult(after)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = LightCodeInsightFixtureTestCase.JAVA_LATEST
}
