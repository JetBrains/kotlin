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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class JKlibJavaInteropIntegrationTest {

    @Test
    @Disabled("The fix is reverted due to KT-87507")
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

    @Test
    fun testJavaCollectionToArrayOverridability(@TempDir tempDir: File) {
        val stdlibKlib = ForTestCompileRuntime.jklibStdlibForTests().path
        val stdlibJar = ForTestCompileRuntime.runtimeJarForTests().path

        val libBDir = File(tempDir, "libB").apply { mkdirs() }
        File(libBDir, "MyJavaCollection.java").apply {
            writeText(
                """
                package test;

                import java.util.AbstractCollection;
                import java.util.Iterator;

                public class MyJavaCollection<E> extends AbstractCollection<E> {
                    @Override
                    public Iterator<E> iterator() { return null; }

                    @Override
                    public int size() { return 0; }

                    @Override
                    public <T> T[] toArray(T[] a) {
                        return a;
                    }
                }
                """.trimIndent()
            )
        }

        val jarB = MockLibraryUtilExt.compileJavaFilesLibraryToJar(
            libBDir.path,
            "libB",
            extraClasspath = listOf(stdlibJar)
        )

        val mainDir = File(tempDir, "main").apply { mkdirs() }
        val mainKt = File(mainDir, "Main.kt").apply {
            writeText(
                """
                package test

                fun test(c: MyJavaCollection<String>) {
                    val arr = arrayOfNulls<String>(0)
                    val res = c.toArray(arr)
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
                "-Xklib=$stdlibKlib"
            ),
            args
        )

        val messageCollector = MessageCollectorImpl()
        val disposable = Disposer.newDisposable()
        try {
            val artifact = compiler.compileKlibAndDeserializeIr(args, messageCollector, disposable)
            assertNotNull(artifact, "compileKlibAndDeserializeIr returned null. Messages:\n" + messageCollector.messages.joinToString("\n"))
        } finally {
            disposeRootInWriteAction(disposable)
        }
    }

    @Test
    fun testPropertyAndFunctionClash(@TempDir tempDir: File) {
        val stdlibKlib = ForTestCompileRuntime.jklibStdlibForTests().path
        val stdlibJar = ForTestCompileRuntime.runtimeJarForTests().path

        val libBDir = File(tempDir, "libB").apply { mkdirs() }
        File(libBDir, "JavaBase.java").apply {
            writeText(
                """
                package test;

                public class JavaBase {
                    public String getValue() {
                        return "OK";
                    }
                }
                """.trimIndent()
            )
        }

        val jarB = MockLibraryUtilExt.compileJavaFilesLibraryToJar(
            libBDir.path,
            "libB",
            extraClasspath = listOf(stdlibJar)
        )

        val mainDir = File(tempDir, "main").apply { mkdirs() }
        val mainKt = File(mainDir, "Main.kt").apply {
            writeText(
                """
                package test

                class KotlinSub : JavaBase() {
                    val value: String get() = super.getValue()
                }

                fun test(sub: KotlinSub) {
                    val v = sub.value
                    val g = sub.getValue()
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
                "-Xklib=$stdlibKlib"
            ),
            args
        )

        val messageCollector = MessageCollectorImpl()
        val disposable = Disposer.newDisposable()
        try {
            val artifact = compiler.compileKlibAndDeserializeIr(args, messageCollector, disposable)
            assertNotNull(artifact, "compileKlibAndDeserializeIr returned null. Messages:\n" + messageCollector.messages.joinToString("\n"))
        } finally {
            disposeRootInWriteAction(disposable)
        }
    }

    @Test
    fun testVarPropertyVsJavaGetterSetterCollision(@TempDir tempDir: File) {
        val stdlibKlib = ForTestCompileRuntime.jklibStdlibForTests().path
        val stdlibJar = ForTestCompileRuntime.runtimeJarForTests().path

        val libBDir = File(tempDir, "libB").apply { mkdirs() }
        File(libBDir, "JavaBase.java").apply {
            writeText(
                """
                package test;

                public class JavaBase {
                    public String getItem() { return "OK"; }
                    public void setItem(String s) {}
                }
                """.trimIndent()
            )
        }

        val jarB = MockLibraryUtilExt.compileJavaFilesLibraryToJar(
            libBDir.path,
            "libB",
            extraClasspath = listOf(stdlibJar)
        )

        val mainDir = File(tempDir, "main").apply { mkdirs() }
        val mainKt = File(mainDir, "Main.kt").apply {
            writeText(
                """
                package test

                class KotlinSub : JavaBase() {
                    var item: String
                        get() = super.getItem()
                        set(value) { super.setItem(value) }
                }

                fun test(sub: KotlinSub) {
                    sub.item = "new"
                    val i = sub.item
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
                "-Xklib=$stdlibKlib"
            ),
            args
        )

        val messageCollector = MessageCollectorImpl()
        val disposable = Disposer.newDisposable()
        try {
            val artifact = compiler.compileKlibAndDeserializeIr(args, messageCollector, disposable)
            assertNotNull(artifact, "compileKlibAndDeserializeIr returned null. Messages:\n" + messageCollector.messages.joinToString("\n"))
        } finally {
            disposeRootInWriteAction(disposable)
        }
    }

    @Test
    fun testJreMappedClassEqualsHashCodeToString(@TempDir tempDir: File) {
        val stdlibKlib = ForTestCompileRuntime.jklibStdlibForTests().path
        val stdlibJar = ForTestCompileRuntime.runtimeJarForTests().path

        val mainDir = File(tempDir, "main").apply { mkdirs() }
        val mainKt = File(mainDir, "Main.kt").apply {
            writeText(
                """
                package test

                class CustomList : java.util.ArrayList<String>() {
                    override fun equals(other: Any?): Boolean = super.equals(other)
                    override fun hashCode(): Int = super.hashCode()
                    override fun toString(): String = super.toString()
                }

                fun test(list: CustomList) {
                    val eq = list.equals("test")
                    val hc = list.hashCode()
                    val str = list.toString()
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
                "-Xklib=$stdlibKlib"
            ),
            args
        )

        val messageCollector = MessageCollectorImpl()
        val disposable = Disposer.newDisposable()
        try {
            val artifact = compiler.compileKlibAndDeserializeIr(args, messageCollector, disposable)
            assertNotNull(artifact, "compileKlibAndDeserializeIr returned null. Messages:\n" + messageCollector.messages.joinToString("\n"))
        } finally {
            disposeRootInWriteAction(disposable)
        }
    }

    @Test
    fun testLocalClassSignatureDisambiguation(@TempDir tempDir: File) {
        val stdlibKlib = ForTestCompileRuntime.jklibStdlibForTests().path
        val stdlibJar = ForTestCompileRuntime.runtimeJarForTests().path

        val libBDir = File(tempDir, "libB").apply { mkdirs() }
        File(libBDir, "JavaBase.java").apply {
            writeText(
                """
                package test;

                public class JavaBase {
                    public void run() {}
                }
                """.trimIndent()
            )
        }

        val jarB = MockLibraryUtilExt.compileJavaFilesLibraryToJar(
            libBDir.path,
            "libB",
            extraClasspath = listOf(stdlibJar)
        )

        val mainDir = File(tempDir, "main").apply { mkdirs() }
        val mainKt = File(mainDir, "Main.kt").apply {
            writeText(
                """
                package test

                fun outer() {
                    val loc1 = object : JavaBase() {
                        override fun run() {}
                    }
                    val loc2 = object : JavaBase() {
                        override fun run() {}
                    }
                    loc1.run()
                    loc2.run()
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
                "-Xklib=$stdlibKlib"
            ),
            args
        )

        val messageCollector = MessageCollectorImpl()
        val disposable = Disposer.newDisposable()
        try {
            val artifact = compiler.compileKlibAndDeserializeIr(args, messageCollector, disposable)
            assertNotNull(artifact, "compileKlibAndDeserializeIr returned null. Messages:\n" + messageCollector.messages.joinToString("\n"))
        } finally {
            disposeRootInWriteAction(disposable)
        }
    }
}
