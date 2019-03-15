/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.smartEnter

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase

class SmartEnterTest : KotlinLightCodeInsightFixtureTestCase() {
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
            when ({<caret>
            }
            """
            ,
            """
            when ({
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
                <caret>
            } while (true)
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

    fun testDoWhile13() = doFunTest(
            """
            do<caret>
            println("some")
            """
            ,
            """
            do {
                println("some")
            } while (<caret>)
            """
    )

    fun testDoWhile14() = doFunTest(
            """
            do <caret>
            println("some")
            """
            ,
            """
            do {
                println("some")
            } while (<caret>)
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
            fun(p: Int, s: String) {
                <caret>
            }
            """
    )

    fun testFunBody3() = doFileTest(
            """
            interface Some {
                fun (<caret>p: Int)
            }
            """
            ,
            """
            interface Some {
                fun(p: Int)
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
                abstract fun(p: Int)
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

    fun testFunBody9() = doFileTest(
            """
            fun test(){<caret>}
            """,
            """
            fun test() {}
            <caret>
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
            some { p<caret> ->
            }
            """,
            """
            some { p ->
                <caret>
            }
            """
    )

    fun testInLambda3() = doFunTest(
            """
            some { (<caret>p: Int) : Int ->
            }
            """,
            """
            some { (p: Int) : Int ->
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

    fun testSetter1() = doFileTest(
            """
            var a : Int = 0
                set<caret>
            """
            ,
            """
            var a : Int = 0
                set
            <caret>
            """
    )

    fun testSetter2() = doFileTest(
            """
            var a : Int = 0
                set(<caret>
            """
            ,
            """
            var a : Int = 0
                set(value) {
                    <caret>
                }
            """
    )

    fun testSetter3() = doFileTest(
            """
            var a : Int = 0
                set(<caret>)
            """
            ,
            """
            var a : Int = 0
                set(value) {
                    <caret>
                }
            """
    )

    fun testSetter4() = doFileTest(
            """
            var a : Int = 0
                set(v<caret>)
            """
            ,
            """
            var a : Int = 0
                set(v) {
                    <caret>
                }
            """
    )

    fun testSetter5() = doFileTest(
            """
            var a : Int = 0
                set(<caret>) {
                }
            """
            ,
            """
            var a : Int = 0
                set(value) {
                    <caret>
                }
            """
    )

    fun testSetter6() = doFileTest(
            """
            var a : Int = 0
                set(v<caret>) {
                }
            """
            ,
            """
            var a : Int = 0
                set(v) {
                    <caret>
                }
            """
    )

    fun testSetter7() = doFileTest(
            """
            var a : Int = 0
                set(value){<caret>}
            """
            ,
            """
            var a : Int = 0
                set(value) {}
            <caret>
            """
    )

    fun testSetterPrivate1() = doFileTest(
            """
            var a : Int = 0
                private set<caret>
            """
            ,
            """
            var a : Int = 0
                private set
            <caret>
            """
    )

    fun testSetterPrivate2() = doFileTest(
            """
            var a : Int = 0
                private set(<caret>
            """
            ,
            """
            var a : Int = 0
                private set(value) {
                    <caret>
                }
            """
    )

    fun testSetterPrivate3() = doFileTest(
            """
            var a : Int = 0
                private set(<caret>)
            """
            ,
            """
            var a : Int = 0
                private set(value) {
                    <caret>
                }
            """
    )

    fun testSetterPrivate4() = doFileTest(
            """
            var a : Int = 0
                private set(v<caret>)
            """
            ,
            """
            var a : Int = 0
                private set(v) {
                    <caret>
                }
            """
    )

    fun testSetterPrivate5() = doFileTest(
            """
            var a : Int = 0
                private set(<caret>) {
                }
            """
            ,
            """
            var a : Int = 0
                private set(value) {
                    <caret>
                }
            """
    )

    fun testSetterPrivate6() = doFileTest(
            """
            var a : Int = 0
                private set(v<caret>) {
                }
            """
            ,
            """
            var a : Int = 0
                private set(v) {
                    <caret>
                }
            """
    )

    fun testTryBody() = doFunTest(
            """
            try<caret>
            """
            ,
            """
            try {
                <caret>
            }
            """
    )

    fun testCatchBody() = doFunTest(
            """
            try {
            } catch(e: Exception) <caret>
            """
            ,
            """
            try {
            } catch (e: Exception) {
                <caret>
            }${" "}
            """
    )

    fun testCatchParameter1() = doFunTest(
            """
            try {
            } catch<caret>
            """
            ,
            """
            try {
            } catch (<caret>) {
            }
            """
    )

    fun testCatchParameter2() = doFunTest(
            """
            try {
            } catch(<caret>
            """
            ,
            """
            try {
            } catch (<caret>) {
            }
            """
    )

    fun testCatchParameter3() = doFunTest(
            """
            try {
            } catch(<caret> {}
            """
            ,
            """
            try {
            } catch (<caret>) {
            }
            """
    )

    fun testCatchParameter4() = doFunTest(
            """
            try {
            } catch(e: Exception<caret>
            """
            ,
            """
            try {
            } catch (e: Exception) {
                <caret>
            }
            """
    )

    fun testFinallyBody() = doFunTest(
            """
            try {
            } catch(e: Exception) {
            } finally<caret>
            """
            ,
            """
            try {
            } catch (e: Exception) {
            } finally {
                <caret>
            }
            """
    )

    fun testLambdaParam() = doFileTest(
            """
            fun foo(a: Any, block: () -> Unit) {
            }
            fun test() {
                foo(Any()<caret>)
            }
            """
            ,
            """
            fun foo(a: Any, block: () -> Unit) {
            }
            fun test() {
                foo(Any()) { <caret>}
            }
            """
    )

    fun testExtensionLambdaParam() = doFileTest(
            """
            fun foo(a: Any, block: Any.() -> Unit) {
            }
            fun test() {
                foo(Any()<caret>)
            }
            """
            ,
            """
            fun foo(a: Any, block: Any.() -> Unit) {
            }
            fun test() {
                foo(Any()) { <caret>}
            }
            """
    )

    fun testClassInit() = doFileTest(
            """
            class Foo {
                init<caret>
            }
            """
            ,
            """
            class Foo {
                init {
                    <caret>
                }
            }
            """
    )

    fun testClassBody1() = doFileTest(
            """
            class Foo<caret>
            """
            ,
            """
            class Foo {
                <caret>
            }
            """
    )

    fun testClassBody2() = doFileTest(
            """
            class <caret>Foo
            """
            ,
            """
            class Foo {
                <caret>
            }
            """
    )

    fun testObjectExpressionBody1() = doFileTest(
            """
            interface I
            val a = object : I<caret>
            """
            ,
            """
            interface I
            val a = object : I {
                <caret>
            }
            """
    )

    fun testObjectExpressionBody2() = doFileTest(
            """
            interface I
            val a = object : I<caret>

            val b = ""
            """
            ,
            """
            interface I
            val a = object : I {
                <caret>
            }

            val b = ""
            """
    )

    fun testEmptyLine() = doFileTest(
        """fun foo() {}
<caret>""",
        """fun foo() {}

<caret>"""
    )

    fun testValueArgumentList1() = doFileTest(
        """
        fun foo(i: Int) = 1
        fun test1() {
            foo(1<caret>
        }
        """,
        """
        fun foo(i: Int) = 1
        fun test1() {
            foo(1)<caret>
        }
        """
    )

    fun testValueArgumentList2() = doFileTest(
        """
        fun foo(i: Int) = 1
        fun test2() {
            foo(foo(1<caret>
        }
        """,
        """
        fun foo(i: Int) = 1
        fun test2() {
            foo(foo(1))<caret>
        }
        """
    )

    fun testValueArgumentList3() = doFileTest(
        """
        fun foo(i: Int) = 1
        fun test3() {
            foo(<caret>
        }
        """,
        """
        fun foo(i: Int) = 1
        fun test3() {
            foo(<caret>)
        }
        """
    )

    fun testValueArgumentList4() = doFileTest(
        """
        fun foo(i: Int) = 1
        fun test4() {
            foo(1,<caret>
        }
        """,
        """
        fun foo(i: Int) = 1
        fun test4() {
            foo(1, <caret>)
        }
        """
    )

    fun testValueArgumentList5() = doFileTest(
        """
        class Foo(i: Int)
        fun test5() {
            Foo(1<caret>
        }
        """,
        """
        class Foo(i: Int)
        fun test5() {
            Foo(1)<caret>
        }
        """
    )

    fun doFunTest(before: String, after: String) {
        fun String.withFunContext(): String {
            val bodyText = "//----\n${this.trimIndent()}\n//----"
            val withIndent = bodyText.prependIndent("    ")

            return "fun method() {\n$withIndent\n}"
        }

        doTest(before.withFunContext(), after.withFunContext())
    }

    fun doFileTest(before: String, after: String) {
        doTest(before.trimIndent().removeFirstEmptyLines(), after.trimIndent().removeFirstEmptyLines())
    }

    fun doTest(before: String, after: String) {
        myFixture.configureByText(KotlinFileType.INSTANCE, before)
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_COMPLETE_STATEMENT)
        myFixture.checkResult(after)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = LightCodeInsightFixtureTestCase.JAVA_LATEST

    private fun String.removeFirstEmptyLines() = this.split("\n").dropWhile { it.isEmpty() }.joinToString(separator = "\n")

}
