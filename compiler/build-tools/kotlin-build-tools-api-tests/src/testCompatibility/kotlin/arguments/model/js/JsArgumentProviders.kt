/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.tests.arguments.model.js

import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerLinkingArguments
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerLinkingArguments.Companion.MODULE_KIND
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerLinkingArguments.Companion.TARGET
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerLinkingArguments.Companion.X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsEcmaVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsIrDiagnosticMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsModuleKind
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTestArgumentProvider
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsCompilationTestArgumentProvider
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.support.ParameterDeclarations
import java.util.stream.Stream

internal class AllJsCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration().map { Arguments.of(it) }.stream()
    }
}

internal class InvalidRawValueJsCompilerArgumentsBtaV2StrategyAgnosticArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedInvalidRawValueBtaV2ArgumentConfigurations().map { Arguments.of(it) }.stream()
    }
}

internal class NullableJsCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.runsNullableTest }.map { Arguments.of(it) }.stream()
    }
}

private fun namedInvalidRawValueBtaV2ArgumentConfigurations(): List<Named<Pair<JsArgumentConfiguration<*>, CompilerExecutionStrategyConfiguration>>> {
    val btaV2Strategies = BtaV2StrategyAgnosticCompilationTestArgumentProvider.namedStrategyArguments()
    val compilerArguments = jsCompilerArguments.filter { it.runsInvalidRawValueTest }.map { named("[${it.argumentName}]", it) }

    return btaV2Strategies.flatMap { namedStrategy ->
        compilerArguments.map { namedArgumentDescriptor ->
            named(
                namedStrategy.name + namedArgumentDescriptor.name,
                JsArgumentConfiguration(namedStrategy.payload.first, namedArgumentDescriptor.payload) to namedStrategy.payload
            )
        }
    }
}

private fun namedArgumentConfiguration(
    argumentPredicate: (JsArgumentTestDescriptor<*>) -> Boolean = { true },
): List<Named<JsArgumentConfiguration<*>>> {
    val btaVersions = BtaVersionsCompilationTestArgumentProvider.namedToolchainProviders()
    val compilerArguments = jsCompilerArguments.filter { argumentPredicate(it) }.map { named("[${it.argumentName}]", it) }

    return btaVersions.flatMap { namedKotlinToolchains ->
        compilerArguments.map { namedArgumentDescriptor ->
            named(
                namedKotlinToolchains.name + namedArgumentDescriptor.name,
                JsArgumentConfiguration(namedKotlinToolchains.payload(), namedArgumentDescriptor.payload)
            )
        }
    }
}

@OptIn(ExperimentalCompilerArgument::class)
private val jsCompilerArguments: List<JsArgumentTestDescriptor<*>> = listOf(
    JsArgumentTestDescriptor(
        argumentName = "target",
        argument = TARGET,
        availableSinceVersion = TARGET.availableSinceVersion,
        argumentValues = JsEcmaVersion.entries.toList(),
        argumentRawValues = JsEcmaVersion.entries.map { it.stringValue },
        invalidRawValues = listOf("es15"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-target", value) },
        setArgumentValue = { value -> (this as JsCompilerLinkingArguments.Builder)[TARGET] = value },
        getArgumentValue = { (this as JsCompilerLinkingArguments)[TARGET] },
    ),
    JsArgumentTestDescriptor(
        argumentName = "module-kind",
        argument = MODULE_KIND,
        availableSinceVersion = MODULE_KIND.availableSinceVersion,
        argumentValues = JsModuleKind.entries.toList(),
        argumentRawValues = JsModuleKind.entries.map { it.stringValue },
        invalidRawValues = listOf("emd"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-module-kind", value) },
        setArgumentValue = { value -> (this as JsCompilerLinkingArguments.Builder)[MODULE_KIND] = value },
        getArgumentValue = { (this as JsCompilerLinkingArguments)[MODULE_KIND] },
    ),
    JsArgumentTestDescriptor(
        argumentName = "Xir-safe-external-boolean-diagnostic",
        argument = X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC,
        availableSinceVersion = X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC.availableSinceVersion,
        argumentValues = JsIrDiagnosticMode.entries.toList(),
        argumentRawValues = JsIrDiagnosticMode.entries.map { it.stringValue },
        invalidRawValues = listOf("error"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-Xir-safe-external-boolean-diagnostic=$value") },
        setArgumentValue = { value -> (this as JsCompilerLinkingArguments.Builder)[X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC] = value },
        getArgumentValue = { (this as JsCompilerLinkingArguments)[X_IR_SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC] },
    ),
)
