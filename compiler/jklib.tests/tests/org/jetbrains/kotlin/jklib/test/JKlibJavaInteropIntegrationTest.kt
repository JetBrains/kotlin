/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jklib.test

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.disposeRootInWriteAction
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.cli.jklib.K2JKlibCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.MockLibraryUtilExt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class JKlibJavaInteropIntegrationTest {

    @Test
    fun testJavaExtendingNestedKotlinClassFromKlib(@TempDir tempDir: File) {
        val stdlibKlib = ForTestCompileRuntime.jklibStdlibForTests().path
        val stdlibJar = ForTestCompileRuntime.runtimeJarForTests().path

        val libADir = File(tempDir, "libA").apply { mkdirs() }
        val outerKt = File(libADir, "Outer.kt").apply {
            writeText(
                """
                package test

                open class Outer {
                    open class Inner {
                        val prop: String = "OK"
                        fun foo(): String = "OK"
                    }
                }
                """.trimIndent()
            )
        }

        val klibA = File(tempDir, "libA.klib")
        val resA = AbstractCliTest.executeCompilerGrabOutput(
            K2JKlibCompiler(),
            listOf(
                outerKt.path,
                "-d", klibA.path,
                "-module-name", "libA",
                "-no-stdlib",
                "-Xklib=$stdlibKlib"
            )
        )
        assertEquals(ExitCode.OK, resA.second, "Failed to compile libA klib: ${resA.first}")

        val jarA = File(tempDir, "libA.jar")
        val resAJvm = AbstractCliTest.executeCompilerGrabOutput(
            K2JVMCompiler(),
            listOf(
                outerKt.path,
                "-d", jarA.path,
                "-no-stdlib",
                "-classpath", stdlibJar
            )
        )
        assertEquals(ExitCode.OK, resAJvm.second, "Failed to compile libA jar: ${resAJvm.first}")

        val libBDir = File(tempDir, "libB").apply { mkdirs() }
        File(libBDir, "JavaClass.java").apply {
            writeText(
                """
                package test;

                public class JavaClass extends Outer.Inner {
                }
                """.trimIndent()
            )
        }

        val jarB = MockLibraryUtilExt.compileJavaFilesLibraryToJar(
            libBDir.path,
            "libB",
            extraClasspath = listOf(jarA.path)
        )

        val mainDir = File(tempDir, "main").apply { mkdirs() }
        val mainKt = File(mainDir, "Main.kt").apply {
            writeText(
                """
                package test

                fun test() {
                    val j = JavaClass()
                    val x = j.prop
                }
                """.trimIndent()
            )
        }

        val mainKlib = File(tempDir, "main.klib")
        val compiler = K2JKlibCompiler()
        val args = compiler.createArguments()
        compiler.parseArguments(
            arrayOf(
                mainKt.path,
                "-d", mainKlib.path,
                "-module-name", "main",
                "-no-stdlib",
                "-classpath", jarB.path,
                "-Xklib=$stdlibKlib${File.pathSeparator}${klibA.path}"
            ),
            args
        )

        val messageCollector = MessageCollectorImpl()
        val disposable = Disposer.newDisposable()
        try {
            val artifact = compiler.compileKlibAndDeserializeIr(args, messageCollector, disposable)
            if (artifact == null) {
                error("compileKlibAndDeserializeIr returned null. Messages:\n" + messageCollector.messages.joinToString("\n"))
            }
        } finally {
            disposeRootInWriteAction(disposable)
        }
    }
}
