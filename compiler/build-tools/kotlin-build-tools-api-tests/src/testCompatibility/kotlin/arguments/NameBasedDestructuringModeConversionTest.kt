/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_NAME_BASED_DESTRUCTURING
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.enums.NameBasedDestructuringMode
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class NameBasedDestructuringModeConversionTest : BaseArgumentTest<NameBasedDestructuringMode>("Xname-based-destructuring") {

    private lateinit var toolchain: KotlinToolchains

    @BeforeEach
    fun setup() {
        toolchain = KotlinToolchains.loadImplementation(btaClassloader)
    }

    @DisplayName("NameBasedDestructuringMode is converted to '-Xname-based-destructuring' argument")
    @Test
    fun testNameBasedDestructuringModeToArgumentString() {
        assumeNameBasedDestructuring(toolchain.getCompilerVersion())
        for (nameBasedDestructuringMode in NameBasedDestructuringMode.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_NAME_BASED_DESTRUCTURING] = nameBasedDestructuringMode
            }.build()

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(nameBasedDestructuringMode), toolchain.getCompilerVersion()),
                actualArgumentStrings,
            )
        }
    }

    @DisplayName("'-Xname-based-destructuring' has the default value when NameBasedDestructuringMode is not set")
    @Test
    fun testNameBasedDestructuringModeNotSetByDefault() {
        assumeNameBasedDestructuring(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("NameBasedDestructuringMode can be set and retrieved")
    @Test
    fun testNameBasedDestructuringModeGetWhenSet() {
        assumeNameBasedDestructuring(toolchain.getCompilerVersion())
        for (nameBasedDestructuringMode in NameBasedDestructuringMode.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_NAME_BASED_DESTRUCTURING] = nameBasedDestructuringMode
            }.build()

            val actualNameBasedDestructuringMode = jvmOperation.compilerArguments[X_NAME_BASED_DESTRUCTURING]

            assertEquals(nameBasedDestructuringMode, actualNameBasedDestructuringMode)
        }
    }

    @DisplayName("NameBasedDestructuringMode has the default value when not set")
    @Test
    fun testNameBasedDestructuringModeGetWhenNull() {
        assumeNameBasedDestructuring(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val nameBasedDestructuringMode = jvmOperation.compilerArguments[X_NAME_BASED_DESTRUCTURING]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(nameBasedDestructuringMode)
        )
    }

    @DisplayName("Raw argument strings '-Xname-based-destructuring=<value>' are converted to NameBasedDestructuringMode")
    @Test
    fun testRawArgumentsNameBasedDestructuringConversion() {
        assumeNameBasedDestructuring(toolchain.getCompilerVersion())
        for (nameBasedDestructuringMode in NameBasedDestructuringMode.entries) {
            val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(nameBasedDestructuringMode),
                    toolchain.getCompilerVersion()
                )
            )

            assertEquals(
                nameBasedDestructuringMode,
                operation.compilerArguments[X_NAME_BASED_DESTRUCTURING]
            )
        }
    }

    @DisplayName("NameBasedDestructuringMode has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsNameBasedDestructuring() {
        assumeNameBasedDestructuring(toolchain.getCompilerVersion())
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_NAME_BASED_DESTRUCTURING])
        )
    }

    @DisplayName("Raw argument with non-existent NameBasedDestructuringMode value fails conversion")
    @Test
    fun testInvalidNameBasedDestructuringModeConversionFails() {
        assumeNameBasedDestructuring(toolchain.getCompilerVersion())
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value")
            )
            operation.compilerArguments[X_NAME_BASED_DESTRUCTURING]
        }
    }

    @DisplayName("NameBasedDestructuringMode of null value is converted to '-Xname-based-destructuring' argument")
    @Test
    fun testNullNameBasedDestructuringMode() {
        assumeNameBasedDestructuring(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_NAME_BASED_DESTRUCTURING] = null
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(null), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: NameBasedDestructuringMode?): String? = argument?.stringValue

    private fun assumeNameBasedDestructuring(compilerVersion: String) {
        assumeTrue(
            compilerVersion >= X_NAME_BASED_DESTRUCTURING.availableSinceVersion.toString(),
            "Test requires compiler version >= ${X_NAME_BASED_DESTRUCTURING.availableSinceVersion}"
        )
    }
}
