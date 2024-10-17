/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.KtAssert
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import java.io.File

class JVMSourceRangesTest : TestCaseWithTmpdir() {
    private val BASE_DIR = File("compiler/testData/serialization/sourceRanges/kt72356")

    private fun compileFiles(vararg fileNames: String): Pair<String, ExitCode> {
        return CompilerTestUtil.executeCompiler(
            K2JVMCompiler(),
            fileNames.map { BASE_DIR.resolve(it).absolutePath } + listOf("-d", tmpdir.absolutePath)
        )
    }

    fun testKT72356PassWhenDifferentSourceRange() {
        val (message, exitCode) = compileFiles("A.kt", "C.kt", "EDifferentSourceRange.kt")
        KtAssert.assertEquals("Wrong exitCode=$exitCode, message: $message", ExitCode.OK, exitCode)
        KtAssert.assertEquals("Wrong compiler message: $message", "", message)
    }

    // Reproducer for KT-72356
    fun testKT72356FailWhenSameSourceRange() {
        val (message, exitCode) = compileFiles("A.kt", "C.kt", "ESameSourceRange.kt")
        // TODO: KT-72356: Fix the issue with `FirExpression.toConstantValue()`, correct testcase name and expected results below
        KtAssert.assertEquals("Wrong exitCode=$exitCode, message: $message", ExitCode.INTERNAL_ERROR, exitCode)
        KtAssert.assertTrue(
            "Wrong compiler message: $message",
            message.contains("java.lang.IllegalStateException: Cannot serialize annotation @R|Something|()"))
    }
}
