/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_KLIB
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class KlibConversionTest : BaseArgumentTest<List<Path>>("Xklib") {

    @DisplayName("Klib is converted to '-Xklib' argument")
    @BtaVersionsOnlyCompilationTest
    fun testKlibToArgumentString(toolchain: KotlinToolchains) {
        val klibPaths = listOf(
            workingDirectory.resolve("path/to/lib1.klib"),
            workingDirectory.resolve("path/to/lib2.klib"),
            workingDirectory.resolve("path/to/lib3.klib")
        )
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_KLIB] = klibPaths
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(klibPaths), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xklib' has the default value when Klib is not set")
    @BtaVersionsOnlyCompilationTest
    fun testKlibNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("Klib can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testKlibGetWhenSet(toolchain: KotlinToolchains) {
        val expectedKlibPaths = listOf(
            workingDirectory.resolve("path/to/lib1.klib"),
            workingDirectory.resolve("path/to/lib2.klib"),
            workingDirectory.resolve("path/to/lib3.klib")
        )
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_KLIB] = expectedKlibPaths
        }.build()

        val actualKlibPaths = jvmOperation.compilerArguments[X_KLIB]

        assertEquals(expectedKlibPaths, actualKlibPaths)
    }

    @DisplayName("Klib has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testKlibGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val klibPaths = jvmOperation.compilerArguments[X_KLIB]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(klibPaths)
        )
    }

    @DisplayName("Raw argument strings '-Xklib=<paths>' are converted to Klib")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsKlibConversion(toolchain: KotlinToolchains) {
        val expectedKlibPaths = listOf(
            workingDirectory.resolve("path/to/lib1.klib"),
            workingDirectory.resolve("path/to/lib2.klib"),
            workingDirectory.resolve("path/to/lib3.klib")
        )
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(
                getValueString(expectedKlibPaths),
                toolchain.getCompilerVersion()
            )
        )

        assertEquals(
            expectedKlibPaths,
            operation.compilerArguments[X_KLIB]
        )
    }

    @DisplayName("Klib has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsKlib(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_KLIB])
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: List<Path>?): String? =
        argument?.joinToString(File.pathSeparator) { it.toFile().absolutePath }
}
