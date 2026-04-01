/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.SCRIPT_TEMPLATES
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import java.nio.file.Paths

internal class ScriptTemplatesConversionTest : BaseArgumentTest<List<String>>("script-templates") {

    @DisplayName("ScriptTemplates is converted to '-script-templates' argument")
    @BtaVersionsOnlyCompilationTest
    fun testScriptTemplatesToArgumentString(toolchain: KotlinToolchains) {
        val templates = listOf("org.example.Template1", "org.example.Template2", "org.example.Template3")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[SCRIPT_TEMPLATES] = templates
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(templates), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-script-templates' has the default value when ScriptTemplates is not set")
    @BtaVersionsOnlyCompilationTest
    fun testScriptTemplatesNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("ScriptTemplates can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testScriptTemplatesGetWhenSet(toolchain: KotlinToolchains) {
        val expectedTemplates = listOf("org.example.Template1", "org.example.Template2", "org.example.Template3")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[SCRIPT_TEMPLATES] = expectedTemplates
        }.build()

        val actualTemplates = jvmOperation.compilerArguments[SCRIPT_TEMPLATES]

        assertEquals(expectedTemplates, actualTemplates)
    }

    @DisplayName("ScriptTemplates has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testScriptTemplatesGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val templates = jvmOperation.compilerArguments[SCRIPT_TEMPLATES]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(templates)
        )
    }

    @DisplayName("Raw argument strings '-script-templates=<value>' are converted to ScriptTemplates")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsScriptTemplatesConversion(toolchain: KotlinToolchains) {
        val expectedTemplates = listOf("org.example.Template1", "org.example.Template2", "org.example.Template3")
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(
                getValueString(expectedTemplates),
                toolchain.getCompilerVersion()
            )
        )

        assertEquals(
            expectedTemplates,
            operation.compilerArguments[SCRIPT_TEMPLATES]
        )
    }

    @DisplayName("ScriptTemplates has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsScriptTemplates(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[SCRIPT_TEMPLATES])
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName", value)
    }

    override fun getValueString(argument: List<String>?): String? = argument?.joinToString(",")
}
