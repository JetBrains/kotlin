/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.test.TestCaseWithTmpdir

class JavaSourceClassInClassPathTest : TestCaseWithTmpdir() {

    // Test that a java source file for a class is taken before a class file for the same
    // class on the class path.
    fun testDeterministicOutput() {
        val aKt = tmpdir.resolve("A.kt").also {
            it.writeText("class A")
        }
        val bKt = tmpdir.resolve("B.kt").also {
            it.writeText("fun main() { A() }")
        }
        val aJava = tmpdir.resolve("A.java").also {
            it.writeText("public class A { public int i = 32; }")
        }
        val bNewKt = tmpdir.resolve("Bnew.kt").also {
            it.writeText("fun main() { A().i }")
        }
        val firstJar = tmpdir.resolve("first.jar")
        val (_, exit) = AbstractCliTest.executeCompilerGrabOutput(
            K2JVMCompiler(),
            listOf(aKt.path, bKt.path, "-d", firstJar.path, "-include-runtime")
        )
        assert(exit == ExitCode.OK)
        val (_, exit2) = AbstractCliTest.executeCompilerGrabOutput(
            K2JVMCompiler(),
            listOf("-cp", firstJar.path, aJava.path, bNewKt.path, "-d", firstJar.path, "-include-runtime")
        )
        assert(exit2 == ExitCode.OK)
    }
}
