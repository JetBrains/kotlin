/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.tests.arguments.model.konan

import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.NativeCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.NativeCompilerArguments.Companion.FRIEND_MODULES
import org.jetbrains.kotlin.buildtools.api.arguments.NativeCompilerArguments.Companion.LIBRARY
import org.jetbrains.kotlin.buildtools.api.arguments.NativeCompilerArguments.Companion.TARGET
import org.jetbrains.kotlin.buildtools.api.arguments.NativeCompilerKlibArguments
import org.jetbrains.kotlin.buildtools.api.arguments.NativeCompilerKlibArguments.Companion.MANIFEST
import org.jetbrains.kotlin.buildtools.api.arguments.NativeCompilerKlibArguments.Companion.NOPACK
import org.jetbrains.kotlin.buildtools.api.arguments.NativeCompilerKlibArguments.Companion.X_HEADER_KLIB_PATH
import org.jetbrains.kotlin.buildtools.api.arguments.NativeCompilerKlibArguments.Companion.X_MANIFEST_NATIVE_TARGETS
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KonanTarget
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTestArgumentProvider
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsCompilationTestArgumentProvider
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.support.ParameterDeclarations
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

internal class AllNativeCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration().map { Arguments.of(it) }.stream()
    }
}

internal class InvalidArgumentValueNativeCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.runsInvalidArgumentValueTest }.map { Arguments.of(it) }.stream()
    }
}

internal class NullableNativeCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.runsNullableTest }.map { Arguments.of(it) }.stream()
    }
}

internal class InvalidRawValueNativeCompilerArgumentsBtaV2StrategyAgnosticArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedInvalidRawValueBtaV2ArgumentConfigurations().map { Arguments.of(it) }.stream()
    }
}

private fun namedInvalidRawValueBtaV2ArgumentConfigurations(): List<Named<Pair<NativeArgumentConfiguration<*>, CompilerExecutionStrategyConfiguration>>> {
    val btaV2Strategies = BtaV2StrategyAgnosticCompilationTestArgumentProvider.namedStrategyArguments()
    val compilerArguments = nativeCompilerArguments.filter { it.runsInvalidRawValueTest }.map { named("[${it.argumentName}]", it) }

    return btaV2Strategies.flatMap { namedStrategy ->
        compilerArguments.map { namedArgumentDescriptor ->
            named(
                namedStrategy.name + namedArgumentDescriptor.name,
                NativeArgumentConfiguration(namedStrategy.payload.first, namedArgumentDescriptor.payload) to namedStrategy.payload
            )
        }
    }
}

private fun namedArgumentConfiguration(
    argumentPredicate: (NativeArgumentTestDescriptor<*>) -> Boolean = { true },
): List<Named<NativeArgumentConfiguration<*>>> {
    val btaVersions = BtaVersionsCompilationTestArgumentProvider.namedToolchainProviders()
    val compilerArguments = nativeCompilerArguments.filter { argumentPredicate(it) }.map { named("[${it.argumentName}]", it) }

    return btaVersions.flatMap { namedKotlinToolchains ->
        compilerArguments.map { namedArgumentDescriptor ->
            named(
                namedKotlinToolchains.name + namedArgumentDescriptor.name,
                NativeArgumentConfiguration(namedKotlinToolchains.payload(), namedArgumentDescriptor.payload)
            )
        }
    }
}

private val testBaseDir: Path = Paths.get("").toAbsolutePath()

private val libraries: List<Path> = listOf(
    testBaseDir.resolve("path/to/lib1.klib"),
    testBaseDir.resolve("path/to/lib2.klib"),
    testBaseDir.resolve("path/with,comma/lib3.klib"),
)

private val friendModules: List<Path> = listOf(
    testBaseDir.resolve("path/to/friend1.klib"),
    testBaseDir.resolve("path/to/friend2.klib"),
)

private fun List<Path>.joinToPathString(): String = joinToString(File.pathSeparator) { it.toFile().absolutePath }

private val nativeCompilerArguments: List<NativeArgumentTestDescriptor<*>> = listOf(
    NativeArgumentTestDescriptor(
        argumentName = "target",
        argument = TARGET,
        availableSinceVersion = TARGET.availableSinceVersion,
        argumentValues = KonanTarget.entries.toList(),
        argumentRawValues = KonanTarget.entries.map { it.stringValue },
        invalidRawValues = listOf("linux_x65"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-target", value) },
        setArgumentValue = { value -> (this as NativeCompilerArguments.Builder)[TARGET] = value },
        getArgumentValue = { (this as NativeCompilerArguments)[TARGET] },
    ),
    NativeArgumentTestDescriptor(
        argumentName = "Xmanifest-native-targets",
        argument = X_MANIFEST_NATIVE_TARGETS,
        availableSinceVersion = X_MANIFEST_NATIVE_TARGETS.availableSinceVersion,
        argumentValues = listOf(KonanTarget.entries.toList(), listOf(KonanTarget.LINUX_X64)),
        argumentRawValues = listOf(KonanTarget.entries.joinToString(",") { it.stringValue }, KonanTarget.LINUX_X64.stringValue),
        // KonanTargetListType (EnumListType) renders an empty list as the Kotlin literal "arrayOf()"
        // (see EnumListType.stringRepresentation in KotlinArgumentValueType.kt), which is what the model reports as this argument's default.
        // Render the empty value the same way so the default tests compare against the model-derived default.
        valueString = { value -> if (value.isNullOrEmpty()) "arrayOf()" else value.joinToString(",") { it.stringValue } },
        expectedArgumentStringsFor = { value -> listOf("-Xmanifest-native-targets=$value") },
        setArgumentValue = { value -> (this as NativeCompilerKlibArguments.Builder)[X_MANIFEST_NATIVE_TARGETS] = value },
        getArgumentValue = { (this as NativeCompilerKlibArguments)[X_MANIFEST_NATIVE_TARGETS] },
    ),
    NativeArgumentTestDescriptor(
        argumentName = "library",
        argument = LIBRARY,
        availableSinceVersion = LIBRARY.availableSinceVersion,
        argumentValues = listOf(libraries, libraries.take(1)),
        argumentRawValues = listOf(libraries.joinToPathString(), libraries.take(1).joinToPathString()),
        // The value string joins the libraries with the path separator only to be able to tell them apart in this test:
        // the argument has no delimiter, so every library is passed as a separate `-library` occurrence.
        valueString = { value -> value?.joinToPathString() },
        expectedArgumentStringsFor = { value -> value.split(File.pathSeparator).flatMap { listOf("-library", it) } },
        setArgumentValue = { value -> (this as NativeCompilerArguments.Builder)[LIBRARY] = value },
        getArgumentValue = { (this as NativeCompilerArguments)[LIBRARY] },
    ),
    NativeArgumentTestDescriptor(
        argumentName = "friend-modules",
        argument = FRIEND_MODULES,
        availableSinceVersion = FRIEND_MODULES.availableSinceVersion,
        argumentValues = listOf(friendModules, friendModules.take(1)),
        argumentRawValues = listOf(friendModules.joinToPathString(), friendModules.take(1).joinToPathString()),
        invalidArgumentValues = listOf(listOf(testBaseDir.resolve("path/with${File.pathSeparator}separator"))),
        runsNullableTest = true,
        valueString = { value -> value?.joinToPathString() },
        expectedArgumentStringsFor = { value -> listOf("-friend-modules", value) },
        setArgumentValue = { value -> (this as NativeCompilerArguments.Builder)[FRIEND_MODULES] = value },
        getArgumentValue = { (this as NativeCompilerArguments)[FRIEND_MODULES] },
    ),
    NativeArgumentTestDescriptor(
        argumentName = "manifest",
        argument = MANIFEST,
        availableSinceVersion = MANIFEST.availableSinceVersion,
        argumentValues = listOf(testBaseDir.resolve("path/to/manifest.properties")),
        argumentRawValues = listOf(testBaseDir.resolve("path/to/manifest.properties").toFile().absolutePath),
        runsNullableTest = true,
        valueString = { value -> value?.toFile()?.absolutePath },
        expectedArgumentStringsFor = { value -> listOf("-manifest", value) },
        setArgumentValue = { value -> (this as NativeCompilerKlibArguments.Builder)[MANIFEST] = value },
        getArgumentValue = { (this as NativeCompilerKlibArguments)[MANIFEST] },
    ),
    NativeArgumentTestDescriptor(
        argumentName = "Xheader-klib-path",
        argument = X_HEADER_KLIB_PATH,
        availableSinceVersion = X_HEADER_KLIB_PATH.availableSinceVersion,
        argumentValues = listOf(testBaseDir.resolve("path/to/header.klib")),
        argumentRawValues = listOf(testBaseDir.resolve("path/to/header.klib").toFile().absolutePath),
        runsNullableTest = true,
        valueString = { value -> value?.toFile()?.absolutePath },
        expectedArgumentStringsFor = { value -> listOf("-Xheader-klib-path=$value") },
        setArgumentValue = { value -> (this as NativeCompilerKlibArguments.Builder)[X_HEADER_KLIB_PATH] = value },
        getArgumentValue = { (this as NativeCompilerKlibArguments)[X_HEADER_KLIB_PATH] },
    ),
    NativeArgumentTestDescriptor(
        argumentName = "nopack",
        argument = NOPACK,
        availableSinceVersion = NOPACK.availableSinceVersion,
        argumentValues = listOf(true),
        argumentRawValues = listOf("true"),
        valueString = { value -> value?.toString() },
        expectedArgumentStringsFor = { _ -> listOf("-nopack") },
        setArgumentValue = { value -> (this as NativeCompilerKlibArguments.Builder)[NOPACK] = value },
        getArgumentValue = { (this as NativeCompilerKlibArguments)[NOPACK] },
    ),
)
