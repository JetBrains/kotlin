/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments.model

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.KOTLIN_HOME
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.OPT_IN
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_ANNOTATION_DEFAULT_TARGET
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_DISABLE_PHASES
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
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.CommonCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.enums.AnnotationDefaultTargetMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.NameBasedDestructuringMode
import org.jetbrains.kotlin.buildtools.api.arguments.enums.VerifyIrMode
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
        return namedArgumentConfiguration { it.isEnum }.map { Arguments.of(it) }.stream()
    }
}

internal class NullableCommonCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.isNullable }.map { Arguments.of(it) }.stream()
    }
}

internal class CommonArgumentDescriptor<T>(
    override val argumentName: String,
    val argumentKey: CommonCompilerArgument<T>,
    override val argumentValues: List<T>,
    override val isEnum: Boolean,
    override val isNullable: Boolean,
    override val valueString: (T?) -> String?,
    override val expectedArgumentStringsFor: (String) -> List<String>,
) : ArgumentDescriptor<T>

private fun namedArgumentConfiguration(argumentPredicate: (CommonArgumentDescriptor<*>) -> Boolean = { true }): List<Named<CommonArgumentConfiguration<*>>> {
    val btaVersions = BtaVersionsCompilationTestArgumentProvider.namedStrategyArguments()
    val compilerArguments = commonCompilerArguments.filter { argumentPredicate(it) }.map { named("[${it.argumentName}]", it) }

    return btaVersions.flatMap { namedKotlinToolchains ->
        compilerArguments.map { namedArgumentDescriptor ->
            named(
                namedKotlinToolchains.name + namedArgumentDescriptor.name,
                CommonArgumentConfiguration(namedKotlinToolchains.payload, namedArgumentDescriptor.payload)
            )
        }
    }
}

private val testBaseDir: Path = Paths.get("").toAbsolutePath()

@OptIn(ExperimentalCompilerArgument::class)
private val commonCompilerArguments: List<CommonArgumentDescriptor<*>> = listOf(
    CommonArgumentDescriptor(
        argumentName = "opt-in",
        argumentKey = OPT_IN,
        argumentValues = listOf(listOf("kotlin.RequiresOptIn", "kotlin.ExperimentalStdlibApi", "kotlin.time.ExperimentalTime")),
        isEnum = false,
        isNullable = false,
        valueString = { value: List<String>? -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value: String -> listOf("-opt-in", value) }),
    CommonArgumentDescriptor(
        argumentName = "kotlin-home",
        argumentKey = KOTLIN_HOME,
        argumentValues = listOf(testBaseDir.resolve("path/to/kotlin")),
        isEnum = false,
        isNullable = true,
        valueString = { value: Path? -> value?.toFile()?.absolutePath },
        expectedArgumentStringsFor = { value: String -> listOf("-kotlin-home", value) }),
    CommonArgumentDescriptor(
        argumentName = "Xverbose-phases",
        argumentKey = X_VERBOSE_PHASES,
        argumentValues = listOf(listOf("phase1", "phase2", "phase3")),
        isEnum = false,
        isNullable = false,
        valueString = { value: List<String>? -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value: String -> listOf("-Xverbose-phases=$value") }),
    CommonArgumentDescriptor(
        argumentName = "Xdisable-phases",
        argumentKey = X_DISABLE_PHASES,
        argumentValues = listOf(listOf("phase1", "phase2", "phase3")),
        isEnum = false,
        isNullable = false,
        valueString = { value: List<String>? -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value: String -> listOf("-Xdisable-phases=$value") }),
    CommonArgumentDescriptor(
        argumentName = "Xsuppress-warning",
        argumentKey = X_SUPPRESS_WARNING,
        argumentValues = listOf(listOf("warning1", "warning2", "warning3")),
        isEnum = false,
        isNullable = false,
        valueString = { value: List<String>? -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value: String -> listOf("-Xsuppress-warning=$value") }),
    CommonArgumentDescriptor(
        argumentName = "Xannotation-default-target",
        argumentKey = X_ANNOTATION_DEFAULT_TARGET,
        argumentValues = AnnotationDefaultTargetMode.entries.toList(),
        isEnum = true,
        isNullable = true,
        valueString = { value: AnnotationDefaultTargetMode? -> value?.stringValue },
        expectedArgumentStringsFor = { value: String -> listOf("-Xannotation-default-target=$value") }),
    CommonArgumentDescriptor(
        argumentName = "Xverify-ir",
        argumentKey = X_VERIFY_IR,
        argumentValues = VerifyIrMode.entries.toList(),
        isEnum = true,
        isNullable = true,
        valueString = { value: VerifyIrMode? -> value?.stringValue },
        expectedArgumentStringsFor = { value: String -> listOf("-Xverify-ir=$value") }),
    CommonArgumentDescriptor(
        argumentName = "Xname-based-destructuring",
        argumentKey = X_NAME_BASED_DESTRUCTURING,
        argumentValues = NameBasedDestructuringMode.entries.toList(),
        isEnum = true,
        isNullable = true,
        valueString = { value: NameBasedDestructuringMode? -> value?.stringValue },
        expectedArgumentStringsFor = { value: String -> listOf("-Xname-based-destructuring=$value") }),
    CommonArgumentDescriptor(
        argumentName = "Xphases-to-dump",
        argumentKey = X_PHASES_TO_DUMP,
        argumentValues = listOf(listOf("phase1", "phase2", "phase3")),
        isEnum = false,
        isNullable = false,
        valueString = { value: List<String>? -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value: String -> listOf("-Xphases-to-dump=$value") }),
    CommonArgumentDescriptor(
        argumentName = "Xphases-to-dump-before",
        argumentKey = X_PHASES_TO_DUMP_BEFORE,
        argumentValues = listOf(listOf("phase1", "phase2", "phase3")),
        isEnum = false,
        isNullable = false,
        valueString = { value: List<String>? -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value: String -> listOf("-Xphases-to-dump-before=$value") }),
    CommonArgumentDescriptor(
        argumentName = "Xphases-to-dump-after",
        argumentKey = X_PHASES_TO_DUMP_AFTER,
        argumentValues = listOf(listOf("phase1", "phase2", "phase3")),
        isEnum = false,
        isNullable = false,
        valueString = { value: List<String>? -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value: String -> listOf("-Xphases-to-dump-after=$value") }),
    CommonArgumentDescriptor(
        argumentName = "Xphases-to-validate",
        argumentKey = X_PHASES_TO_VALIDATE,
        argumentValues = listOf(listOf("phase1", "phase2", "phase3")),
        isEnum = false,
        isNullable = false,
        valueString = { value: List<String>? -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value: String -> listOf("-Xphases-to-validate=$value") }),
    CommonArgumentDescriptor(
        argumentName = "Xphases-to-validate-before",
        argumentKey = X_PHASES_TO_VALIDATE_BEFORE,
        argumentValues = listOf(listOf("phase1", "phase2", "phase3")),
        isEnum = false,
        isNullable = false,
        valueString = { value: List<String>? -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value: String -> listOf("-Xphases-to-validate-before=$value") }),
    CommonArgumentDescriptor(
        argumentName = "Xphases-to-validate-after",
        argumentKey = X_PHASES_TO_VALIDATE_AFTER,
        argumentValues = listOf(listOf("phase1", "phase2", "phase3")),
        isEnum = false,
        isNullable = false,
        valueString = { value: List<String>? -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value: String -> listOf("-Xphases-to-validate-after=$value") }),
)
