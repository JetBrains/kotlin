/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.SCRIPT_TEMPLATES
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Paths

internal class ScriptTemplatesConversionTest : BaseArgumentTest<Array<String>>("script-templates") {

    @DisplayName("ScriptTemplates is converted to '-script-templates' argument")
    @Test
    fun testScriptTemplatesToArgumentString() {
        val templates = arrayOf("org.example.Template1", "org.example.Template2", "org.example.Template3")
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[SCRIPT_TEMPLATES] = templates
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(templates)),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-script-templates' has the default value when ScriptTemplates is not set")
    @Test
    fun testScriptTemplatesNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("ScriptTemplates can be set and retrieved")
    @Test
    fun testScriptTemplatesGetWhenSet() {
        val expectedTemplates = arrayOf("org.example.Template1", "org.example.Template2", "org.example.Template3")
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[SCRIPT_TEMPLATES] = expectedTemplates
        }

        val actualTemplates = jvmOperation.compilerArguments[SCRIPT_TEMPLATES]

        assertArrayEquals(expectedTemplates, actualTemplates)
    }

    @DisplayName("ScriptTemplates has the default value when not set")
    @Test
    fun testScriptTemplatesGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val templates = jvmOperation.compilerArguments[SCRIPT_TEMPLATES]

        assertEquals(
            getDefaultValueString(), getValueString(templates)
        )
    }

    @DisplayName("Raw argument strings '-script-templates=<value>' are converted to ScriptTemplates")
    @Test
    fun testRawArgumentsScriptTemplatesConversion() {
        val expectedTemplates = arrayOf("org.example.Template1", "org.example.Template2", "org.example.Template3")
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(getValueString(expectedTemplates))
        )

        assertArrayEquals(
            expectedTemplates, operation.compilerArguments[SCRIPT_TEMPLATES]
        )
    }

    @DisplayName("ScriptTemplates has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsScriptTemplates() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[SCRIPT_TEMPLATES])
        )
    }

    @DisplayName("ScriptTemplates of null value is converted to '-script-templates' argument")
    @Test
    fun testNullScriptTemplates() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[SCRIPT_TEMPLATES] = null
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(null)),
            actualArgumentStrings,
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName", value)
    }

    override fun getValueString(argument: Array<String>?): String? = argument?.joinToString(",")
}
