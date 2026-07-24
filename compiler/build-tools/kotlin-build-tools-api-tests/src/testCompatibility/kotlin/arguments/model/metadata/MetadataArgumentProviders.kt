/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.tests.arguments.model.metadata

import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments.Companion.CLASSPATH
import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments.Companion.MODULE_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments.Companion.X_FRIEND_PATHS
import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments.Companion.X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT
import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments.Companion.X_REFINES_PATHS
import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments.Companion.X_TARGET_PLATFORM
import org.jetbrains.kotlin.buildtools.api.arguments.enums.MetadataTargetPlatform
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

internal class AllMetadataCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration().map { Arguments.of(it) }.stream()
    }
}

internal class InvalidArgumentValueMetadataCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.runsInvalidArgumentValueTest }.map { Arguments.of(it) }.stream()
    }
}

internal class NullableMetadataCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.runsNullableTest }.map { Arguments.of(it) }.stream()
    }
}

internal class InvalidRawValueMetadataCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.runsInvalidRawValueTest }.map { Arguments.of(it) }.stream()
    }
}

internal class InvalidRawValueMetadataCompilerArgumentsBtaV2StrategyAgnosticArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedInvalidRawValueBtaV2ArgumentConfigurations().map { Arguments.of(it) }.stream()
    }
}

private fun namedInvalidRawValueBtaV2ArgumentConfigurations(): List<Named<Pair<MetadataArgumentConfiguration<*>, CompilerExecutionStrategyConfiguration>>> {
    val btaV2Strategies = BtaV2StrategyAgnosticCompilationTestArgumentProvider.namedStrategyArguments()
    val compilerArguments = metadataCompilerArguments.filter { it.runsInvalidRawValueTest }

    return btaV2Strategies.flatMap { namedStrategy ->
        compilerArguments.map { descriptor ->
            named(
                namedStrategy.name + "[${descriptor.argumentName}]",
                MetadataArgumentConfiguration(namedStrategy.payload.first, descriptor) to namedStrategy.payload
            )
        }
    }
}

private fun namedArgumentConfiguration(
    argumentPredicate: (MetadataArgumentTestDescriptor<*>) -> Boolean = { true },
): List<Named<MetadataArgumentConfiguration<*>>> {
    val btaVersions = BtaVersionsCompilationTestArgumentProvider.namedToolchainProviders()
    val compilerArguments = metadataCompilerArguments.filter { argumentPredicate(it) }.map { named("[${it.argumentName}]", it) }

    return btaVersions.flatMap { namedKotlinToolchains ->
        compilerArguments.map { namedArgumentDescriptor ->
            named(
                namedKotlinToolchains.name + namedArgumentDescriptor.name,
                MetadataArgumentConfiguration(namedKotlinToolchains.payload(), namedArgumentDescriptor.payload)
            )
        }
    }
}

private val testBaseDir: Path = Paths.get("").toAbsolutePath()

private val metadataCompilerArguments: List<MetadataArgumentTestDescriptor<*>> = listOf(
    MetadataArgumentTestDescriptor(
        argumentName = "classpath",
        argument = CLASSPATH,
        argumentValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/lib1.jar"),
                testBaseDir.resolve("path/to/lib2.jar"),
                testBaseDir.resolve("path/to/classes"),
            ),
            listOf(testBaseDir.resolve("path/with,comma/lib3.jar")),
        ),
        argumentRawValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/lib1.jar"),
                testBaseDir.resolve("path/to/lib2.jar"),
                testBaseDir.resolve("path/to/classes"),
            ).joinToString(File.pathSeparator) { it.toFile().absolutePath },
            testBaseDir.resolve("path/with,comma/lib3.jar").toFile().absolutePath,
        ),
        invalidArgumentValues = listOf(listOf(testBaseDir.resolve("path/with${File.pathSeparator}separator"))),
        runsNullableTest = true,
        valueString = { value -> value?.joinToString(File.pathSeparator) { it.toFile().absolutePath } },
        expectedArgumentStringsFor = { value -> listOf("-classpath", value) },
    ),
    MetadataArgumentTestDescriptor(
        argumentName = "Xfriend-paths",
        argument = X_FRIEND_PATHS,
        argumentValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/friend1"),
                testBaseDir.resolve("path/to/friend2"),
            )
        ),
        argumentRawValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/friend1"),
                testBaseDir.resolve("path/to/friend2"),
            ).joinToString(",") { it.toFile().absolutePath }
        ),
        invalidArgumentValues = listOf(listOf(testBaseDir.resolve("path/with,comma"))),
        valueString = { value -> value?.joinToString(",") { it.toFile().absolutePath } },
        expectedArgumentStringsFor = { value -> listOf("-Xfriend-paths=$value") },
    ),
    MetadataArgumentTestDescriptor(
        argumentName = "Xrefines-paths",
        argument = X_REFINES_PATHS,
        argumentValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/refines1"),
                testBaseDir.resolve("path/to/refines2"),
            )
        ),
        argumentRawValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/refines1"),
                testBaseDir.resolve("path/to/refines2"),
            ).joinToString(",") { it.toFile().absolutePath }
        ),
        invalidArgumentValues = listOf(listOf(testBaseDir.resolve("path/with,comma"))),
        valueString = { value -> value?.joinToString(",") { it.toFile().absolutePath } },
        expectedArgumentStringsFor = { value -> listOf("-Xrefines-paths=$value") },
    ),
    MetadataArgumentTestDescriptor(
        argumentName = "Xtarget-platform",
        argument = X_TARGET_PLATFORM,
        argumentValues = listOf(MetadataTargetPlatform.entries.toList()),
        argumentRawValues = listOf(MetadataTargetPlatform.entries.joinToString(",") { it.stringValue }),
        invalidRawValues = listOf("WasmWas"),
        // MetadataTargetPlatformType (EnumListType) renders an empty list as the Kotlin literal "arrayOf()"
        // (see EnumListType.stringRepresentation in KotlinArgumentValueType.kt), which is what the model reports as this argument's default.
        // Render the empty value the same way so the default tests compare against the model-derived default.
        valueString = { value -> if (value.isNullOrEmpty()) "arrayOf()" else value.joinToString(",") { it.stringValue } },
        expectedArgumentStringsFor = { value -> listOf("-Xtarget-platform=$value") },
    ),
    MetadataArgumentTestDescriptor(
        argumentName = "module-name",
        argument = MODULE_NAME,
        argumentValues = listOf("my-metadata-module"),
        argumentRawValues = listOf("my-metadata-module"),
        runsNullableTest = true,
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> listOf("-module-name", value) },
    ),
    MetadataArgumentTestDescriptor(
        argumentName = "Xklib-zip-file-accessor-cache-limit",
        argument = X_KLIB_ZIP_FILE_ACCESSOR_CACHE_LIMIT,
        argumentValues = listOf(128),
        argumentRawValues = listOf("128"),
        valueString = { value -> value?.toString() },
        expectedArgumentStringsFor = { value -> listOf("-Xklib-zip-file-accessor-cache-limit=$value") },
    ),
)
