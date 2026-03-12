/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_MODULE_PATH
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

@OptIn(ExperimentalCompilerArgument::class)
internal class ModulePathConversionTest : BaseArgumentTest<String>("Xmodule-path") {

    @DisplayName("ModulePath is converted to '-Xmodule-path' argument")
    @Test
    fun testModulePathToArgumentString() {
        val modulePaths = modulePathStringOf(
            listOf(
                workingDirectory.resolve("path/to/module1"),
                workingDirectory.resolve("path/to/module2"),
                workingDirectory.resolve("path/to/module3")

            )
        )
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_MODULE_PATH] = modulePaths
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(modulePaths)),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xmodule-path' has the default value when ModulePath is not set")
    @Test
    fun testModulePathNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("ModulePath can be set and retrieved")
    @Test
    fun testModulePathGetWhenSet() {
        val expectedModulePaths = modulePathStringOf(
            listOf(
                workingDirectory.resolve("path/to/module1"),
                workingDirectory.resolve("path/to/module2"),
                workingDirectory.resolve("path/to/module3")

            )
        )
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_MODULE_PATH] = expectedModulePaths
        }

        val actualModulePaths = jvmOperation.compilerArguments[X_MODULE_PATH]

        assertEquals(expectedModulePaths, actualModulePaths)
    }

    @DisplayName("ModulePath has the default value when not set")
    @Test
    fun testModulePathGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val modulePaths = jvmOperation.compilerArguments[X_MODULE_PATH]

        assertEquals(
            getDefaultValueString(), getValueString(modulePaths)
        )
    }

    @DisplayName("Raw argument strings '-Xmodule-path=<paths>' are converted to ModulePath")
    @Test
    fun testRawArgumentsModulePathConversion() {
        val expectedModulePaths = modulePathStringOf(
            listOf(
                workingDirectory.resolve("path/to/module1"),
                workingDirectory.resolve("path/to/module2"),
                workingDirectory.resolve("path/to/module3")

            )
        )
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(getValueString(expectedModulePaths))
        )

        assertEquals(
            expectedModulePaths, operation.compilerArguments[X_MODULE_PATH]
        )
    }

    @DisplayName("ModulePath has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsModulePath() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[X_MODULE_PATH])
        )
    }

    @DisplayName("ModulePath of null value is converted to '-Xmodule-path' argument")
    @Test
    fun testNullModulePath() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_MODULE_PATH] = null
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

    override fun getValueString(argument: String?): String? = argument

    private fun modulePathStringOf(paths: List<Path>): String = paths.joinToString(File.pathSeparator) { it.absolutePathString() }
}
