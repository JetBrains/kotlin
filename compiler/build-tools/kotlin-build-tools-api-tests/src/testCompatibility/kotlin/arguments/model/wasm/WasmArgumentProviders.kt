/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.tests.arguments.model.wasm

import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.WasmCompilerKlibArguments
import org.jetbrains.kotlin.buildtools.api.arguments.WasmCompilerKlibArguments.Companion.X_WASM_TARGET
import org.jetbrains.kotlin.buildtools.api.arguments.WasmCompilerLinkingArguments
import org.jetbrains.kotlin.buildtools.api.arguments.WasmCompilerLinkingArguments.Companion.X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE
import org.jetbrains.kotlin.buildtools.api.arguments.WasmCompilerLinkingArguments.Companion.X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE
import org.jetbrains.kotlin.buildtools.api.arguments.enums.WasmTarget
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.arguments.model.wasm.WasmArgumentOperationKind.KLIB
import org.jetbrains.kotlin.buildtools.tests.arguments.model.wasm.WasmArgumentOperationKind.LINKING
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

internal class AllWasmCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration().map { Arguments.of(it) }.stream()
    }
}

internal class InvalidRawValueWasmCompilerArgumentsBtaV2StrategyAgnosticArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedInvalidRawValueBtaV2ArgumentConfigurations().map { Arguments.of(it) }.stream()
    }
}

internal class NullableWasmCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.runsNullableTest }.map { Arguments.of(it) }.stream()
    }
}

private fun namedInvalidRawValueBtaV2ArgumentConfigurations(): List<Named<Pair<WasmArgumentConfiguration<*>, CompilerExecutionStrategyConfiguration>>> {
    val btaV2Strategies = BtaV2StrategyAgnosticCompilationTestArgumentProvider.namedStrategyArguments()
    val compilerArguments = wasmCompilerArguments.filter { it.runsInvalidRawValueTest }.map {
        named("[${it.operationKind.displayName}][${it.argumentName}]", it)
    }

    return btaV2Strategies.flatMap { namedStrategy ->
        compilerArguments.map { namedArgumentDescriptor ->
            named(
                namedStrategy.name + namedArgumentDescriptor.name,
                WasmArgumentConfiguration(namedStrategy.payload.first, namedArgumentDescriptor.payload) to namedStrategy.payload
            )
        }
    }
}

private fun namedArgumentConfiguration(
    argumentPredicate: (WasmArgumentTestDescriptor<*>) -> Boolean = { true },
): List<Named<WasmArgumentConfiguration<*>>> {
    val btaVersions = BtaVersionsCompilationTestArgumentProvider.namedToolchainProviders()
    val compilerArguments = wasmCompilerArguments.filter { argumentPredicate(it) }.map {
        named("[${it.operationKind.displayName}][${it.argumentName}]", it)
    }

    return btaVersions.flatMap { namedKotlinToolchains ->
        compilerArguments.map { namedArgumentDescriptor ->
            named(
                namedKotlinToolchains.name + namedArgumentDescriptor.name,
                WasmArgumentConfiguration(namedKotlinToolchains.payload(), namedArgumentDescriptor.payload)
            )
        }
    }
}

private val testBaseDir: Path = Paths.get("").toAbsolutePath()

@OptIn(ExperimentalCompilerArgument::class)
private val wasmCompilerArguments: List<WasmArgumentTestDescriptor<*>> = listOf(
    WasmArgumentTestDescriptor(
        argumentName = "Xwasm-target",
        argument = X_WASM_TARGET,
        availableSinceVersion = X_WASM_TARGET.availableSinceVersion,
        operationKind = KLIB,
        argumentValues = WasmTarget.entries.toList(),
        argumentRawValues = WasmTarget.entries.map { it.stringValue },
        invalidRawValues = listOf("wasi"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-Xwasm-target=$value") },
        setArgumentValue = { value -> (this as WasmCompilerKlibArguments.Builder)[X_WASM_TARGET] = value },
        getArgumentValue = { (this as WasmCompilerKlibArguments)[X_WASM_TARGET] },
    ),
    WasmArgumentTestDescriptor(
        argumentName = "Xir-dce-dump-reachability-info-to-file",
        argument = X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE,
        availableSinceVersion = X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE.availableSinceVersion,
        operationKind = LINKING,
        argumentValues = listOf(testBaseDir.resolve("path/to/reachability.json")),
        argumentRawValues = listOf(testBaseDir.resolve("path/to/reachability.json").toFile().absolutePath),
        runsNullableTest = true,
        valueString = { value -> value?.toFile()?.absolutePath },
        expectedArgumentStringsFor = { value -> listOf("-Xir-dce-dump-reachability-info-to-file=$value") },
        setArgumentValue = { value -> (this as WasmCompilerLinkingArguments.Builder)[X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE] = value },
        getArgumentValue = { (this as WasmCompilerLinkingArguments)[X_IR_DCE_DUMP_REACHABILITY_INFO_TO_FILE] },
    ),
    WasmArgumentTestDescriptor(
        argumentName = "Xir-dump-declaration-ir-sizes-to-file",
        argument = X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE,
        availableSinceVersion = X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE.availableSinceVersion,
        operationKind = LINKING,
        argumentValues = listOf(testBaseDir.resolve("path/to/ir-sizes.txt")),
        argumentRawValues = listOf(testBaseDir.resolve("path/to/ir-sizes.txt").toFile().absolutePath),
        runsNullableTest = true,
        valueString = { value -> value?.toFile()?.absolutePath },
        expectedArgumentStringsFor = { value -> listOf("-Xir-dump-declaration-ir-sizes-to-file=$value") },
        setArgumentValue = { value -> (this as WasmCompilerLinkingArguments.Builder)[X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE] = value },
        getArgumentValue = { (this as WasmCompilerLinkingArguments)[X_IR_DUMP_DECLARATION_IR_SIZES_TO_FILE] },
    ),
)
