/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(DeprecatedCompilerArgument::class, ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.tests.arguments.model.commonklib

import org.jetbrains.kotlin.buildtools.api.DeprecatedCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.CommonKlibBasedArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonKlibBasedArguments.Companion.X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY
import org.jetbrains.kotlin.buildtools.api.arguments.CommonKlibBasedArgumentsKlibArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonKlibBasedArgumentsKlibArguments.Companion.X_KLIB_RELATIVE_PATH_BASE
import org.jetbrains.kotlin.buildtools.api.arguments.CommonKlibBasedArgumentsLinkingArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonKlibBasedArgumentsLinkingArguments.Companion.X_PARTIAL_LINKAGE
import org.jetbrains.kotlin.buildtools.api.arguments.CommonKlibBasedArgumentsLinkingArguments.Companion.X_PARTIAL_LINKAGE_LOGLEVEL
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.enums.DuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.buildtools.api.arguments.enums.PartialLinkageLogLevel
import org.jetbrains.kotlin.buildtools.api.arguments.enums.PartialLinkageMode
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonklib.CommonKlibArgumentOperationKind.JS_KLIB
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonklib.CommonKlibArgumentOperationKind.JS_LINKING
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonklib.CommonKlibArgumentOperationKind.WASM_KLIB
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonklib.CommonKlibArgumentOperationKind.WASM_LINKING
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTestArgumentProvider
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsCompilationTestArgumentProvider
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.support.ParameterDeclarations
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

internal class AllCommonKlibCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration().map { Arguments.of(it) }.stream()
    }
}

internal class InvalidArgumentValueCommonKlibCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.runsInvalidArgumentValueTest }.map { Arguments.of(it) }.stream()
    }
}

internal class InvalidRawValueCommonKlibCompilerArgumentsBtaV2StrategyAgnosticArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedInvalidRawValueBtaV2ArgumentConfigurations().map { Arguments.of(it) }.stream()
    }
}

internal class NullableCommonKlibCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.runsNullableTest }.map { Arguments.of(it) }.stream()
    }
}

private fun namedInvalidRawValueBtaV2ArgumentConfigurations(): List<Named<Pair<CommonKlibArgumentConfiguration<*>, CompilerExecutionStrategyConfiguration>>> {
    val btaV2Strategies = BtaV2StrategyAgnosticCompilationTestArgumentProvider.namedStrategyArguments()
    val compilerArguments = commonKlibCompilerArguments.filter { it.runsInvalidRawValueTest }

    return btaV2Strategies.flatMap { namedStrategy ->
        compilerArguments.flatMap { descriptor ->
            descriptor.operationKinds.map { operationKind ->
                named(
                    namedStrategy.name + "[${operationKind.displayName}][${descriptor.argumentName}]",
                    CommonKlibArgumentConfiguration(namedStrategy.payload.first, descriptor, operationKind) to namedStrategy.payload
                )
            }
        }
    }
}

private fun namedArgumentConfiguration(
    argumentPredicate: (CommonKlibArgumentTestDescriptor<*>) -> Boolean = { true },
): List<Named<CommonKlibArgumentConfiguration<*>>> {
    val btaVersions = BtaVersionsCompilationTestArgumentProvider.namedToolchainProviders()
    val compilerArguments = commonKlibCompilerArguments.filter { argumentPredicate(it) }

    return btaVersions.flatMap { namedKotlinToolchains ->
        compilerArguments.flatMap { descriptor ->
            descriptor.operationKinds.mapNotNull { operationKind ->
                named(
                    namedKotlinToolchains.name + "[${operationKind.displayName}][${descriptor.argumentName}]",
                    CommonKlibArgumentConfiguration(namedKotlinToolchains.payload(), descriptor, operationKind)
                )
            }
        }
    }
}

private val testBaseDir: Path = Paths.get("").toAbsolutePath()

@OptIn(ExperimentalCompilerArgument::class)
private val commonKlibCompilerArguments: List<CommonKlibArgumentTestDescriptor<*>> = listOf(
    CommonKlibArgumentTestDescriptor(
        argumentName = "Xklib-relative-path-base",
        argument = X_KLIB_RELATIVE_PATH_BASE,
        availableSinceVersion = X_KLIB_RELATIVE_PATH_BASE.availableSinceVersion,
        operationKinds = listOf(JS_KLIB, WASM_KLIB),
        argumentValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/project/src/commonMain/kotlin"),
                testBaseDir.resolve("path/to/generated/src"),
            )
        ),
        argumentRawValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/project/src/commonMain/kotlin"),
                testBaseDir.resolve("path/to/generated/src"),
            ).joinToString(",") { it.toFile().absolutePath }
        ),
        invalidArgumentValues = listOf(listOf(testBaseDir.resolve("path/with,comma"))),
        valueString = { value -> value?.joinToString(",") { it.toFile().absolutePath } },
        expectedArgumentStringsFor = { value -> listOf("-Xklib-relative-path-base=$value") },
        setArgumentValue = { value -> (this as CommonKlibBasedArgumentsKlibArguments.Builder)[X_KLIB_RELATIVE_PATH_BASE] = value },
        getArgumentValue = { (this as CommonKlibBasedArgumentsKlibArguments)[X_KLIB_RELATIVE_PATH_BASE] },
    ),
    CommonKlibArgumentTestDescriptor(
        argumentName = "Xpartial-linkage",
        argument = X_PARTIAL_LINKAGE,
        availableSinceVersion = X_PARTIAL_LINKAGE.availableSinceVersion,
        operationKinds = listOf(JS_LINKING, WASM_LINKING),
        argumentValues = PartialLinkageMode.entries.toList(),
        argumentRawValues = PartialLinkageMode.entries.map { it.stringValue },
        invalidRawValues = listOf("disabled"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-Xpartial-linkage=$value") },
        setArgumentValue = { value -> (this as CommonKlibBasedArgumentsLinkingArguments.Builder)[X_PARTIAL_LINKAGE] = value },
        getArgumentValue = { (this as CommonKlibBasedArgumentsLinkingArguments)[X_PARTIAL_LINKAGE] },
    ),
    CommonKlibArgumentTestDescriptor(
        argumentName = "Xpartial-linkage-loglevel",
        argument = X_PARTIAL_LINKAGE_LOGLEVEL,
        availableSinceVersion = X_PARTIAL_LINKAGE_LOGLEVEL.availableSinceVersion,
        operationKinds = listOf(JS_LINKING, WASM_LINKING),
        argumentValues = PartialLinkageLogLevel.entries.toList(),
        argumentRawValues = PartialLinkageLogLevel.entries.map { it.stringValue },
        invalidRawValues = listOf("warn"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-Xpartial-linkage-loglevel=$value") },
        setArgumentValue = { value -> (this as CommonKlibBasedArgumentsLinkingArguments.Builder)[X_PARTIAL_LINKAGE_LOGLEVEL] = value },
        getArgumentValue = { (this as CommonKlibBasedArgumentsLinkingArguments)[X_PARTIAL_LINKAGE_LOGLEVEL] },
    ),
    CommonKlibArgumentTestDescriptor(
        argumentName = "Xklib-duplicated-unique-name-strategy",
        argument = X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY,
        availableSinceVersion = X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY.availableSinceVersion,
        operationKinds = listOf(JS_KLIB, WASM_KLIB, JS_LINKING, WASM_LINKING),
        argumentValues = DuplicatedUniqueNameStrategy.entries.toList(),
        argumentRawValues = DuplicatedUniqueNameStrategy.entries.map { it.stringValue },
        invalidRawValues = listOf("allow-second-with-warning"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-Xklib-duplicated-unique-name-strategy=$value") },
        setArgumentValue = { value -> (this as CommonKlibBasedArguments.Builder)[X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY] = value },
        getArgumentValue = { (this as CommonKlibBasedArguments)[X_KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY] },
    ),
)
