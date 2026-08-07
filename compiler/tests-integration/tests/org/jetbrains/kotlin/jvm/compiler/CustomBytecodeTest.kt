/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.util.Textifier
import org.jetbrains.org.objectweb.asm.util.TraceClassVisitor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter

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
}
