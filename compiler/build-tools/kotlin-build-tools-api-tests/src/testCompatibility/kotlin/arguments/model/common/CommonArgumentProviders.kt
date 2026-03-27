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
import org.jetbrains.kotlin.buildtools.tests.arguments.model.ArgumentTestDescriptor
import org.jetbrains.kotlin.buildtools.tests.arguments.model.argumentTestDescriptor
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

internal class EnumCommonCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.runsEnumTest }.map { Arguments.of(it) }.stream()
    }
}

internal class NullableCommonCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.runsNullableTest }.map { Arguments.of(it) }.stream()
    }
}

private fun namedArgumentConfiguration(argumentPredicate: (ArgumentTestDescriptor<*>) -> Boolean = { true }): List<Named<CommonArgumentConfiguration<*>>> {
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
internal val commonCompilerArguments: List<ArgumentTestDescriptor<*>> = listOf(
    argumentTestDescriptor<List<String>> {
        argumentName = "opt-in"
        argumentId = OPT_IN.id
        availableSinceVersion = OPT_IN.availableSinceVersion
        argumentValues = listOf(listOf("kotlin.RequiresOptIn", "kotlin.ExperimentalStdlibApi", "kotlin.time.ExperimentalTime"))
        valueString = { value -> value?.joinToString(",") }
        expectedArgumentStringsFor = { value -> listOf("-opt-in", value) }
    },
    argumentTestDescriptor<Path> {
        argumentName = "kotlin-home"
        argumentId = KOTLIN_HOME.id
        availableSinceVersion = KOTLIN_HOME.availableSinceVersion
        argumentValues = listOf(testBaseDir.resolve("path/to/kotlin"))
        runsNullableTest = true
        valueString = { value -> value?.toFile()?.absolutePath }
        expectedArgumentStringsFor = { value -> listOf("-kotlin-home", value) }
    },
    argumentTestDescriptor<List<String>> {
        argumentName = "Xverbose-phases"
        argumentId = X_VERBOSE_PHASES.id
        availableSinceVersion = X_VERBOSE_PHASES.availableSinceVersion
        argumentValues = listOf(listOf("phase1", "phase2", "phase3"))
        valueString = { value -> value?.joinToString(",") }
        expectedArgumentStringsFor = { value -> listOf("-Xverbose-phases=$value") }
    },
    argumentTestDescriptor<List<String>> {
        argumentName = "Xdisable-phases"
        argumentId = X_DISABLE_PHASES.id
        availableSinceVersion = X_DISABLE_PHASES.availableSinceVersion
        argumentValues = listOf(listOf("phase1", "phase2", "phase3"))
        valueString = { value -> value?.joinToString(",") }
        expectedArgumentStringsFor = { value -> listOf("-Xdisable-phases=$value") }
    },
    argumentTestDescriptor<List<String>> {
        argumentName = "Xsuppress-warning"
        argumentId = X_SUPPRESS_WARNING.id
        availableSinceVersion = X_SUPPRESS_WARNING.availableSinceVersion
        argumentValues = listOf(listOf("warning1", "warning2", "warning3"))
        valueString = { value -> value?.joinToString(",") }
        expectedArgumentStringsFor = { value -> listOf("-Xsuppress-warning=$value") }
    },
    argumentTestDescriptor<AnnotationDefaultTargetMode> {
        argumentName = "Xannotation-default-target"
        argumentId = X_ANNOTATION_DEFAULT_TARGET.id
        availableSinceVersion = X_ANNOTATION_DEFAULT_TARGET.availableSinceVersion
        argumentValues = AnnotationDefaultTargetMode.entries.toList()
        runsEnumTest = true
        runsNullableTest = true
        valueString = { value -> value?.stringValue }
        expectedArgumentStringsFor = { value -> listOf("-Xannotation-default-target=$value") }
    },
    argumentTestDescriptor<VerifyIrMode> {
        argumentName = "Xverify-ir"
        argumentId = X_VERIFY_IR.id
        availableSinceVersion = X_VERIFY_IR.availableSinceVersion
        argumentValues = VerifyIrMode.entries.toList()
        runsEnumTest = true
        runsNullableTest = true
        valueString = { value -> value?.stringValue }
        expectedArgumentStringsFor = { value -> listOf("-Xverify-ir=$value") }
    },
    argumentTestDescriptor<NameBasedDestructuringMode> {
        argumentName = "Xname-based-destructuring"
        argumentId = X_NAME_BASED_DESTRUCTURING.id
        availableSinceVersion = X_NAME_BASED_DESTRUCTURING.availableSinceVersion
        argumentValues = NameBasedDestructuringMode.entries.toList()
        runsEnumTest = true
        runsNullableTest = true
        skipBtaV1 = true
        valueString = { value -> value?.stringValue }
        expectedArgumentStringsFor = { value -> listOf("-Xname-based-destructuring=$value") }
    },
    argumentTestDescriptor<List<String>> {
        argumentName = "Xphases-to-dump"
        argumentId = X_PHASES_TO_DUMP.id
        availableSinceVersion = X_PHASES_TO_DUMP.availableSinceVersion
        argumentValues = listOf(listOf("phase1", "phase2", "phase3"))
        valueString = { value -> value?.joinToString(",") }
        expectedArgumentStringsFor = { value -> listOf("-Xphases-to-dump=$value") }
    },
    argumentTestDescriptor<List<String>> {
        argumentName = "Xphases-to-dump-before"
        argumentId = X_PHASES_TO_DUMP_BEFORE.id
        availableSinceVersion = X_PHASES_TO_DUMP_BEFORE.availableSinceVersion
        argumentValues = listOf(listOf("phase1", "phase2", "phase3"))
        valueString = { value -> value?.joinToString(",") }
        expectedArgumentStringsFor = { value -> listOf("-Xphases-to-dump-before=$value") }
    },
    argumentTestDescriptor<List<String>> {
        argumentName = "Xphases-to-dump-after"
        argumentId = X_PHASES_TO_DUMP_AFTER.id
        availableSinceVersion = X_PHASES_TO_DUMP_AFTER.availableSinceVersion
        argumentValues = listOf(listOf("phase1", "phase2", "phase3"))
        valueString = { value -> value?.joinToString(",") }
        expectedArgumentStringsFor = { value -> listOf("-Xphases-to-dump-after=$value") }
    },
    argumentTestDescriptor<List<String>> {
        argumentName = "Xphases-to-validate"
        argumentId = X_PHASES_TO_VALIDATE.id
        availableSinceVersion = X_PHASES_TO_VALIDATE.availableSinceVersion
        argumentValues = listOf(listOf("phase1", "phase2", "phase3"))
        valueString = { value -> value?.joinToString(",") }
        expectedArgumentStringsFor = { value -> listOf("-Xphases-to-validate=$value") }
    },
    argumentTestDescriptor<List<String>> {
        argumentName = "Xphases-to-validate-before"
        argumentId = X_PHASES_TO_VALIDATE_BEFORE.id
        availableSinceVersion = X_PHASES_TO_VALIDATE_BEFORE.availableSinceVersion
        argumentValues = listOf(listOf("phase1", "phase2", "phase3"))
        valueString = { value -> value?.joinToString(",") }
        expectedArgumentStringsFor = { value -> listOf("-Xphases-to-validate-before=$value") }
    },
    argumentTestDescriptor<List<String>> {
        argumentName = "Xphases-to-validate-after"
        argumentId = X_PHASES_TO_VALIDATE_AFTER.id
        availableSinceVersion = X_PHASES_TO_VALIDATE_AFTER.availableSinceVersion
        argumentValues = listOf(listOf("phase1", "phase2", "phase3"))
        valueString = { value -> value?.joinToString(",") }
        expectedArgumentStringsFor = { value -> listOf("-Xphases-to-validate-after=$value") }
    },
    argumentTestDescriptor<Path> {
        argumentName = "Xdump-directory"
        argumentId = X_DUMP_DIRECTORY.id
        availableSinceVersion = X_DUMP_DIRECTORY.availableSinceVersion
        argumentValues = listOf(testBaseDir.resolve("path/to/dump"))
        valueString = { value -> value?.toFile()?.absolutePath }
        expectedArgumentStringsFor = { value -> listOf("-Xdump-directory=$value") }
    },
    argumentTestDescriptor<Path> {
        argumentName = "Xdump-perf"
        argumentId = X_DUMP_PERF.id
        availableSinceVersion = X_DUMP_PERF.availableSinceVersion
        argumentValues = listOf(testBaseDir.resolve("path/to/perf.log"))
        valueString = { value -> value?.toFile()?.absolutePath }
        expectedArgumentStringsFor = { value -> listOf("-Xdump-perf=$value") }
    },
    argumentTestDescriptor<List<WarningLevel>> {
        argumentName = "Xwarning-level"
        argumentId = X_WARNING_LEVEL.id
        availableSinceVersion = X_WARNING_LEVEL.availableSinceVersion
        argumentValues = listOf(
            listOf(
                WarningLevel("DEPRECATION", WarningLevel.Severity.ERROR),
                WarningLevel("UNUSED_VARIABLE", WarningLevel.Severity.DISABLED),
            )
        )
        valueString = { value -> value?.joinToString(",") { "${it.warningName}:${it.severity.stringValue}" } }
        expectedArgumentStringsFor = { value -> listOf("-Xwarning-level=$value") }
    }
)
