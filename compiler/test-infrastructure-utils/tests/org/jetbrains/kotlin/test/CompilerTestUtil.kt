/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.cli.common.CLITool
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

object CompilerTestUtil {
    @JvmStatic
    fun executeCompilerAssertSuccessful(compiler: CLITool<*>, args: List<String>, messageRenderer: MessageRenderer? = null) {
        val (output, exitCode) = executeCompiler(compiler, args, messageRenderer)
        KtAssert.assertEquals(output, ExitCode.OK, exitCode)
    }

    @JvmStatic
    fun executeCompiler(compiler: CLITool<*>, args: List<String>, messageRenderer: MessageRenderer? = null): Pair<String, ExitCode> {
        val bytes = ByteArrayOutputStream()
        val origErr = System.err
        try {
            System.setErr(PrintStream(bytes))
            val exitCode =
                if (messageRenderer == null) CLITool.doMainNoExit(compiler, args.toTypedArray())
                else CLITool.doMainNoExit(compiler, args.toTypedArray(), messageRenderer)
            return Pair(String(bytes.toByteArray()), exitCode)
        } finally {
            System.setErr(origErr)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun compileJvmLibrary(
            src: File,
            libraryName: String = "library",
            extraOptions: List<String> = emptyList(),
            extraClasspath: List<File> = emptyList(),
            messageRenderer: MessageRenderer? = null,
    ): File {
        val destination = File(KtTestUtil.tmpDir("testLibrary"), "$libraryName.jar")
        val args = mutableListOf<String>().apply {
            add(src.path)
            add("-d")
            add(destination.path)
            if (extraClasspath.isNotEmpty()) {
                add("-cp")
                add(extraClasspath.joinToString(":") { it.path })
            }
            addAll(extraOptions)
        }
        executeCompilerAssertSuccessful(K2JVMCompiler(), args, messageRenderer)
        return destination
    }
}
