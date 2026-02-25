/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_FRIEND_PATHS
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import java.nio.file.Paths

@OptIn(ExperimentalCompilerArgument::class)
internal class FriendPathsConversionTest : BaseArgumentTest<List<Path>>("Xfriend-paths") {

    @DisplayName("FriendPaths is converted to '-Xfriend-paths' argument")
    @BtaVersionsOnlyCompilationTest
    fun testFriendPathsToArgumentString(toolchain: KotlinToolchains) {
        val friendPaths = listOf(
            workingDirectory.resolve("path/to/friend1"),
            workingDirectory.resolve("path/to/friend2"),
            workingDirectory.resolve("path/to/friend3")
        )
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_FRIEND_PATHS] = friendPaths
        }.build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getValueString(friendPaths), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("'-Xfriend-paths' has the default value when FriendPaths is not set")
    @BtaVersionsOnlyCompilationTest
    fun testFriendPathsNotSetByDefault(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val actualArgumentStrings = jvmOperation.compilerArguments.toArgumentStrings()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString(toolchain.getCompilerVersion()), toolchain.getCompilerVersion()),
            actualArgumentStrings,
        )
    }

    @DisplayName("FriendPaths can be set and retrieved")
    @BtaVersionsOnlyCompilationTest
    fun testFriendPathsGetWhenSet(toolchain: KotlinToolchains) {
        val expectedFriendPaths = listOf(
            workingDirectory.resolve("path/to/friend1"),
            workingDirectory.resolve("path/to/friend2"),
            workingDirectory.resolve("path/to/friend3")
        )
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[X_FRIEND_PATHS] = expectedFriendPaths
        }.build()

        val actualFriendPaths = jvmOperation.compilerArguments[X_FRIEND_PATHS]

        assertEquals(expectedFriendPaths, actualFriendPaths)
    }

    @DisplayName("FriendPaths has the default value when not set")
    @BtaVersionsOnlyCompilationTest
    fun testFriendPathsGetWhenNull(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        val friendPaths = jvmOperation.compilerArguments[X_FRIEND_PATHS]

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(friendPaths)
        )
    }

    @DisplayName("Raw argument strings '-Xfriend-paths=<paths>' are converted to FriendPaths")
    @BtaVersionsOnlyCompilationTest
    fun testRawArgumentsFriendPathsConversion(toolchain: KotlinToolchains) {
        val expectedFriendPaths = listOf(
            workingDirectory.resolve("path/to/friend1"),
            workingDirectory.resolve("path/to/friend2"),
            workingDirectory.resolve("path/to/friend3")
        )
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(
            expectedArgumentStringsFor(
                getValueString(expectedFriendPaths),
                toolchain.getCompilerVersion()
            )
        )

        assertEquals(
            expectedFriendPaths,
            operation.compilerArguments[X_FRIEND_PATHS]
        )
    }

    @DisplayName("FriendPaths has the default value when no raw arguments are applied")
    @BtaVersionsOnlyCompilationTest
    fun testNoRawArgumentsFriendPaths(toolchain: KotlinToolchains) {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get("."))

        operation.compilerArguments.applyArgumentStrings(listOf())

        assertEquals(
            getDefaultValueString(toolchain.getCompilerVersion()),
            getValueString(operation.compilerArguments[X_FRIEND_PATHS])
        )
    }

    override fun expectedArgumentStringsFor(value: String): List<String> {
        return listOf("-$argumentName=$value")
    }

    override fun getValueString(argument: List<Path>?): String? =
        argument?.joinToString(",") { it.toFile().absolutePath }
}
