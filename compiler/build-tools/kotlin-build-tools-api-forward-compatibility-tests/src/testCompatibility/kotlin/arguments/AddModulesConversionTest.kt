/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_ADD_MODULES
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class AddModulesConversionTest : BaseArgumentTest<Array<String>>("Xadd-modules") {

    @DisplayName("AddModules is converted to '-Xadd-modules' argument")
    @Test
    fun testAddModulesToArgumentString() {
        val modules = arrayOf("module1", "module2", "module3")
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_ADD_MODULES] = modules
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(modules)),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xadd-modules' has the default value when AddModules is not set")
    @Test
    fun testAddModulesNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("AddModules can be set and retrieved")
    @Test
    fun testAddModulesGetWhenSet() {
        val expectedModules = arrayOf("module1", "module2", "module3")
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_ADD_MODULES] = expectedModules
        }

        val actualModules = jvmOperation.compilerArguments[X_ADD_MODULES]

        assertArrayEquals(expectedModules, actualModules)
    }

    @DisplayName("AddModules has the default value when not set")
    @Test
    fun testAddModulesGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val modules = jvmOperation.compilerArguments[X_ADD_MODULES]

        assertEquals(
            getDefaultValueString(), getValueString(modules)
        )
    }

    @DisplayName("Raw argument strings '-Xadd-modules=<value>' are converted to AddModules")
    @Test
    fun testRawArgumentsAddModulesConversion() {
        val expectedModules = arrayOf("module1", "module2", "module3")
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(getValueString(expectedModules))
        )

        assertArrayEquals(
            expectedModules, operation.compilerArguments[X_ADD_MODULES]
        )
    }

    @DisplayName("AddModules has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsAddModules() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[X_ADD_MODULES])
        )
    }

    @DisplayName("AddModules of null value is converted to '-Xadd-modules' argument")
    @Test
    fun testNullAddModules() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_ADD_MODULES] = null
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(null)),
            actualArgumentStrings,
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: Array<String>?): String? = argument?.joinToString(",")
}