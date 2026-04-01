/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_FRIEND_PATHS
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

@OptIn(ExperimentalCompilerArgument::class)
internal class FriendPathsConversionTest : BaseArgumentTest<Array<String>>("Xfriend-paths") {

    @DisplayName("FriendPaths is converted to '-Xfriend-paths' argument")
    @Test
    fun testFriendPathsToArgumentString() {
        val friendPaths = arrayOf(
            workingDirectory.resolve("path/to/friend1").absolutePathString(),
            workingDirectory.resolve("path/to/friend2").absolutePathString(),
            workingDirectory.resolve("path/to/friend3").absolutePathString()
        )
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_FRIEND_PATHS] = friendPaths
        }

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(friendPaths)),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xfriend-paths' has the default value when FriendPaths is not set")
    @Test
    fun testFriendPathsNotSetByDefault() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            actualArgumentStrings,
        )
    }

    @DisplayName("FriendPaths can be set and retrieved")
    @Test
    fun testFriendPathsGetWhenSet() {
        val expectedFriendPaths = arrayOf(
            workingDirectory.resolve("path/to/friend1").absolutePathString(),
            workingDirectory.resolve("path/to/friend2").absolutePathString(),
            workingDirectory.resolve("path/to/friend3").absolutePathString()
        )
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_FRIEND_PATHS] = expectedFriendPaths
        }

        val actualFriendPaths = jvmOperation.compilerArguments[X_FRIEND_PATHS]

        assertArrayEquals(expectedFriendPaths, actualFriendPaths)
    }

    @DisplayName("FriendPaths has the default value when not set")
    @Test
    fun testFriendPathsGetWhenNull() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        val friendPaths = jvmOperation.compilerArguments[X_FRIEND_PATHS]

        assertEquals(
            getDefaultValueString(), getValueString(friendPaths)
        )
    }

    @DisplayName("Raw argument strings '-Xfriend-paths=<paths>' are converted to FriendPaths")
    @Test
    fun testRawArgumentsFriendPathsConversion() {
        val expectedFriendPaths = arrayOf(
            workingDirectory.resolve("path/to/friend1").absolutePathString(),
            workingDirectory.resolve("path/to/friend2").absolutePathString(),
            workingDirectory.resolve("path/to/friend3").absolutePathString()
        )
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(getValueString(expectedFriendPaths))
        )

        assertArrayEquals(
            expectedFriendPaths, operation.compilerArguments[X_FRIEND_PATHS]
        )
    }

    @DisplayName("FriendPaths has the default value when no raw arguments are applied")
    @Test
    fun testNoRawArgumentsFriendPaths() {
        val operation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[X_FRIEND_PATHS])
        )
    }

    @DisplayName("FriendPaths of null value is converted to '-Xfriend-paths' argument")
    @Test
    fun testNullFriendPaths() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_FRIEND_PATHS] = null
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
