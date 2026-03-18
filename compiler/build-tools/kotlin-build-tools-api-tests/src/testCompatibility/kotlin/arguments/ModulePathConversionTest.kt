/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_MODULE_PATH
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class ModulePathConversionTest : BaseArgumentTest<List<Path>>("Xmodule-path") {

    @DisplayName("ModulePath is converted to '-Xmodule-path' argument")
    @BtaVersionsOnlyCompilationTest
    fun testModulePathToArgumentString(toolchain: KotlinToolchains) {
        val modulePaths = listOf(
            workingDirectory.resolve("path/to/module1"),
            workingDirectory.resolve("path/to/module2"),
            workingDirectory.resolve("path/to/module3")
        )
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_MODULE_PATH] = modulePaths
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(modulePaths), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xmodule-path' has the default value when ModulePath is not set")
    @BtaVersionsOnlyCompilationTest
    fun testModulePathNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("ModulePath can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testModulePathGetWhenSet(toolchain: KotlinToolchains) {
        val expectedModulePaths = listOf(
            workingDirectory.resolve("path/to/module1"),
            workingDirectory.resolve("path/to/module2"),
            workingDirectory.resolve("path/to/module3")
        )
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_MODULE_PATH] = expectedModulePaths
        }.build()

        val actualModulePaths = jvmOperation.compilerArguments[X_MODULE_PATH]

        assertEquals(expectedModulePaths, actualModulePaths)
    }

    @DisplayName("ModulePath has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testModulePathGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val modulePaths = jvmOperation.compilerArguments[X_MODULE_PATH]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(modulePaths)
        )
    }

    @DisplayName("Raw argument strings '-Xmodule-path=<paths>' are converted to ModulePath")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsModulePathConversion(toolchain: KotlinToolchains) {
        val expectedModulePaths = listOf(
            workingDirectory.resolve("path/to/module1"),
            workingDirectory.resolve("path/to/module2"),
            workingDirectory.resolve("path/to/module3")
        )
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(
                getValueString(expectedModulePaths),
                toolchain.getCompilerVersion()
            )
        )

        assertEquals(
            expectedModulePaths,
            operation.compilerArguments[X_MODULE_PATH]
        )
    }

    @DisplayName("ModulePath has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsModulePath(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_MODULE_PATH])
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: List<Path>?): String? =
        argument?.joinToString(File.pathSeparator) { it.toFile().absolutePath }
}
