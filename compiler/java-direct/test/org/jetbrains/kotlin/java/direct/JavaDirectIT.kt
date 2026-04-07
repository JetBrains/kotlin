/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.LanguageFeature
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Path

class JavaDirectIT {

    @Test
    fun testinPsiMode(@TempDir tempDir: Path) {
        val files = createFilesForPseudoRawTypesTest(tempDir)

        val outStream = ByteArrayOutputStream()
        val exitCodePsi = K2JVMCompiler().exec(
            PrintStream(outStream),
            "-d", tempDir.toFile().resolve("out").absolutePath,
            *(files.map { it.absolutePath }.toTypedArray())
        )
        assert(exitCodePsi == ExitCode.OK)
    }

    @Test
    fun testEnableViaLanguageFeature(@TempDir tempDir: java.nio.file.Path) {
        val files = createFilesForPseudoRawTypesTest(tempDir)

        val outStream = ByteArrayOutputStream()
        val exitCodeDirect = K2JVMCompiler().exec(
            PrintStream(outStream),
            "-XXLanguage:+${LanguageFeature.JavaDirect.name}",
            "-d", tempDir.toFile().resolve("out").absolutePath,
            *(files.map { it.absolutePath }.toTypedArray())
        )
        assert(exitCodeDirect == ExitCode.COMPILATION_ERROR)
    }
}

private fun createFilesForPseudoRawTypesTest(tempDir: Path): List<File> {
    val javaUtilDir = tempDir.resolve("java/util").toFile().also { it.mkdirs() }
    val files = listOf(
        javaUtilDir.resolve("Collection.java").also {
            it.writeText(
                """
                    package java.util;

                    public class Collection {
                      public void foo() {}
                    }
                    """.trimIndent()
            )
        },
        tempDir.toFile().resolve("Usage.java").also {
            it.writeText(
                """
                    import java.util.*;

                    public class Usage {
                      void foo(Collection c) {
                        c.foo();
                      }
                    }
                    """.trimIndent()
            )
        },
        tempDir.toFile().resolve("Kotlin.kt").also {
            it.writeText(
                """
                    fun foo(u: Usage) {
                      u.foo(null)
                    }
                    """.trimIndent()
            )
        },
    )
    return files
}