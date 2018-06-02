/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parsing.utils

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.parsing.util.classesFqNames
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class ClassesFqNamesTest {
    private lateinit var workingDir: File

    @Before
    fun setUp() {
        workingDir = FileUtil.createTempDirectory("ClassesFqNamesTest", null)
    }

    @After
    fun tearDown() {
        workingDir.deleteRecursively()
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