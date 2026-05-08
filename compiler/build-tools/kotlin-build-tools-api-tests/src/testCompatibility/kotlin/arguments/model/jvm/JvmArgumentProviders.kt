/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments.model.jvm

import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.Jsr305
import org.jetbrains.kotlin.buildtools.api.arguments.Jsr305.Mode
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.CLASSPATH
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.JDK_HOME
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.JVM_DEFAULT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.SCRIPT_TEMPLATES
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_ABI_STABILITY
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_ADD_MODULES
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_ASSERTIONS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_FRIEND_PATHS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_IGNORED_ANNOTATIONS_FOR_BRIDGES
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_JAVA_SOURCE_ROOTS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_JDK_RELEASE
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_JSPECIFY_ANNOTATIONS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_JSR305
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_KLIB
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_LAMBDAS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_MODULE_PATH
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_NULLABILITY_ANNOTATIONS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_PROFILE
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_SAM_CONVERSIONS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_SCRIPT_RESOLVER_ENVIRONMENT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_STRING_CONCAT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_WHEN_EXPRESSIONS
import org.jetbrains.kotlin.buildtools.api.arguments.NullabilityAnnotation
import org.jetbrains.kotlin.buildtools.api.arguments.ProfileCompilerCommand
import org.jetbrains.kotlin.buildtools.api.arguments.enums.*
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTestArgumentProvider
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsCompilationTestArgumentProvider
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

internal class AllJvmCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration().map { Arguments.of(it) }.stream()
    }
}

internal class InvalidArgumentValueJvmCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.runsInvalidArgumentValueTest }.map { Arguments.of(it) }.stream()
    }
}

internal class InvalidRawValueJvmCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.runsInvalidRawValueTest }.map { Arguments.of(it) }.stream()
    }
}

internal class NullableJvmCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.runsNullableTest }.map { Arguments.of(it) }.stream()
    }
}

internal class InvalidRawValueJvmCompilerArgumentsBtaV2StrategyAgnosticArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedInvalidRawValueBtaV2ArgumentConfigurations().map { Arguments.of(it) }.stream()
    }
}

private fun namedInvalidRawValueBtaV2ArgumentConfigurations(): List<Named<Pair<JvmArgumentConfiguration<*>, CompilerExecutionStrategyConfiguration>>> {
    val btaV2Strategies = BtaV2StrategyAgnosticCompilationTestArgumentProvider.namedStrategyArguments()
    val compilerArguments = jvmCompilerArguments
        .filter { it.runsInvalidRawValueTest }
        .map { named("[${it.argumentName}]", it) }

    return btaV2Strategies.flatMap { namedStrategy ->
        compilerArguments.map { namedArgDescriptor ->
            named(
                namedStrategy.name + namedArgDescriptor.name,
                JvmArgumentConfiguration(namedStrategy.payload.first, namedArgDescriptor.payload) to namedStrategy.payload
            )
        }
    }
}

private fun namedArgumentConfiguration(argumentPredicate: (JvmArgumentTestDescriptor<*>) -> Boolean = { true }): List<Named<JvmArgumentConfiguration<*>>> {
    val btaVersions = BtaVersionsCompilationTestArgumentProvider.namedStrategyArguments()
    val compilerArguments = jvmCompilerArguments.filter { argumentPredicate(it) }.map { named("[${it.argumentName}]", it) }

    return btaVersions.flatMap { namedKotlinToolchains ->
        compilerArguments.mapNotNull { namedArgumentDescriptor ->
            if (namedArgumentDescriptor.payload.skipBtaV1 && namedKotlinToolchains.name.contains("[v1]")) return@mapNotNull null

            named(
                namedKotlinToolchains.name + namedArgumentDescriptor.name,
                JvmArgumentConfiguration(namedKotlinToolchains.payload, namedArgumentDescriptor.payload)
            )
        }
    }
}

private val testBaseDir: Path = Paths.get("").toAbsolutePath()

@OptIn(ExperimentalCompilerArgument::class)
private val jvmCompilerArguments: List<JvmArgumentTestDescriptor<*>> = listOf(
    JvmArgumentTestDescriptor(
        argumentName = "Xabi-stability",
        argument = X_ABI_STABILITY,
        argumentValues = AbiStabilityMode.entries.toList(),
        argumentRawValues = AbiStabilityMode.entries.map { it.stringValue },
        invalidRawValues = listOf("non-existent-value"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-Xabi-stability=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xadd-modules",
        argument = X_ADD_MODULES,
        argumentValues = listOf(listOf("module1", "module2", "module3")),
        argumentRawValues = listOf(listOf("module1", "module2", "module3").joinToString(",")),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xadd-modules=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xassertions",
        argument = X_ASSERTIONS,
        argumentValues = AssertionsMode.entries.toList(),
        argumentRawValues = AssertionsMode.entries.map { it.stringValue },
        invalidRawValues = listOf("non-existent-value"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-Xassertions=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "classpath",
        argument = CLASSPATH,
        argumentValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/lib1.jar"),
                testBaseDir.resolve("path/to/lib2.jar"),
                testBaseDir.resolve("path/to/classes"),
            )
        ),
        argumentRawValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/lib1.jar"),
                testBaseDir.resolve("path/to/lib2.jar"),
                testBaseDir.resolve("path/to/classes"),
            ).joinToString(File.pathSeparator) { it.toFile().absolutePath }
        ),
        invalidArgumentValues = listOf(listOf(testBaseDir.resolve("path/with${File.pathSeparator}separator"))),
        runsNullableTest = true,
        valueString = { value -> value?.joinToString(File.pathSeparator) { it.toFile().absolutePath } },
        expectedArgumentStringsFor = { value -> listOf("-classpath", value) },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "jdk-home",
        argument = JDK_HOME,
        argumentValues = listOf(testBaseDir.resolve("path/to/jdk")),
        argumentRawValues = listOf(testBaseDir.resolve("path/to/jdk").toFile().absolutePath),
        runsNullableTest = true,
        valueString = { value -> value?.toFile()?.absolutePath },
        expectedArgumentStringsFor = { value -> listOf("-jdk-home", value) },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "jvm-default",
        argument = JVM_DEFAULT,
        argumentValues = JvmDefaultMode.entries.toList(),
        argumentRawValues = JvmDefaultMode.entries.map { it.stringValue },
        invalidRawValues = listOf("non-existent-value"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-jvm-default", value) },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "script-templates",
        argument = SCRIPT_TEMPLATES,
        argumentValues = listOf(listOf("org.example.Template1", "org.example.Template2")),
        argumentRawValues = listOf(listOf("org.example.Template1", "org.example.Template2").joinToString(",")),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-script-templates", value) },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xfriend-paths",
        argument = X_FRIEND_PATHS,
        argumentValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/friend1"),
                testBaseDir.resolve("path/to/friend2"),
                testBaseDir.resolve("path/to/friend3"),
            ),
            listOf(testBaseDir.resolve("path/with,comma"))
        ),
        argumentRawValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/friend1"),
                testBaseDir.resolve("path/to/friend2"),
                testBaseDir.resolve("path/to/friend3"),
            ).joinToString(","),
        ),
        valueString = { value -> value?.joinToString(",") { it.toFile().absolutePath } },
        expectedArgumentStringsFor = { value -> listOf("-Xfriend-paths=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xjdk-release",
        argument = X_JDK_RELEASE,
        argumentValues = JdkRelease.entries.toList(),
        argumentRawValues = JdkRelease.entries.map { it.stringValue },
        invalidRawValues = listOf("non-existent-value"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-Xjdk-release=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xmodule-path",
        argument = X_MODULE_PATH,
        argumentValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/module1"),
                testBaseDir.resolve("path/to/module2"),
                testBaseDir.resolve("path/to/module3"),
            )
        ),
        argumentRawValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/module1"),
                testBaseDir.resolve("path/to/module2"),
                testBaseDir.resolve("path/to/module3"),
            ).joinToString(File.pathSeparator) { it.toFile().absolutePath }
        ),
        invalidArgumentValues = listOf(listOf(testBaseDir.resolve("path/with${File.pathSeparator}separator"))),
        runsNullableTest = true,
        valueString = { value -> value?.joinToString(File.pathSeparator) { it.toFile().absolutePath } },
        expectedArgumentStringsFor = { value -> listOf("-Xmodule-path=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xstring-concat",
        argument = X_STRING_CONCAT,
        argumentValues = StringConcatMode.entries.toList(),
        argumentRawValues = StringConcatMode.entries.map { it.stringValue },
        invalidRawValues = listOf("non-existent-value"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-Xstring-concat=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xlambdas",
        argument = X_LAMBDAS,
        argumentValues = LambdasMode.entries.toList(),
        argumentRawValues = LambdasMode.entries.map { it.stringValue },
        invalidRawValues = listOf("non-existent-value"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-Xlambdas=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xwhen-expressions",
        argument = X_WHEN_EXPRESSIONS,
        argumentValues = WhenExpressionsMode.entries.toList(),
        argumentRawValues = WhenExpressionsMode.entries.map { it.stringValue },
        invalidRawValues = listOf("non-existent-value"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-Xwhen-expressions=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xsam-conversions",
        argument = X_SAM_CONVERSIONS,
        argumentValues = SamConversionsMode.entries.toList(),
        argumentRawValues = SamConversionsMode.entries.map { it.stringValue },
        invalidRawValues = listOf("non-existent-value"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-Xsam-conversions=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xjspecify-annotations",
        argument = X_JSPECIFY_ANNOTATIONS,
        argumentValues = JspecifyAnnotationsMode.entries.toList(),
        argumentRawValues = JspecifyAnnotationsMode.entries.map { it.stringValue },
        invalidRawValues = listOf("non-existent-value"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-Xjspecify-annotations=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xsupport-compatqual-checker-framework-annotations",
        argument = X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS,
        argumentValues = CompatqualAnnotationsMode.entries.toList(),
        argumentRawValues = CompatqualAnnotationsMode.entries.map { it.stringValue },
        invalidRawValues = listOf("non-existent-value"),
        runsNullableTest = true,
        valueString = { value -> value?.stringValue },
        expectedArgumentStringsFor = { value -> listOf("-Xsupport-compatqual-checker-framework-annotations=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xklib",
        argument = X_KLIB,
        argumentValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/lib1.klib"),
                testBaseDir.resolve("path/to/lib2.klib"),
                testBaseDir.resolve("path/to/lib3.klib"),
            )
        ),
        argumentRawValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/lib1.klib"),
                testBaseDir.resolve("path/to/lib2.klib"),
                testBaseDir.resolve("path/to/lib3.klib"),
            ).joinToString(File.pathSeparator) { it.toFile().absolutePath }
        ),
        invalidArgumentValues = listOf(listOf(testBaseDir.resolve("path/with${File.pathSeparator}separator"))),
        runsNullableTest = true,
        valueString = { value -> value?.joinToString(File.pathSeparator) { it.toFile().absolutePath } },
        expectedArgumentStringsFor = { value -> listOf("-Xklib=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xjava-source-roots",
        argument = X_JAVA_SOURCE_ROOTS,
        argumentValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/java/src1"),
                testBaseDir.resolve("path/to/java/src2"),
                testBaseDir.resolve("path/to/java/src3"),
            ),
            listOf(testBaseDir.resolve("path/with,comma"))
        ),
        argumentRawValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/java/src1"),
                testBaseDir.resolve("path/to/java/src2"),
                testBaseDir.resolve("path/to/java/src3"),
            ).joinToString(",") { it.toFile().absolutePath }
        ),
        valueString = { value -> value?.joinToString(",") { it.toFile().absolutePath } },
        expectedArgumentStringsFor = { value -> listOf("-Xjava-source-roots=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xignored-annotations-for-bridges",
        argument = X_IGNORED_ANNOTATIONS_FOR_BRIDGES,
        argumentValues = listOf(listOf("com.example.MyAnnotation", "*")),
        argumentRawValues = listOf(listOf("com.example.MyAnnotation", "*").joinToString(",")),
        skipBtaV1 = true,
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xignored-annotations-for-bridges=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xscript-resolver-environment",
        argument = X_SCRIPT_RESOLVER_ENVIRONMENT,
        argumentValues = listOf(
            listOf("key1=value1", "key2=value2"),
            listOf("optional="),
        ),
        argumentRawValues = listOf(
            listOf("key1=value1", "key2=value2").joinToString(","),
            "optional=",
        ),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xscript-resolver-environment=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xjsr305",
        argument = X_JSR305,
        argumentValues = listOf(
            listOf(
                Jsr305.Global(Mode.STRICT),
                Jsr305.UnderMigration(Mode.WARN),
                Jsr305.SpecificAnnotation("com.example.Nullable", Mode.IGNORE),
            )
        ),
        argumentRawValues = listOf(
            listOf(
                Jsr305.Global(Mode.STRICT),
                Jsr305.UnderMigration(Mode.WARN),
                Jsr305.SpecificAnnotation("com.example.Nullable", Mode.IGNORE),
            ).joinToString(",") { item ->
                when (item) {
                    is Jsr305.Global -> item.mode.stringValue
                    is Jsr305.UnderMigration -> "under-migration:${item.mode.stringValue}"
                    is Jsr305.SpecificAnnotation -> "${item.annotationFqName}:${item.mode.stringValue}"
                }
            }
        ),
        invalidRawValues = listOf(
            "non-existent-mode",
            "under-migration=warn",
            "foo:bar:baz",
        ),
        valueString = { value ->
            value?.joinToString(",") { item ->
                when (item) {
                    is Jsr305.Global -> item.mode.stringValue
                    is Jsr305.UnderMigration -> "under-migration:${item.mode.stringValue}"
                    is Jsr305.SpecificAnnotation -> "${item.annotationFqName}:${item.mode.stringValue}"
                }
            }
        },
        expectedArgumentStringsFor = { value -> listOf("-Xjsr305=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xnullability-annotations",
        argument = X_NULLABILITY_ANNOTATIONS,
        argumentValues = listOf(
            listOf(
                NullabilityAnnotation("javax.annotation.Nullable", NullabilityAnnotation.Mode.STRICT),
                NullabilityAnnotation("javax.annotation.Nonnull", NullabilityAnnotation.Mode.WARN),
            )
        ),
        argumentRawValues = listOf(
            listOf(
                NullabilityAnnotation("javax.annotation.Nullable", NullabilityAnnotation.Mode.STRICT),
                NullabilityAnnotation("javax.annotation.Nonnull", NullabilityAnnotation.Mode.WARN),
            ).joinToString(",") { "${it.annotationFqName}:${it.mode.stringValue}" }
        ),
        invalidRawValues = listOf(
            "@javax.annotation.Nullable:bogus",
            "@javax.annotation.Nullable=ignore",
            "@javax.annotation.Nullable"
        ),
        valueString = { value -> value?.joinToString(",") { "${it.annotationFqName}:${it.mode.stringValue}" } },
        expectedArgumentStringsFor = { value -> listOf("-Xnullability-annotations=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xprofile",
        argument = X_PROFILE,
        argumentValues = listOf(
            ProfileCompilerCommand(
                profilerPath = testBaseDir.resolve("path/to/libasyncProfiler.so"),
                command = "event=cpu,interval=1ms,threads,start",
                outputDir = testBaseDir.resolve("/path/to/snapshots")
            )
        ),
        argumentRawValues = listOf(
            ProfileCompilerCommand(
                profilerPath = testBaseDir.resolve("path/to/libasyncProfiler.so"),
                command = "event=cpu,interval=1ms,threads,start",
                outputDir = testBaseDir.resolve("/path/to/snapshots")
            ).let { it.profilerPath.toFile().absolutePath + File.pathSeparator + it.command + File.pathSeparator + it.outputDir.toFile().absolutePath }
        ),
        invalidRawValues = listOf("path/to/libasyncProfiler.so"),
        runsNullableTest = true,
        valueString = { value ->
            value?.let { value.profilerPath.toFile().absolutePath + File.pathSeparator + value.command + File.pathSeparator + value.outputDir.toFile().absolutePath }
        },
        expectedArgumentStringsFor = { value -> listOf("-Xprofile=$value") },
    ),
)
