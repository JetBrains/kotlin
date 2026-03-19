/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_SCRIPT_RESOLVER_ENVIRONMENT
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class ScriptResolverEnvironmentConversionTest : BaseArgumentTest<List<String>>("Xscript-resolver-environment") {

    @DisplayName("ScriptResolverEnvironment is converted to '-Xscript-resolver-environment' argument")
    @BtaVersionsOnlyCompilationTest
    fun testScriptResolverEnvironmentToArgumentString(toolchain: KotlinToolchains) {
        assumeArgumentSupported(toolchain.getCompilerVersion())
        val entries = listOf("key1=value1", "key2=value2")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT] = entries
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(entries), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xscript-resolver-environment' has the default value when ScriptResolverEnvironment is not set")
    @BtaVersionsOnlyCompilationTest
    fun testScriptResolverEnvironmentNotSetByDefault(toolchain: KotlinToolchains) {
        assumeArgumentSupported(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("ScriptResolverEnvironment can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testScriptResolverEnvironmentGetWhenSet(toolchain: KotlinToolchains) {
        assumeArgumentSupported(toolchain.getCompilerVersion())
        val expectedEntries = listOf("key1=value1", "key2=value2")
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT] = expectedEntries
        }.build()

        val actualEntries = jvmOperation.compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT]

        assertEquals(expectedEntries, actualEntries)
    }

    @DisplayName("ScriptResolverEnvironment has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testScriptResolverEnvironmentGetWhenNull(toolchain: KotlinToolchains) {
        assumeArgumentSupported(toolchain.getCompilerVersion())
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val entries = jvmOperation.compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(entries)
        )
    }

    @DisplayName("Raw argument strings '-Xscript-resolver-environment=<value>' are converted to ScriptResolverEnvironment")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsScriptResolverEnvironmentConversion(toolchain: KotlinToolchains) {
        assumeArgumentSupported(toolchain.getCompilerVersion())
        val expectedEntries = listOf("key1=value1", "key2=value2")
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(
                getValueString(expectedEntries),
                toolchain.getCompilerVersion()
            )
        )

        assertEquals(
            expectedEntries,
            operation.compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT]
        )
    }

    @DisplayName("ScriptResolverEnvironment has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsScriptResolverEnvironment(toolchain: KotlinToolchains) {
        assumeArgumentSupported(toolchain.getCompilerVersion())
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT])
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: List<String>?): String? =
        argument?.joinToString(",")

    private fun assumeArgumentSupported(compilerVersion: String) {
        assumeTrue(
            compilerVersion >= X_SCRIPT_RESOLVER_ENVIRONMENT.availableSinceVersion.toString(),
            "Test requires compiler version >= ${X_SCRIPT_RESOLVER_ENVIRONMENT.availableSinceVersion}"
        )
    }
}
