/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_KLIB
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
internal class KlibConversionTest : BaseArgumentTest<String>("Xklib") {

    @DisplayName("Klib is converted to '-Xklib' argument")
    @Test
    fun testKlibToArgumentString() {
        val klibPaths = klibStringOf(
            listOf(
                workingDirectory.resolve("path/to/lib1.klib"),
                workingDirectory.resolve("path/to/lib2.klib"),
                workingDirectory.resolve("path/to/lib3.klib")
            )
        )
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_KLIB] = klibPaths
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(klibPaths)),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xklib' has the default value when Klib is not set")
    @Test
    fun testKlibNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("Klib can be set and retrieved")
    @Test
    fun testKlibGetWhenSet() {
        val expectedKlibPaths = klibStringOf(
            listOf(
                workingDirectory.resolve("path/to/lib1.klib"),
                workingDirectory.resolve("path/to/lib2.klib"),
                workingDirectory.resolve("path/to/lib3.klib")
            )
        )
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_KLIB] = expectedKlibPaths
        }

        val actualKlibPaths = jvmOperation.compilerArguments[X_KLIB]

        assertEquals(expectedKlibPaths, actualKlibPaths)
    }

    @DisplayName("Klib has the default value when not set")
    @Test
    fun testKlibGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val klibPaths = jvmOperation.compilerArguments[X_KLIB]

        assertEquals(
            getDefaultValueString(), getValueString(klibPaths)
        )
    }

    @DisplayName("Raw argument strings '-Xklib=<paths>' are converted to Klib")
    @Test
    fun testRawArgumentsKlibConversion() {
        val expectedKlibPaths = klibStringOf(
            listOf(
                workingDirectory.resolve("path/to/lib1.klib"),
                workingDirectory.resolve("path/to/lib2.klib"),
                workingDirectory.resolve("path/to/lib3.klib")
            )
        )
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(getValueString(expectedKlibPaths))
        )

        assertEquals(
            expectedKlibPaths, operation.compilerArguments[X_KLIB]
        )
    }

    @DisplayName("Klib has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsKlib() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[X_KLIB])
        )
    }

    @DisplayName("Klib of null value is converted to '-Xklib' argument")
    @Test
    fun testNullKlib() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_KLIB] = null
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

    private fun klibStringOf(paths: List<Path>): String = paths.joinToString(File.pathSeparator) { it.absolutePathString() }
}
