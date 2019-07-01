/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.parsing

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class ClassesFqNamesTest : KtUsefulTestCase() {
    private lateinit var workingDir: File

    @Before
    override fun setUp() {
        super.setUp()
        workingDir = FileUtil.createTempDirectory("ClassesFqNamesTest", null)
    }

    @After
    override fun tearDown() {
        workingDir.deleteRecursively()
        super.tearDown()
    }

    @Test
    fun testSingleClass() {
        doTest(
            setOf("test.Foo"),
            """
                package test

                class Foo""".trimIndent()
        )
    }

    @Test
    fun testComplexPackage() {
        doTest(
            setOf("foo.bar.юникод.Foo"),
            """
                // package simpleComment
                package foo . bar . `юникод`
                /*
                    package multiLineComment
                */

                class Foo""".trimIndent()
        )
    }

    @Test
    fun testDifferentTypeOfClasses() {
        doTest(
            setOf("test.C", "test.I", "test.O", "test.E", "test.A"),
            """
                package test

                class C
                interface I
                object O
                enum class E
                annotation class A
                typealias T""".trimIndent()
        )
    }

    @Test
    fun testLocalClass() {
        doTest(
            setOf("test.Foo"),
            """
                package test

                fun f() {
                    class Fizz
                }

                class Foo {
                    fun m() {
                        class Buzz
                    }
                }""".trimIndent()
        )
    }

    @Test
    fun testMultipleClasses() {
        doTest(
            setOf("test.Fizz", "test.Buzz"),
            """
                package test

                class Fizz
                class Buzz""".trimIndent()
        )
    }

    @Test
    fun testInnerClasses() {
        doTest(
            setOf("test.Foo", "test.Foo.Fizz", "test.Foo.Buzz"),
            """
                package test

                class Foo {
                    class Fizz
                    class Buzz
                }""".trimIndent()
        )
    }

    @Test
    fun testObject() {
        doTest(
            setOf("test.Foo", "test.Foo.Fizz", "test.Bar", "test.Bar.Buzz"),
            """
                package test

                object Foo {
                    class Fizz
                }
                
                class Bar {
                    object Buzz
                }""".trimIndent()
        )
    }

    private fun doTest(expectedClasses: Set<String>, code: String) {
        val testKt = File(workingDir, "test.kt")
        testKt.writeText(code)

        val expected = expectedClasses.sorted().joinToString("\n")
        val actual = classesFqNames(setOf(testKt)).sorted().joinToString("\n")
        UsefulTestCase.assertEquals(expected, actual)
    }
}