/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.test.TestCaseWithTmpdir

class JavaSourceInnerClassInClassPathTest : TestCaseWithTmpdir() {

    // Test that a java source file for a class is taken before a class file for the same
    // class on the class path.
    fun test() {
        val aJava = tmpdir.resolve("A.java").also {
            it.writeText("class A { interface AInner { int foo(); } }")
        }
        val bJava = tmpdir.resolve("B.java").also {
            it.writeText("class B implements A.AInner { public int foo() { return 42; } }")
        }
        val cKt = tmpdir.resolve("C.kt").also {
            it.writeText("fun main() { B().foo() }")
        }
        val (output, exit) = AbstractCliTest.executeCompilerGrabOutput(
            K2JVMCompiler(),
            listOf(aJava.path, bJava.path, cKt.path, "-d", tmpdir.path, "-Xcompile-java", "-Xuse-javac")
        )
        assert(exit == ExitCode.OK) { output }
        val (output2, exit2) = AbstractCliTest.executeCompilerGrabOutput(
            K2JVMCompiler(),
            listOf(aJava.path, bJava.path, cKt.path, "-cp", tmpdir.path, "-d", tmpdir.path)
        )
        assert(exit2 == ExitCode.OK) { output2 }
    }
}
