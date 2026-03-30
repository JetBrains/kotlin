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
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

internal class AllCommonCompilerArgumentsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration().map { Arguments.of(it) }.stream()
    }
}

internal class InvalidValueCommonCompilerArgumentsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.runsInvalidArgumentValueTest }.map { Arguments.of(it) }.stream()
    }
}

private fun namedArgumentConfiguration(
    argumentPredicate: (CommonArgumentTestDescriptor<*>) -> Boolean = { true },
): List<Named<CommonArgumentConfiguration<*>>> {
    return commonCompilerArgumentTestDescriptors
        .filter { argumentPredicate(it) }
        .map { named("[${it.argumentName}]", CommonArgumentConfiguration(it)) }
}

private val testBaseDir: Path = Paths.get("").toAbsolutePath()

@OptIn(ExperimentalCompilerArgument::class)
internal val commonCompilerArgumentTestDescriptors: List<CommonArgumentTestDescriptor<*>> = listOf(
    CommonArgumentTestDescriptor(
        argumentName = "opt-in",
        argument = OPT_IN,
        argumentValues = listOf(arrayOf("kotlin.RequiresOptIn", "kotlin.ExperimentalStdlibApi", "kotlin.time.ExperimentalTime")),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-opt-in", value) },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "kotlin-home",
        argument = KOTLIN_HOME,
        argumentValues = listOf(testBaseDir.resolve("path/to/kotlin").toFile().absolutePath),
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> listOf("-kotlin-home", value) },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xverbose-phases",
        argument = X_VERBOSE_PHASES,
        argumentValues = listOf(arrayOf("phase1", "phase2", "phase3")),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xverbose-phases=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xdisable-phases",
        argument = X_DISABLE_PHASES,
        argumentValues = listOf(arrayOf("phase1", "phase2", "phase3")),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xdisable-phases=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xsuppress-warning",
        argument = X_SUPPRESS_WARNING,
        argumentValues = listOf(arrayOf("warning1", "warning2", "warning3")),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xsuppress-warning=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xannotation-default-target",
        argument = X_ANNOTATION_DEFAULT_TARGET,
        argumentValues = listOf("first-only", "first-only-warn", "param-property"),
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> listOf("-Xannotation-default-target=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xverify-ir",
        argument = X_VERIFY_IR,
        argumentValues = listOf("none", "warning", "error"),
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> listOf("-Xverify-ir=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xname-based-destructuring",
        argument = X_NAME_BASED_DESTRUCTURING,
        argumentValues = listOf("only-syntax", "name-mismatch", "complete"),
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> listOf("-Xname-based-destructuring=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xphases-to-dump",
        argument = X_PHASES_TO_DUMP,
        argumentValues = listOf(arrayOf("phase1", "phase2", "phase3")),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xphases-to-dump=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xphases-to-dump-before",
        argument = X_PHASES_TO_DUMP_BEFORE,
        argumentValues = listOf(arrayOf("phase1", "phase2", "phase3")),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xphases-to-dump-before=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xphases-to-dump-after",
        argument = X_PHASES_TO_DUMP_AFTER,
        argumentValues = listOf(arrayOf("phase1", "phase2", "phase3")),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xphases-to-dump-after=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xphases-to-validate",
        argument = X_PHASES_TO_VALIDATE,
        argumentValues = listOf(arrayOf("phase1", "phase2", "phase3")),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xphases-to-validate=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xphases-to-validate-before",
        argument = X_PHASES_TO_VALIDATE_BEFORE,
        argumentValues = listOf(arrayOf("phase1", "phase2", "phase3")),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xphases-to-validate-before=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xphases-to-validate-after",
        argument = X_PHASES_TO_VALIDATE_AFTER,
        argumentValues = listOf(arrayOf("phase1", "phase2", "phase3")),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xphases-to-validate-after=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xdump-directory",
        argument = X_DUMP_DIRECTORY,
        argumentValues = listOf(testBaseDir.resolve("path/to/dump").toFile().absolutePath),
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> listOf("-Xdump-directory=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xdump-perf",
        argument = X_DUMP_PERF,
        argumentValues = listOf(testBaseDir.resolve("path/to/perf.log").toFile().absolutePath),
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> listOf("-Xdump-perf=$value") },
    ),
    CommonArgumentTestDescriptor(
        argumentName = "Xwarning-level",
        argument = X_WARNING_LEVEL,
        argumentValues = listOf(arrayOf("DEPRECATION:error", "UNUSED_VARIABLE:disabled")),
        invalidArgumentValue = arrayOf("DEPRECATION:non-existent-level"),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xwarning-level=$value") },
    ),
)
