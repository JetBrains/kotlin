/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.tests.arguments.model.commonjswasm

import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.IR_OUTPUT_DIR
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.LIBRARIES
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments.Companion.X_FRIEND_MODULES
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmCompilerLinkingArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmCompilerLinkingArguments.Companion.MAIN
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmCompilerLinkingArguments.Companion.SOURCE_MAP_BASE_DIRS
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmCompilerLinkingArguments.Companion.SOURCE_MAP_EMBED_SOURCES
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmCompilerLinkingArguments.Companion.SOURCE_MAP_NAMES_POLICY
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmCompilerLinkingArguments.Companion.X_CACHE_DIRECTORY
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmCompilerLinkingArguments.Companion.X_INCLUDE
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmCompilerLinkingArguments.Companion.X_IR_DCE_RUNTIME_DIAGNOSTIC
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsIrDiagnosticMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsMainCallMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.SourceMapEmbedSources
import org.jetbrains.kotlin.buildtools.api.arguments.enums.SourceMapNamesPolicy
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonjswasm.CommonJsAndWasmArgumentOperationKind.JS_KLIB
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonjswasm.CommonJsAndWasmArgumentOperationKind.JS_LINKING
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonjswasm.CommonJsAndWasmArgumentOperationKind.WASM_KLIB
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonjswasm.CommonJsAndWasmArgumentOperationKind.WASM_LINKING
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

internal class AllCommonJsAndWasmCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration().map { Arguments.of(it) }.stream()
    }
}

internal class InvalidArgumentValueCommonJsAndWasmCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.runsInvalidArgumentValueTest }.map { Arguments.of(it) }.stream()
    }
}

internal class InvalidRawValueCommonJsAndWasmCompilerArgumentsBtaV2StrategyAgnosticArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedInvalidRawValueBtaV2ArgumentConfigurations().map { Arguments.of(it) }.stream()
    }
}

internal class NullableCommonJsAndWasmCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.runsNullableTest }.map { Arguments.of(it) }.stream()
    }
}

private fun namedInvalidRawValueBtaV2ArgumentConfigurations(): List<Named<Pair<CommonJsAndWasmArgumentConfiguration<*>, CompilerExecutionStrategyConfiguration>>> {
    val btaV2Strategies = BtaV2StrategyAgnosticCompilationTestArgumentProvider.namedStrategyArguments()
    val compilerArguments = commonJsAndWasmCompilerArguments.filter { it.runsInvalidRawValueTest }

    return btaV2Strategies.flatMap { namedStrategy ->
        compilerArguments.flatMap { descriptor ->
            descriptor.operationKinds.map { operationKind ->
                named(
                    namedStrategy.name + "[${operationKind.displayName}][${descriptor.argumentName}]",
                    CommonJsAndWasmArgumentConfiguration(namedStrategy.payload.first, descriptor, operationKind) to namedStrategy.payload
                )
            }
        }
    }
}

private fun namedArgumentConfiguration(
    argumentPredicate: (CommonJsAndWasmArgumentTestDescriptor<*>) -> Boolean = { true },
): List<Named<CommonJsAndWasmArgumentConfiguration<*>>> {
    val btaVersions = BtaVersionsCompilationTestArgumentProvider.namedToolchainProviders()
    val compilerArguments = commonJsAndWasmCompilerArguments.filter { argumentPredicate(it) }

    return btaVersions.flatMap { namedKotlinToolchains ->
        compilerArguments.flatMap { descriptor ->
            descriptor.operationKinds.mapNotNull { operationKind ->
                named(
                    namedKotlinToolchains.name + "[${operationKind.displayName}][${descriptor.argumentName}]",
                    CommonJsAndWasmArgumentConfiguration(namedKotlinToolchains.payload(), descriptor, operationKind)
                )
            }
        }
    }
}

private val testBaseDir: Path = Paths.get("").toAbsolutePath()

@OptIn(ExperimentalCompilerArgument::class)
private val commonJsAndWasmCompilerArguments: List<CommonJsAndWasmArgumentTestDescriptor<*>> = listOf(
    CommonJsAndWasmArgumentTestDescriptor(
        argumentName = "ir-output-dir",
        argument = IR_OUTPUT_DIR,
        availableSinceVersion = IR_OUTPUT_DIR.availableSinceVersion,
        operationKinds = listOf(JS_KLIB, WASM_KLIB),
        argumentValues = listOf(testBaseDir.resolve("path/to/output")),
        argumentRawValues = listOf(testBaseDir.resolve("path/to/output").toFile().absolutePath),
        runsNullableTest = true,
        valueString = { value -> value?.toFile()?.absolutePath },
        expectedArgumentStringsFor = { value -> listOf("-ir-output-dir", value) },
        setArgumentValue = { value -> (this as CommonJsAndWasmArguments.Builder)[IR_OUTPUT_DIR] = value },
        getArgumentValue = { (this as CommonJsAndWasmArguments)[IR_OUTPUT_DIR] },
    ),
    CommonJsAndWasmArgumentTestDescriptor(
        argumentName = "libraries",
        argument = LIBRARIES,
        availableSinceVersion = LIBRARIES.availableSinceVersion,
        operationKinds = listOf(JS_KLIB, WASM_KLIB),
        argumentValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/lib1.klib"),
                testBaseDir.resolve("path/to/lib2.klib"),
            ),
            listOf(testBaseDir.resolve("path/with,comma/lib3.klib")),
        ),
        argumentRawValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/lib1.klib"),
                testBaseDir.resolve("path/to/lib2.klib"),
            ).joinToString(File.pathSeparator) { it.toFile().absolutePath },
            testBaseDir.resolve("path/with,comma/lib3.klib").toFile().absolutePath,
        ),
        invalidArgumentValues = listOf(listOf(testBaseDir.resolve("path/with${File.pathSeparator}separator/lib4.klib"))),
        runsNullableTest = true,
        valueString = { value -> value?.joinToString(File.pathSeparator) { it.toFile().absolutePath } },
        expectedArgumentStringsFor = { value -> listOf("-libraries", value) },
        setArgumentValue = { value -> (this as CommonJsAndWasmArguments.Builder)[LIBRARIES] = value },
        getArgumentValue = { (this as CommonJsAndWasmArguments)[LIBRARIES] },
    ),
    CommonJsAndWasmArgumentTestDescriptor(
        argumentName = "Xfriend-modules",
        argument = X_FRIEND_MODULES,
        availableSinceVersion = X_FRIEND_MODULES.availableSinceVersion,
        operationKinds = listOf(JS_KLIB, WASM_KLIB),
        argumentValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/friend1.klib"),
                testBaseDir.resolve("path/to/friend2.klib"),
            ),
            listOf(testBaseDir.resolve("path/with,comma/lib3.klib")),
        ),
        argumentRawValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/friend1.klib"),
                testBaseDir.resolve("path/to/friend2.klib"),
            ).joinToString(File.pathSeparator) { it.toFile().absolutePath },
            testBaseDir.resolve("path/with,comma/lib3.klib").toFile().absolutePath,
        ),
        invalidArgumentValues = listOf(listOf(testBaseDir.resolve("path/with${File.pathSeparator}separator.klib"))),
        runsNullableTest = true,
        valueString = { value -> value?.joinToString(File.pathSeparator) { it.toFile().absolutePath } },
        expectedArgumentStringsFor = { value -> listOf("-Xfriend-modules=$value") },
        setArgumentValue = { value -> (this as CommonJsAndWasmArguments.Builder)[X_FRIEND_MODULES] = value },
        getArgumentValue = { (this as CommonJsAndWasmArguments)[X_FRIEND_MODULES] },
    ),
    CommonJsAndWasmArgumentTestDescriptor(
        argumentName = "Xinclude",
        argument = X_INCLUDE,
        availableSinceVersion = X_INCLUDE.availableSinceVersion,
        operationKinds = listOf(JS_LINKING, WASM_LINKING),
        argumentValues = listOf(testBaseDir.resolve("path/to/included.klib")),
        argumentRawValues = listOf(testBaseDir.resolve("path/to/included.klib").toFile().absolutePath),
        runsNullableTest = true,
        valueString = { value -> value?.toFile()?.absolutePath },
        expectedArgumentStringsFor = { value -> listOf("-Xinclude=$value") },
        setArgumentValue = { value -> (this as CommonJsAndWasmCompilerLinkingArguments.Builder)[X_INCLUDE] = value },
        getArgumentValue = { (this as CommonJsAndWasmCompilerLinkingArguments)[X_INCLUDE] },
    ),
    CommonJsAndWasmArgumentTestDescriptor(
        argumentName = "Xcache-directory",
        argument = X_CACHE_DIRECTORY,
        availableSinceVersion = X_CACHE_DIRECTORY.availableSinceVersion,
        operationKinds = listOf(JS_LINKING, WASM_LINKING),
        argumentValues = listOf(testBaseDir.resolve("path/to/js-cache")),
        argumentRawValues = listOf(testBaseDir.resolve("path/to/js-cache").toFile().absolutePath),
        runsNullableTest = true,
        valueString = { value -> value?.toFile()?.absolutePath },
        expectedArgumentStringsFor = { value -> listOf("-Xcache-directory=$value") },
        setArgumentValue = { value -> (this as CommonJsAndWasmCompilerLinkingArguments.Builder)[X_CACHE_DIRECTORY] = value },
        getArgumentValue = { (this as CommonJsAndWasmCompilerLinkingArguments)[X_CACHE_DIRECTORY] },
    ),
    CommonJsAndWasmArgumentTestDescriptor(
        argumentName = "main",
        argument = MAIN,
        availableSinceVersion = MAIN.availableSinceVersion,
        operationKinds = listOf(JS_LINKING, WASM_LINKING),
        argumentValues = JsMainCallMode.entries.toList(),
        argumentRawValues = JsMainCallMode.entries.map { it.stringValue },
        invalidRawValues = listOf("non-existent-value"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-main", value) },
        setArgumentValue = { value -> (this as CommonJsAndWasmCompilerLinkingArguments.Builder)[MAIN] = value },
        getArgumentValue = { (this as CommonJsAndWasmCompilerLinkingArguments)[MAIN] },
    ),
    CommonJsAndWasmArgumentTestDescriptor(
        argumentName = "source-map-base-dirs",
        argument = SOURCE_MAP_BASE_DIRS,
        availableSinceVersion = SOURCE_MAP_BASE_DIRS.availableSinceVersion,
        operationKinds = listOf(JS_LINKING, WASM_LINKING),
        argumentValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/project/src/jsMain/kotlin"),
                testBaseDir.resolve("path/to/project/build/generated"),
            ),
            listOf(testBaseDir.resolve("path/with,comma")),
        ),
        argumentRawValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/project/src/jsMain/kotlin"),
                testBaseDir.resolve("path/to/project/build/generated"),
            ).joinToString(File.pathSeparator) { it.toFile().absolutePath },
            testBaseDir.resolve("path/with,comma").toFile().absolutePath,
        ),
        invalidArgumentValues = listOf(listOf(testBaseDir.resolve("path/with${File.pathSeparator}separator"))),
        runsNullableTest = true,
        valueString = { value -> value?.joinToString(File.pathSeparator) { it.toFile().absolutePath } },
        expectedArgumentStringsFor = { value -> listOf("-source-map-base-dirs", value) },
        setArgumentValue = { value -> (this as CommonJsAndWasmCompilerLinkingArguments.Builder)[SOURCE_MAP_BASE_DIRS] = value },
        getArgumentValue = { (this as CommonJsAndWasmCompilerLinkingArguments)[SOURCE_MAP_BASE_DIRS] },
    ),
    CommonJsAndWasmArgumentTestDescriptor(
        argumentName = "source-map-embed-sources",
        argument = SOURCE_MAP_EMBED_SOURCES,
        availableSinceVersion = SOURCE_MAP_EMBED_SOURCES.availableSinceVersion,
        operationKinds = listOf(JS_LINKING, WASM_LINKING),
        argumentValues = SourceMapEmbedSources.entries.toList(),
        argumentRawValues = SourceMapEmbedSources.entries.map { it.stringValue },
        invalidRawValues = listOf("non-existent-value"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-source-map-embed-sources", value) },
        setArgumentValue = { value -> (this as CommonJsAndWasmCompilerLinkingArguments.Builder)[SOURCE_MAP_EMBED_SOURCES] = value },
        getArgumentValue = { (this as CommonJsAndWasmCompilerLinkingArguments)[SOURCE_MAP_EMBED_SOURCES] },
    ),
    CommonJsAndWasmArgumentTestDescriptor(
        argumentName = "source-map-names-policy",
        argument = SOURCE_MAP_NAMES_POLICY,
        availableSinceVersion = SOURCE_MAP_NAMES_POLICY.availableSinceVersion,
        operationKinds = listOf(JS_LINKING, WASM_LINKING),
        argumentValues = SourceMapNamesPolicy.entries.toList(),
        argumentRawValues = SourceMapNamesPolicy.entries.map { it.stringValue },
        invalidRawValues = listOf("non-existent-value"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-source-map-names-policy", value) },
        setArgumentValue = { value -> (this as CommonJsAndWasmCompilerLinkingArguments.Builder)[SOURCE_MAP_NAMES_POLICY] = value },
        getArgumentValue = { (this as CommonJsAndWasmCompilerLinkingArguments)[SOURCE_MAP_NAMES_POLICY] },
    ),
    CommonJsAndWasmArgumentTestDescriptor(
        argumentName = "Xir-dce-runtime-diagnostic",
        argument = X_IR_DCE_RUNTIME_DIAGNOSTIC,
        availableSinceVersion = X_IR_DCE_RUNTIME_DIAGNOSTIC.availableSinceVersion,
        operationKinds = listOf(JS_LINKING, WASM_LINKING),
        argumentValues = JsIrDiagnosticMode.entries.toList(),
        argumentRawValues = JsIrDiagnosticMode.entries.map { it.stringValue },
        invalidRawValues = listOf("non-existent-value"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-Xir-dce-runtime-diagnostic=$value") },
        setArgumentValue = { value -> (this as CommonJsAndWasmCompilerLinkingArguments.Builder)[X_IR_DCE_RUNTIME_DIAGNOSTIC] = value },
        getArgumentValue = { (this as CommonJsAndWasmCompilerLinkingArguments)[X_IR_DCE_RUNTIME_DIAGNOSTIC] },
    ),
)
