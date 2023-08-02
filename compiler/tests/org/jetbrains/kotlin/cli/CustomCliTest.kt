/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.junit.Assert
import java.io.File
import java.util.jar.JarFile

private const val EMPTY_MAIN_FUN = "fun main() {}"

class CustomCliTest : TestCaseWithTmpdir() {
    fun testArgfileWithNonTrivialWhitespaces() {
        val text = "-include-runtime\r\n\t\t-language-version\n\t1.5\r\n-version"
        val argfile = File(tmpdir, "argfile").apply { writeText(text, Charsets.UTF_8) }
        CompilerTestUtil.executeCompilerAssertSuccessful(K2JVMCompiler(), listOf("@" + argfile.absolutePath))
    }

    fun testMainClass() {
        val mainKt = tmpdir.resolve("main.kt").apply {
            writeText(EMPTY_MAIN_FUN)
        }
        compileAndCheckMainClass(listOf(mainKt), expectedMainClass = "MainKt")
    }

    fun testMultipleMainClasses() {
        val main1Kt = tmpdir.resolve("main1.kt").apply {
            writeText(EMPTY_MAIN_FUN)
        }
        val main2Kt = tmpdir.resolve("main2.kt").apply {
            writeText(EMPTY_MAIN_FUN)
        }

        compileAndCheckMainClass(listOf(main1Kt, main2Kt), expectedMainClass = null)
    }

    fun testObjectJvmStaticFunctionMainClass() {
        val mainKt = tmpdir.resolve("main.kt").apply {
            writeText(
                """
                    object ObjectMain {
                        @JvmStatic
                        fun main(args: Array<String>) = println("hello")
                    }
                """
            )
        }
        compileAndCheckMainClass(listOf(mainKt), expectedMainClass = "ObjectMain")
    }

    fun testCompanionObjectJvmStaticFunctionMainClass() {
        val mainKt = tmpdir.resolve("main.kt").apply {
            writeText(
                """
                    class Test {
                        companion object {
                            @JvmStatic
                            fun main(args: Array<String>) = println("hello")
                        }
                    }
                """
            )
        }
        compileAndCheckMainClass(listOf(mainKt), expectedMainClass = "Test")
    }

    fun testInterfaceCompanionObjectJvmStaticFunctionMainClass() {
        val mainKt = tmpdir.resolve("main.kt").apply {
            writeText(
                """
                    interface Test {
                        companion object {
                            @JvmStatic
                            fun main(args: Array<String>) = println("hello")
                        }
                    }
                """
            )
        }
        compileAndCheckMainClass(listOf(mainKt), expectedMainClass = "Test")
    }

    fun testMultipleMainsInOneFile() {
        val mainKt = tmpdir.resolve("main.kt").apply {
            writeText(
                """
                    object ObjectMain {
                        @JvmStatic
                        fun main(args: Array<String>) = println("hello")
                    }
                    object ObjectMain2 {
                        @JvmStatic
                        fun main(args: Array<String>) = println("hello2")
                    }
                    fun main(args: Array<String>) = println("hello3")
                """
            )
        }
        compileAndCheckMainClass(listOf(mainKt), expectedMainClass = null)
    }

    private fun compileAndCheckMainClass(sourceFiles: List<File>, expectedMainClass: String?) {
        val jarFile = tmpdir.resolve("output.jar")
        // TODO: remove explicit verion after implementing main fun detector (KT-44557)
        val args = listOf("-language-version", "1.9", "-include-runtime", "-d", jarFile.absolutePath) + sourceFiles.map { it.absolutePath }
        CompilerTestUtil.executeCompilerAssertSuccessful(K2JVMCompiler(), args)

        JarFile(jarFile).use {
            val mainClassAttr = it.manifest.mainAttributes.getValue("Main-Class")
            Assert.assertEquals(expectedMainClass, mainClassAttr)
        }
    }
}
