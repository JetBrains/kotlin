/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.util.Textifier
import org.jetbrains.org.objectweb.asm.util.TraceClassVisitor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter

@Suppress("CanConvertToMultiDollarString")
class CustomBytecodeTest : AbstractKotlinCompilerIntegrationTest() {
    override val testDataPath: String
        get() = tmpdir.absolutePath

    @Test
    fun testEnumMapping() {
        val bytes = compileAndLoadBytes(
            """
            enum class MyEnum {
                ENTRY1, ENTRY2, ENTRY3, ENTRY4
            }
        
            fun f(e: MyEnum) {
                when (e) {
                    MyEnum.ENTRY4 -> {}
                    MyEnum.ENTRY3 -> {}
                    MyEnum.ENTRY2 -> {}
                    MyEnum.ENTRY1 -> {}
                }
            }
            """.trimIndent(),
            "\$WhenMappings",
        )
        val bytecodeText = renderBytecode(bytes)
        val getstatics = bytecodeText.lines().filter { it.contains("GETSTATIC MyEnum.") }.map { it.trim() }
        assertEquals(
            listOf(
                "GETSTATIC MyEnum.ENTRY4 : LMyEnum;",
                "GETSTATIC MyEnum.ENTRY3 : LMyEnum;",
                "GETSTATIC MyEnum.ENTRY2 : LMyEnum;",
                "GETSTATIC MyEnum.ENTRY1 : LMyEnum;"
            ).joinToString("\n"),
            getstatics.joinToString("\n"),
        ) { "actual bytecode:\n$bytecodeText" }
    }

    @Test
    fun testDelegatedMethod() {
        doMethodOrderTest(
            """
                interface Trait {
                    fun f0()
                    fun f4()
                    fun f3()
                    fun f2()
                    fun f1()
                    fun f5()
                }

                val delegate: Trait = throw Error()

                val obj = object : Trait by delegate {
                    override fun f3() { }
                }
            """,
            "\$obj$1",
            listOf("<init>()V", "f3()V", "f0()V", "f4()V", "f2()V", "f1()V", "f5()V"),
        )
    }

    @Test
    fun testAnonymousObjectClosureOrdering() {
        doMethodOrderTest(
            """
                class Klass {
                    fun Any.f(a: String, b: Int, c: Double, d: Any, e: Long) {
                        object : Runnable {
                            override fun run() {
                                a + b + c + d + e + this@f + this@Klass
                            }
                        }.run()
                    }
                }
            """,
            "\$f$1",
            listOf("<init>(Ljava/lang/String;IDLjava/lang/Object;JLjava/lang/Object;LKlass;)V", "run()V")
        )
    }

    @Test
    fun testMemberAccessor() {
        doMethodOrderTest(
            """
                class Outer(private val a: Int, private var b: String) {
                    private fun c() {
                    }

                    inner class Inner() {
                        init {
                            b = b + a
                            c()
                        }
                    }
                }
            """,
            "Outer",
            listOf(
                "<init>(ILjava/lang/String;)V",
                "c()V",
                "access\$setB\$p(LOuter;Ljava/lang/String;)V",
                "access\$getB\$p(LOuter;)Ljava/lang/String;",
                "access\$getA\$p(LOuter;)I",
                "access\$c(LOuter;)V"
            )
        )
    }

    @Test
    fun testDeterministicDefaultMethodImplOrder() {
        doMethodOrderTest(
            """
                interface Base<K, V> {
                    fun getSize(): Int = 5
                    fun size(): Int = getSize()
                    fun getKeys(): Int = 4
                    fun keySet() = getKeys()
                    fun getEntries(): Int = 3
                    fun entrySet() = getEntries()
                    fun getValues(): Int = 2
                    fun values() = getValues()

                    fun removeEldestEntry(eldest: Any?): Boolean
                }

                class MinMap<K, V> : Base<K, V> {
                    override fun removeEldestEntry(eldest: Any?) = true
                }
            """,
            "MinMap",
            listOf(
                "<init>()V",
                "removeEldestEntry(Ljava/lang/Object;)Z",
                "getSize()I",
                "size()I",
                "getKeys()I",
                "keySet()I",
                "getEntries()I",
                "entrySet()I",
                "getValues()I",
                "values()I"
            )
        )
    }

    @Test
    fun testBridgeOrder() {
        doMethodOrderTest(
            """
                interface IrElement
                class IrClassContext

                interface IrElementVisitor<out R, in D> {
                    fun visitElement(element: IrElement, data: D): R
                }

                interface IrElementTransformer<in D> : IrElementVisitor<IrElement, D> {
                    override fun visitElement(element: IrElement, data: D): IrElement =
                            element.also { throw RuntimeException() }
                }

                abstract class ClassLowerWithContext : IrElementTransformer<IrClassContext?>
            """,
            "ClassLowerWithContext",
            listOf(
                "<init>()V",
                "visitElement(LIrElement;LIrClassContext;)LIrElement;",
                "visitElement(LIrElement;Ljava/lang/Object;)LIrElement;",
                "visitElement(LIrElement;Ljava/lang/Object;)Ljava/lang/Object;",
            )
        )
    }

    private fun compileAndLoadBytes(source: String, classSuffix: String): ByteArray {
        File(testDataDirectory, "src/test.kt").apply { parentFile.mkdirs() }.writeText(source)
        val destination = compileLibrary("src", testDataDirectory.resolve("out"))

        val files = destination.walk().filter { it.isFile }.filter { it.name.endsWith("$classSuffix.class") }
        val file = files.singleOrNull() ?: error("No single file whose name ends with $classSuffix:\n${files.joinToString("\n")}")
        return file.readBytes()
    }

    private fun renderBytecode(bytes: ByteArray): String {
        val out = ByteArrayOutputStream()
        val writer = PrintWriter(out.writer())
        ClassReader(bytes).accept(TraceClassVisitor(null, Textifier(), writer), 0)
        return String(out.toByteArray())
    }

    private fun doMethodOrderTest(source: String, classSuffix: String, expectedOrder: List<String>) {
        val bytes = compileAndLoadBytes(source, classSuffix)

        val methodNames = mutableListOf<String>()
        ClassReader(bytes).accept(object : ClassVisitor(Opcodes.API_VERSION) {
            override fun visitMethod(
                access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?,
            ): MethodVisitor? {
                methodNames.add(name + desc)
                return null
            }
        }, ClassReader.SKIP_CODE and ClassReader.SKIP_DEBUG and ClassReader.SKIP_FRAMES)

        assertEquals(expectedOrder, methodNames)
    }
}
