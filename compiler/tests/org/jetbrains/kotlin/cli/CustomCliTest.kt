/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import java.io.File

class CustomCliTest : TestCaseWithTmpdir() {
    fun testArgfileWithNonTrivialWhitespaces() {
        val text = "-include-runtime\r\n\t\t-language-version\n\t1.2\r\n-version"
        val argfile = File(tmpdir, "argfile").apply { writeText(text, Charsets.UTF_8) }
        CompilerTestUtil.executeCompilerAssertSuccessful(K2JVMCompiler(), listOf("@" + argfile.absolutePath))
    }
}
