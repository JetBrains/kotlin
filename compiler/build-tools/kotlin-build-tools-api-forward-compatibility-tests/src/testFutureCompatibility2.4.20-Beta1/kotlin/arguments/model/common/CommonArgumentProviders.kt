/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments.model.common

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.KOTLIN_HOME
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.OPT_IN
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_ANNOTATION_DEFAULT_TARGET
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_DISABLE_PHASES
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_DUMP_DIRECTORY
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_DUMP_PERF
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_NAME_BASED_DESTRUCTURING
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_PHASES_TO_DUMP
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_PHASES_TO_DUMP_AFTER
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_PHASES_TO_DUMP_BEFORE
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_PHASES_TO_VALIDATE
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_PHASES_TO_VALIDATE_AFTER
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_PHASES_TO_VALIDATE_BEFORE
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_SUPPRESS_WARNING
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_VERBOSE_PHASES
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_VERIFY_IR
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_WARNING_LEVEL
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.WarningLevel
import org.jetbrains.kotlin.buildtools.api.arguments.enums.AnnotationDefaultTargetMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.NameBasedDestructuringMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.VerifyIrMode
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTestArgumentProvider
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsCompilationTestArgumentProvider
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

internal class AllCommonCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration().map { Arguments.of(it) }.stream()
    }
}

internal class InvalidArgumentValueCommonCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.runsInvalidArgumentValueTest }.map { Arguments.of(it) }.stream()
    }
}

internal class InvalidRawValueCommonCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.runsInvalidRawValueTest }.map { Arguments.of(it) }.stream()
    }
}

internal class NullableCommonCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.runsNullableTest }.map { Arguments.of(it) }.stream()
    }
}

internal class InvalidRawValueCommonCompilerArgumentsBtaV2StrategyAgnosticArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedInvalidRawValueBtaV2ArgumentConfigurations().map { Arguments.of(it) }.stream()
    }
}

private fun namedInvalidRawValueBtaV2ArgumentConfigurations(): List<Named<Pair<CommonArgumentConfiguration<*>, CompilerExecutionStrategyConfiguration>>> {
    val btaV2Strategies = BtaV2StrategyAgnosticCompilationTestArgumentProvider.namedStrategyArguments()
    val compilerArguments = commonCompilerArguments
        .filter { it.runsInvalidRawValueTest }
        .map { named("[${it.argumentName}]", it) }

    return btaV2Strategies.flatMap { namedStrategy ->
        compilerArguments.map { namedArgDescriptor ->
            named(
                namedStrategy.name + namedArgDescriptor.name,
                CommonArgumentConfiguration(namedStrategy.payload.first, namedArgDescriptor.payload) to namedStrategy.payload
            )
        }
    }
}

private fun namedArgumentConfiguration(argumentPredicate: (CommonArgumentTestDescriptor<*>) -> Boolean = { true }): List<Named<CommonArgumentConfiguration<*>>> {
    val btaVersions = BtaVersionsCompilationTestArgumentProvider.namedStrategyArguments()
    val compilerArguments = commonCompilerArguments.filter { argumentPredicate(it) }.map { named("[${it.argumentName}]", it) }

    return btaVersions.flatMap { namedKotlinToolchains ->
        compilerArguments.mapNotNull { namedArgumentDescriptor ->
            if (namedArgumentDescriptor.payload.skipBtaV1 && namedKotlinToolchains.name.contains("[v1]")) return@mapNotNull null
            named(
                namedKotlinToolchains.name + namedArgumentDescriptor.name,
                CommonArgumentConfiguration(namedKotlinToolchains.payload, namedArgumentDescriptor.payload)
            )
        }
    }
}

private val testBaseDir: Path = Paths.get("").toAbsolutePath()

@OptIn(ExperimentalCompilerArgument::class)
internal val commonCompilerArguments: List<CommonArgumentTestDescriptor<*>> = listOf(
    CommonArgumentTestDescriptor(
        argumentName = "opt-in",
        argument = OPT_IN,
        argumentValues = listOf(listOf("kotlin.RequiresOptIn", "kotlin.ExperimentalStdlibApi", "kotlin.time.ExperimentalTime")),
        argumentRawValues = listOf(
            listOf(
                "kotlin.RequiresOptIn",
                "kotlin.ExperimentalStdlibApi",
                "kotlin.time.ExperimentalTime"
            ).joinToString(",")
        ),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-opt-in", value) },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "kotlin-home",
        argument = KOTLIN_HOME,
        argumentValues = listOf(testBaseDir.resolve("path/to/kotlin")),
        argumentRawValues = listOf(testBaseDir.resolve("path/to/kotlin").toFile().absolutePath),
        runsNullableTest = true,
        valueString = { value -> value?.toFile()?.absolutePath },
        expectedArgumentStringsFor = { value -> listOf("-kotlin-home", value) },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xverbose-phases",
        argument = X_VERBOSE_PHASES,
        argumentValues = listOf(listOf("phase1", "phase2", "phase3")),
        argumentRawValues = listOf(listOf("phase1", "phase2", "phase3").joinToString(",")),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xverbose-phases=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xdisable-phases",
        argument = X_DISABLE_PHASES,
        argumentValues = listOf(listOf("phase1", "phase2", "phase3")),
        argumentRawValues = listOf(listOf("phase1", "phase2", "phase3").joinToString(",")),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xdisable-phases=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xsuppress-warning",
        argument = X_SUPPRESS_WARNING,
        argumentValues = listOf(listOf("warning1", "warning2", "warning3")),
        argumentRawValues = listOf(listOf("warning1", "warning2", "warning3").joinToString(",")),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xsuppress-warning=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xannotation-default-target",
        argument = X_ANNOTATION_DEFAULT_TARGET,
        argumentValues = AnnotationDefaultTargetMode.entries.toList(),
        argumentRawValues = AnnotationDefaultTargetMode.entries.map { it.stringValue },
        invalidRawValues = listOf("non-existent-value"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-Xannotation-default-target=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xverify-ir",
        argument = X_VERIFY_IR,
        argumentValues = VerifyIrMode.entries.toList(),
        argumentRawValues = VerifyIrMode.entries.map { it.stringValue },
        invalidRawValues = listOf("non-existent-value"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-Xverify-ir=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xname-based-destructuring",
        argument = X_NAME_BASED_DESTRUCTURING,
        argumentValues = NameBasedDestructuringMode.entries.toList(),
        argumentRawValues = NameBasedDestructuringMode.entries.map { it.stringValue },
        invalidRawValues = listOf("non-existent-value"),
        runsNullableTest = true,
        skipBtaV1 = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-Xname-based-destructuring=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xphases-to-dump",
        argument = X_PHASES_TO_DUMP,
        argumentValues = listOf(listOf("phase1", "phase2", "phase3")),
        argumentRawValues = listOf(listOf("phase1", "phase2", "phase3").joinToString(",")),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xphases-to-dump=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xphases-to-dump-before",
        argument = X_PHASES_TO_DUMP_BEFORE,
        argumentValues = listOf(listOf("phase1", "phase2", "phase3")),
        argumentRawValues = listOf(listOf("phase1", "phase2", "phase3").joinToString(",")),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xphases-to-dump-before=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xphases-to-dump-after",
        argument = X_PHASES_TO_DUMP_AFTER,
        argumentValues = listOf(listOf("phase1", "phase2", "phase3")),
        argumentRawValues = listOf(listOf("phase1", "phase2", "phase3").joinToString(",")),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xphases-to-dump-after=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xphases-to-validate",
        argument = X_PHASES_TO_VALIDATE,
        argumentValues = listOf(listOf("phase1", "phase2", "phase3")),
        argumentRawValues = listOf(listOf("phase1", "phase2", "phase3").joinToString(",")),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xphases-to-validate=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xphases-to-validate-before",
        argument = X_PHASES_TO_VALIDATE_BEFORE,
        argumentValues = listOf(listOf("phase1", "phase2", "phase3")),
        argumentRawValues = listOf(listOf("phase1", "phase2", "phase3").joinToString(",")),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xphases-to-validate-before=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xphases-to-validate-after",
        argument = X_PHASES_TO_VALIDATE_AFTER,
        argumentValues = listOf(listOf("phase1", "phase2", "phase3")),
        argumentRawValues = listOf(listOf("phase1", "phase2", "phase3").joinToString(",")),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xphases-to-validate-after=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xdump-directory",
        argument = X_DUMP_DIRECTORY,
        argumentValues = listOf(testBaseDir.resolve("path/to/dump")),
        argumentRawValues = listOf(testBaseDir.resolve("path/to/dump").toFile().absolutePath),
        runsNullableTest = true,
        valueString = { value -> value?.toFile()?.absolutePath },
        expectedArgumentStringsFor = { value -> listOf("-Xdump-directory=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xdump-perf",
        argument = X_DUMP_PERF,
        argumentValues = listOf(testBaseDir.resolve("path/to/perf.log")),
        argumentRawValues = listOf(testBaseDir.resolve("path/to/perf.log").toFile().absolutePath),
        runsNullableTest = true,
        valueString = { value -> value?.toFile()?.absolutePath },
        expectedArgumentStringsFor = { value -> listOf("-Xdump-perf=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xwarning-level",
        argument = X_WARNING_LEVEL,
        argumentValues = listOf(
            listOf(
                WarningLevel("DEPRECATION", WarningLevel.Severity.ERROR),
                WarningLevel("UNUSED_VARIABLE", WarningLevel.Severity.DISABLED),
            )
        ),
        argumentRawValues = listOf("DEPRECATION:error,UNUSED_VARIABLE:disabled"),
        invalidRawValues = listOf("DEPRECATION:non-existent-level", "CONTEXTUAL_OVERLOAD_SHADOWED=error"),
        valueString = { value -> value?.joinToString(",") { "${it.warningName}:${it.severity.stringValue}" } },
        expectedArgumentStringsFor = { value -> listOf("-Xwarning-level=$value") },
    ),
)
