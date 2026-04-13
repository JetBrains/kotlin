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
@Suppress("DEPRECATION")
internal class ScriptResolverEnvironmentSpecialCharsTest {

    @DisplayName("ScriptResolverEnvironment entry with comma in value is converted to '-Xscript-resolver-environment' argument")
    @Test
    fun testScriptResolverEnvironmentWithCommaInValueGetWhenSet() {
        val entries = arrayOf("key=val1,val2")

        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT] = entries
        }

        assertEquals(
            expectedArgumentStringsFor(getValueString(entries)),
            jvmOperation.compilerArguments.toArgumentStrings(),
        )
    }

    @DisplayName("Raw argument string with comma in value is split on the comma")
    @Test
    fun testRawArgumentStringsWithCommaInValue() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor("key=val1,val2")
        )

        assertArrayEquals(
            arrayOf("key=val1", "val2"),
            operation.compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT]
        )
    }

    @DisplayName("ScriptResolverEnvironment entry with quote in value is converted to '-Xscript-resolver-environment' argument")
    @Test
    fun testScriptResolverEnvironmentWithQuoteInValueGetWhenSet() {
        val entries = arrayOf("key=\"quoted\"")

        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT] = entries
        }

        assertEquals(
            expectedArgumentStringsFor(getValueString(entries)),
            jvmOperation.compilerArguments.toArgumentStrings(),
        )
    }

    @DisplayName("Raw argument string with quoted value is parsed as a single entry with quotes preserved")
    @Test
    fun testRawArgumentStringsWithQuotedEntry() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor("key=\"quoted\"")
        )

        assertArrayEquals(
            arrayOf("key=\"quoted\""),
            operation.compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT]
        )
    }

    @DisplayName("ScriptResolverEnvironment entry with backslash in value is converted to '-Xscript-resolver-environment' argument")
    @Test
    fun testScriptResolverEnvironmentWithBackslashInValueGetWhenSet() {
        val entries = arrayOf("key=path\\to\\file")

        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT] = entries
        }

        assertEquals(
            expectedArgumentStringsFor(getValueString(entries)),
            jvmOperation.compilerArguments.toArgumentStrings(),
        )
    }

    @DisplayName("Raw argument string with backslash in value preserves the backslash")
    @Test
    fun testRawArgumentStringsWithBackslashInValue() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor("key=path\\to\\file")
        )

        assertArrayEquals(
            arrayOf("key=path\\to\\file"),
            operation.compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT]
        )
    }

    @DisplayName("ScriptResolverEnvironment entries with partially-quoted content are converted to '-Xscript-resolver-environment' argument")
    @Test
    fun testScriptResolverEnvironmentWithQuotedCommaEntriesGetWhenSet() {
        val entries = arrayOf("key=\"value1", "key2\"")

        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT] = entries
        }

        assertEquals(
            expectedArgumentStringsFor(getValueString(entries)),
            jvmOperation.compilerArguments.toArgumentStrings(),
        )
    }

    @DisplayName("Quoted value containing a comma is still split on the comma")
    @Test
    fun testRawArgumentStringsQuotedValueWithCommaIsSplit() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor("key=\"value1,key2\"")
        )

        assertArrayEquals(
            arrayOf("key=\"value1", "key2\""),
            operation.compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT]
        )
    }

    @DisplayName("ScriptResolverEnvironment entries where one has a trailing backslash are converted to '-Xscript-resolver-environment' argument")
    @Test
    fun testScriptResolverEnvironmentWithBackslashCommaEntriesGetWhenSet() {
        val entries = arrayOf("key=value1\\", "key2")

        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT] = entries
        }

        assertEquals(
            expectedArgumentStringsFor(getValueString(entries)),
            jvmOperation.compilerArguments.toArgumentStrings(),
        )
    }

    @DisplayName("Backslash before comma does not prevent splitting on the comma")
    @Test
    fun testRawArgumentStringsBackslashCommaIsSplit() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor("key=value1\\,key2")
        )

        assertArrayEquals(
            arrayOf("key=value1\\", "key2"),
            operation.compilerArguments[X_SCRIPT_RESOLVER_ENVIRONMENT]
        )
    }

    private fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-Xscript-resolver-environment=$value")
    }

    private fun getValueString(argument: Array<String>): String =
        argument.joinToString(",")
}
