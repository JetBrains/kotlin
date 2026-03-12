/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_JDK_RELEASE
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JdkRelease
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class JdkReleaseConversionTest : BaseArgumentTest<JdkRelease>("Xjdk-release") {

    @DisplayName("JdkRelease is converted to '-Xjdk-release' argument")
    @BtaVersionsOnlyCompilationTest
    fun testJdkReleaseToArgumentString(toolchain: KotlinToolchains) {
        for (jdkRelease in JdkRelease.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_JDK_RELEASE] = jdkRelease
            }.build()

            val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

            assertEquals(
                expectedArgumentStringsFor(getValueString(jdkRelease), toolchain.getCompilerVersion()),
                actualArgumentStrings,
            )
        }
    }

    @DisplayName("'-Xjdk-release' has the default value when JdkRelease is not set")
    @BtaVersionsOnlyCompilationTest
    fun testJdkReleaseNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("JdkRelease can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testJdkReleaseGetWhenSet(toolchain: KotlinToolchains) {
        for (jdkRelease in JdkRelease.entries) {
            val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[X_JDK_RELEASE] = jdkRelease
            }.build()

            val actualJdkRelease = jvmOperation.compilerArguments[X_JDK_RELEASE]

            assertEquals(jdkRelease, actualJdkRelease)
        }
    }

    @DisplayName("JdkRelease has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testJdkReleaseGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val jdkRelease = jvmOperation.compilerArguments[X_JDK_RELEASE]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(jdkRelease)
        )
    }

    @DisplayName("Raw argument strings '-Xjdk-release <value>' are converted to JdkRelease")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsJdkReleaseConversion(toolchain: KotlinToolchains) {
        for (jdkRelease in JdkRelease.entries) {
            val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor(
                    getValueString(jdkRelease),
                    toolchain.getCompilerVersion()
                )
            )

            assertEquals(
                jdkRelease,
                operation.compilerArguments[X_JDK_RELEASE]
            )
        }
    }

    @DisplayName("JdkRelease has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsJdkRelease(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_JDK_RELEASE])
        )
    }

    @DisplayName("Raw argument with non-existent JdkRelease value fails conversion")
    @BtaVersionsOnlyCompilationTest
    fun testInvalidJdkReleaseConversionFails(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        val exception = assertThrows<CompilerArgumentsParseException> {
            operation.compilerArguments.applyArgumentStrings(
                expectedArgumentStringsFor("non-existent-value")
            )
            operation.compilerArguments[X_JDK_RELEASE]
        }

        assertEquals("Unknown -Xjdk-release value: non-existent-value", exception.message)
    }

    @DisplayName("JdkRelease of null value is converted to '-Xjdk-release' argument")
    @BtaVersionsOnlyCompilationTest
    fun testNullJdkRelease(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_JDK_RELEASE] = null
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

    override fun getValueString(argument: JdkRelease?): String? = argument?.stringValue
}
