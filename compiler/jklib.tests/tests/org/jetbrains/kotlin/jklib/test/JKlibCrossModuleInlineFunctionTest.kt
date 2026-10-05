/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jklib.test

import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jklib.K2JKlibCompiler
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Checks that the inline function bodies of a dependency, inlined on the first phase, can be linked when they refer to
 * declarations that the first phase of the dependency generated, such as synthetic accessors. Those declarations exist
 * only in the IR of the dependency KLIB, not in its metadata.
 */
class JKlibCrossModuleInlineFunctionTest {

    private val stdlibKlib: String
        get() = ForTestCompileRuntime.jklibStdlibForTests().path

    private fun compile(tempDir: File, moduleName: String, source: String, vararg extraArgs: String): File {
        val srcFile = File(tempDir, "$moduleName.kt").apply { writeText(source.trimIndent()) }
        val outputKlib = File(tempDir, "$moduleName.klib")
        val args = listOf(
            srcFile.path,
            "-d", outputKlib.path,
            "-module-name", moduleName,
            "-no-stdlib",
            "-Xklib-ir-inliner=full",
        ) + extraArgs
        val result = AbstractCliTest.executeCompilerGrabOutput(K2JKlibCompiler(), args)
        assertEquals(ExitCode.OK, result.second) { "Compilation of $moduleName failed: ${result.first}" }
        return outputKlib
    }

    private fun compileLibAndApp(tempDir: File, libSource: String, appSource: String, friend: Boolean = false) {
        val libKlib = compile(tempDir, "lib", libSource, "-Xklib=$stdlibKlib")
        val friendArgs = if (friend) arrayOf("-friend-modules", libKlib.path) else emptyArray()
        compile(tempDir, "app", appSource, "-Xklib=$stdlibKlib${File.pathSeparator}${libKlib.path}", *friendArgs)
    }

    @Test
    fun testOuterThisAccessedFromInlineFunctionOfInnerClass(@TempDir tempDir: File) {
        compileLibAndApp(
            tempDir,
            libSource = """
                package lib

                class A {
                    val x: Int = 42

                    inner class B {
                        inline fun foo() = x
                    }
                }
                """,
            appSource = """
                package app

                import lib.A

                fun test() = A().B().foo()
                """,
        )
    }

    @Test
    fun testPrivateTopLevelMembersLeakedThroughInternalInlineFunction(@TempDir tempDir: File) {
        compileLibAndApp(
            tempDir,
            libSource = """
                package lib

                private val privateTopLevelProperty: Int = 1

                private fun privateTopLevelFunction(): Int = 2

                internal inline fun internalInlineFunction(): Int = privateTopLevelProperty + privateTopLevelFunction()
                """,
            appSource = """
                package app

                import lib.internalInlineFunction

                fun test() = internalInlineFunction()
                """,
            friend = true,
        )
    }

    @Test
    fun testPrivateClassMembersLeakedThroughInternalInlineFunction(@TempDir tempDir: File) {
        compileLibAndApp(
            tempDir,
            libSource = """
                package lib

                class ClassWithPrivateMembers {
                    private val privateProperty: Int = 1

                    private fun privateFunction(): Int = 2

                    internal inline fun internalInlineFunction(): Int = privateProperty + privateFunction()
                }
                """,
            appSource = """
                package app

                import lib.ClassWithPrivateMembers

                fun test() = ClassWithPrivateMembers().internalInlineFunction()
                """,
            friend = true,
        )
    }
}
