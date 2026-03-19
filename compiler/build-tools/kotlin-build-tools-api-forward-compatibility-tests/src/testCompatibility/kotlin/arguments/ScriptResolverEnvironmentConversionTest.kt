/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_SCRIPT_RESOLVER_ENVIRONMENT
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class ScriptResolverEnvironmentConversionTest : BaseArgumentTest<Array<String>>("Xscript-resolver-environment") {

    @DisplayName("ScriptResolverEnvironment is converted to '-Xscript-resolver-environment' argument")
    @Test
    fun testScriptResolverEnvironmentToArgumentString() {
        val entries = arrayOf("key1=value1", "key2=value2")
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT] = entries
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(entries)),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xscript-resolver-environment' has the default value when ScriptResolverEnvironment is not set")
    @Test
    fun testScriptResolverEnvironmentNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("ScriptResolverEnvironment can be set and retrieved")
    @Test
    fun testScriptResolverEnvironmentGetWhenSet() {
        val expectedEntries = arrayOf("key1=value1", "key2=value2")
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT] = expectedEntries
        }

        val actualEntries = jvmOperation.compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT]

        assertArrayEquals(expectedEntries, actualEntries)
    }

    @DisplayName("ScriptResolverEnvironment has the default value when not set")
    @Test
    fun testScriptResolverEnvironmentGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val entries = jvmOperation.compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT]

        assertEquals(
            getDefaultValueString(), getValueString(entries)
        )
    }

    @DisplayName("Raw argument strings '-Xscript-resolver-environment=<value>' are converted to ScriptResolverEnvironment")
    @Test
    fun testRawArgumentsScriptResolverEnvironmentConversion() {
        val expectedEntries = arrayOf("key1=value1", "key2=value2")
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(getValueString(expectedEntries))
        )

        assertArrayEquals(
            expectedEntries, operation.compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT]
        )
    }

    @DisplayName("ScriptResolverEnvironment has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsScriptResolverEnvironment() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT])
        )
    }

    @DisplayName("ScriptResolverEnvironment of null value is converted to '-Xscript-resolver-environment' argument")
    @Test
    fun testNullScriptResolverEnvironment() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT] = null
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
