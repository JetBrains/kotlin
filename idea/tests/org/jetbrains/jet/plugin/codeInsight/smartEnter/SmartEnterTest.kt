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

    fun testWhileMultiLine1() = doFunTest(
            """
            while (true)
                println()<caret>
            """,
            """
            while (true)
                println()
            <caret>
            """
    )

    fun testWhileMultiLine2() = doFunTest(
            """
            while (<caret>true)
                println()
            """,
            """
            while (true) {
                <caret>
            }
                println()
            """
    )

    fun testForStatement() = doFunTest(
            """
            for <caret>
            """
            ,
            """
            for (<caret>) {
            }
            """
    )

    fun testForStatement2() = doFunTest(
            """
            for<caret>
            """
            ,
            """
            for (<caret>) {
            }
            """
    )

    fun testForStatement4() = doFunTest(
            """
            for (i in 1..10<caret>) {
            }
            """
            ,
            """
            for (i in 1..10) {
                <caret>
            }
            """
    )

    fun testForStatement5() = doFunTest(
            """
            for (i in 1..10) {<caret>
            """
            ,
            """
            for (i in 1..10) {
                <caret>
            }
            """
    )

    fun testForStatement6() = doFunTest(
            """
            for (i in 1..10<caret>) {
                println()
            }
            """
            ,
            """
            for (i in 1..10) {
                <caret>
                println()
            }
            """
    )

    fun testForStatementSingle() = doFunTest(
            """
            for (i in 1..10<caret>) println()
            """
            ,
            """
            for (i in 1..10) println()
            <caret>
            """
    )

    fun testForStatementSingleEmpty() = doFunTest(
            """
            for (<caret>) println()
            """
            ,
            """
            for (<caret>) println()
            """
    )

    fun testForStatementOnLoopParameter() = doFunTest(
            """
            for (som<caret>e)
            println()
            """
            ,
            """
            for (some) {
                <caret>
            }
            println()
            """
    )

    fun testForMultiLine1() = doFunTest(
            """
            for (i in 1..10<caret>)
                println()
            """,
            """
            for (i in 1..10) {
                <caret>
            }
                println()
            """
    )

    fun testForMultiLine2() = doFunTest(
            """
            for (i in 1..10)
                println()<caret>
            """,
            """
            for (i in 1..10)
                println()
            <caret>
            """
    )

    fun testWhen() = doFunTest(
            """
            when <caret>
            """
            ,
            """
            when {
                <caret>
            }
            """
    )

    fun testWhen1() = doFunTest(
            """
            when<caret>
            """
            ,
            """
            when {
                <caret>
            }
            """
    )

    fun testWhen2() = doFunTest(
            """
            when (true<caret>) {
            }
            """
            ,
            """
            when (true) {
                <caret>
            }
            """
    )

    fun testWhen3() = doFunTest(
            """
            when (true) {<caret>
            """
            ,
            """
            when (true) {
                <caret>
            }
            """
    )

    fun testWhen4() = doFunTest(
            """
            when (true<caret>) {
                false -> println("false")
            }
            """
            ,
            """
            when (true) {
                <caret>
                false -> println("false")
            }
            """
    )

    fun testWhen5() = doFunTest(
            """
            when (<caret>)
            """
            ,
            """
            when (<caret>) {
            }
            """
    )

    fun testWhen6() = doFunTest(
            """
            when (true<caret>)
            """
            ,
            """
            when (true) {
                <caret>
            }
            """
    )

    // Check that no addition {} inserted
    fun testWhenBadParsed() = doFunTest(
            """
            when ( {<caret>
            }
            """
            ,
            """
            when ( {
                <caret>
            }
            """
    )

    fun testDoWhile() = doFunTest(
            """
            do <caret>
            """
            ,
            """
            do {
            } while (<caret>)${' '}
            """
    )

    fun testDoWhile2() = doFunTest(
            """
            do<caret>
            """
            ,
            """
            do {
            } while (<caret>)
            """
    )

    fun testDoWhile3() = doFunTest(
            """
            do<caret> {
                println(hi)
            }
            """
            ,
            """
            do {
                println(hi)
            } while (<caret>)
            """
    )

    fun testDoWhile5() = doFunTest(
            """
            do<caret> {
            } while ()
            """
            ,
            """
            do {
            } while (<caret>)
            """
    )

    fun testDoWhile6() = doFunTest(
            """
            do<caret> {
            } while (true)
            """
            ,
            """
            do {
                <caret>
            } while (true)
            """
    )

    fun testDoWhile7() = doFunTest(
            """
            do {
            } <caret>while (true)
            """
            ,
            """
            do {
            } while (true)
            <caret>
            """
    )

    fun testDoWhile8() = doFunTest(
            """
            do {
            } while (<caret>true)
            """
            ,
            """
            do {
                <caret>
            } while (true)
            """
    )

    fun testDoWhile9() = doFunTest(
            """
            do while<caret>
            """
            ,
            """
            do {
            } while (<caret>)
            """
    )

    fun testDoWhile10() = doFunTest(
            """
            do while (true<caret>)
            """
            ,
            """
            do {
                <caret>
            } while (true)
            """
    )

    fun testDoWhile11() = doFunTest(
            """
            do {
                println("some")
            } while<caret>
            """
            ,
            """
            do {
                println("some")
            } while (<caret>)
            """
    )

    fun testDoWhile12() = doFunTest(
            """
            do {
                println("some")
            } while (true<caret>
            """
            ,
            """
            do {
                <caret>
                println("some")
            } while (true
            """
    )

    fun testDoWhileOneLine1() = doFunTest(
            """
            do println("some") while (true<caret>)
            println("hi")
            """
            ,
            """
            do println("some") while (true)
            <caret>
            println("hi")
            """
    )

    fun testDoWhileOneLine2() = doFunTest(
            """
            do <caret>println("some") while (true)
            println("hi")
            """
            ,
            """
            do println("some") while (true)
            <caret>
            println("hi")
            """
    )

    fun testDoWhileMultiLine1() = doFunTest(
            """
            do
                println()<caret>
            while (true)
            """,
            """
            do
                println()
                <caret>
            while (true)
            """
    )

    fun testDoWhileMultiLine2() = doFunTest(
            """
            do<caret>
                println()
            while (true)
            """,
            """
            do {
                <caret>
                println()
            } while (true)
            """
    )

    fun testDoWhileMultiLine3() = doFunTest(
            """
            do
                println()
            while <caret>(true)
            """,
            """
            do {
                <caret>
                println()
            } while (true)
            """
    )

    fun testFunBody() = doFileTest(
            """
            fun test<caret>()
            """
            ,
            """
            fun test() {
                <caret>
            }
            """
    )

    fun testFunBody1() = doFileTest(
            """
            fun test<caret>
            """
            ,
            """
            fun test() {
                <caret>
            }
            """
    )

    fun testFunBody2() = doFileTest(
            """
            fun (p: Int, s: String<caret>
            """
            ,
            """
            fun (p: Int, s: String) {
                <caret>
            }
            """
    )

    fun testFunBody3() = doFileTest(
            """
            trait Some {
                fun (<caret>p: Int)
            }
            """
            ,
            """
            trait Some {
                fun (p: Int)
                <caret>
            }
            """
    )

    fun testFunBody4() = doFileTest(
            """
            class Some {
                abstract fun (<caret>p: Int)
            }
            """
            ,
            """
            class Some {
                abstract fun (p: Int)
                <caret>
            }
            """
    )

    fun testFunBody5() = doFileTest(
            """
            class Some {
                fun test(<caret>p: Int) = 1
            }
            """
            ,
            """
            class Some {
                fun test(p: Int) = 1
                <caret>
            }
            """
    )

    fun testFunBody6() = doFileTest(
            """
            fun test(<caret>p: Int) {
            }
            """
            ,
            """
            fun test(p: Int) {
                <caret>
            }
            """
    )

    fun testFunBody7() = doFileTest(
            """
            trait T
            fun <U> other() where U: T<caret>
            """,
            """
            trait T
            fun <U> other() where U : T {
                <caret>
            }
            """
    )

    fun testFunBody8() = doFileTest(
            """
            fun Int.other<caret>
            """,
            """
            fun Int.other() {
                <caret>
            }
            """
    )

    fun testInLambda1() = doFunTest(
            """
            some {
                p -><caret>
            }
            """,
            """
            some {
                p ->
                <caret>
            }
            """
    )

    fun testInLambda2() = doFunTest(
            """
            some {
                p<caret> ->
            }
            """,
            """
            some {
                p ->
                <caret>
            }
            """
    )

    fun testInLambda3() = doFunTest(
            """
            some {
                (<caret>p: Int) : Int ->
            }
            """,
            """
            some {
                (p: Int) : Int ->
                <caret>
            }
            """
    )

    fun testInLambda4() = doFunTest(
            """
            some {
                (p: <caret>Int) : Int ->
            }
            """,
            """
            some {
                (p: Int) : Int ->
                <caret>
            }
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

    fun doFileTest(before: String, after: String) {
        doTest(before.trimIndent().removeFirstEmptyLines(), after.trimIndent().removeFirstEmptyLines())
    }

    fun doTest(before: String, after: String) {
        myFixture.configureByText(JetFileType.INSTANCE, before)
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_COMPLETE_STATEMENT)
        myFixture.checkResult(after)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = LightCodeInsightFixtureTestCase.JAVA_LATEST

    private fun String.removeFirstEmptyLines() = this.split("\n").dropWhile { it.isEmpty() }.joinToString(separator = "\n")
}
