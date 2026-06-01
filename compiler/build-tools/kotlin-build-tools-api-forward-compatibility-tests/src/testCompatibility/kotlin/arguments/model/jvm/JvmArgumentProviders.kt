/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments.model.jvm

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
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
import org.jetbrains.kotlin.buildtools.tests.toolchain
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

internal class AllJvmCompilerArgumentsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration().map { Arguments.of(it) }.stream()
    }
}

internal class InvalidArgumentValueJvmCompilerArgumentsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.runsInvalidArgumentValueTest }.map { Arguments.of(it) }.stream()
    }
}

internal class InvalidRawValueJvmCompilerArgumentsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.runsInvalidRawValueTest }.map { Arguments.of(it) }.stream()
    }
}

internal class InvalidRawValueJvmCompilerArgumentsStrategyAgnosticArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedInvalidRawValueStrategicArgumentConfigurations().map { Arguments.of(it) }.stream()
    }
}

private fun namedInvalidRawValueStrategicArgumentConfigurations(): List<Named<Pair<JvmArgumentConfiguration<*>, ExecutionPolicy>>> {
    val strategies: List<Named<ExecutionPolicy>> = [
        named("[in-process]", toolchain.createInProcessExecutionPolicy()),
        named("[daemon]", toolchain.daemonExecutionPolicyBuilder().build()),
    ]
    val compilerArguments = namedArgumentConfiguration { it.runsInvalidRawValueTest }
    return strategies.flatMap { namedStrategy ->
        compilerArguments.map { namedArgConfig ->
            named(
                namedArgConfig.name + namedStrategy.name,
                namedArgConfig.payload to namedStrategy.payload
            )
        }
    }
}

private fun namedArgumentConfiguration(
    argumentPredicate: (JvmArgumentTestDescriptor<*>) -> Boolean = { true },
): List<Named<JvmArgumentConfiguration<*>>> {
    return jvmArgumentTestDescriptors
        .filter { argumentPredicate(it) }
        .map { named("[${it.argumentName}]", JvmArgumentConfiguration(it)) }
}

private val testBaseDir: Path = Paths.get("").toAbsolutePath()

@OptIn(ExperimentalCompilerArgument::class)
private val jvmArgumentTestDescriptors: List<JvmArgumentTestDescriptor<*>> = [
    JvmArgumentTestDescriptor(
        argumentName = "Xabi-stability",
        argument = X_ABI_STABILITY,
        argumentValues = ["stable", "unstable"],
        argumentRawValues = ["stable", "unstable"],
        invalidArgumentValues = ["non-existent-value"],
        invalidRawValues = ["non-existent-value"],
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> ["-Xabi-stability=$value"] },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xadd-modules",
        argument = X_ADD_MODULES,
        argumentValues = [["module1", "module2", "module3"]],
        argumentRawValues = [arrayOf("module1", "module2", "module3").joinToString(",")],
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> ["-Xadd-modules=$value"] },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xassertions",
        argument = X_ASSERTIONS,
        argumentValues = ["always-enable", "always-disable", "jvm", "legacy"],
        argumentRawValues = ["always-enable", "always-disable", "jvm", "legacy"],
        invalidArgumentValues = ["non-existent-value"],
        invalidRawValues = ["non-existent-value"],
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> ["-Xassertions=$value"] },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "classpath",
        argument = CLASSPATH,
        argumentValues = [
            listOf(
                testBaseDir.resolve("path/to/lib1.jar"),
                testBaseDir.resolve("path/to/lib2.jar"),
                testBaseDir.resolve("path/to/classes"),
            ).joinToString(File.pathSeparator) { it.toFile().absolutePath }
        ],
        argumentRawValues = [
            listOf(
                testBaseDir.resolve("path/to/lib1.jar"),
                testBaseDir.resolve("path/to/lib2.jar"),
                testBaseDir.resolve("path/to/classes"),
            ).joinToString(File.pathSeparator) { it.toFile().absolutePath }
        ],
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> ["-classpath", value] },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "jdk-home",
        argument = JDK_HOME,
        argumentValues = [testBaseDir.resolve("path/to/jdk").toFile().absolutePath],
        argumentRawValues = [testBaseDir.resolve("path/to/jdk").toFile().absolutePath],
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> ["-jdk-home", value] },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "jvm-default",
        argument = JVM_DEFAULT,
        argumentValues = ["enable", "no-compatibility", "disable"],
        argumentRawValues = ["enable", "no-compatibility", "disable"],
        invalidArgumentValues = ["non-existent-value"],
        invalidRawValues = ["non-existent-value"],
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> ["-jvm-default", value] },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "script-templates",
        argument = SCRIPT_TEMPLATES,
        argumentValues = [["org.example.Template1", "org.example.Template2"]],
        argumentRawValues = [arrayOf("org.example.Template1", "org.example.Template2").joinToString(",")],
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> ["-script-templates", value] },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xfriend-paths",
        argument = X_FRIEND_PATHS,
        argumentValues = [
            [
                testBaseDir.resolve("path/to/friend1").toFile().absolutePath,
                testBaseDir.resolve("path/to/friend2").toFile().absolutePath,
                testBaseDir.resolve("path/to/friend3").toFile().absolutePath,
            ],
            [testBaseDir.resolve("path/with,comma").toFile().absolutePath]
        ],
        argumentRawValues = [
            arrayOf(
                testBaseDir.resolve("path/to/friend1").toFile().absolutePath,
                testBaseDir.resolve("path/to/friend2").toFile().absolutePath,
                testBaseDir.resolve("path/to/friend3").toFile().absolutePath,
            ).joinToString(",")
        ],
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> ["-Xfriend-paths=$value"] },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xjdk-release",
        argument = X_JDK_RELEASE,
        argumentValues = [
            "1.6", "1.7", "1.8", "8", "9", "10", "11", "12", "13", "14",
            "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"
        ],
        argumentRawValues = [
            "1.6", "1.7", "1.8", "8", "9", "10", "11", "12", "13", "14",
            "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"
        ],
        invalidArgumentValues = ["non-existent-value"],
        invalidRawValues = ["non-existent-value"],
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> ["-Xjdk-release=$value"] },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xmodule-path",
        argument = X_MODULE_PATH,
        argumentValues = [
            listOf(
                testBaseDir.resolve("path/to/module1"),
                testBaseDir.resolve("path/to/module2"),
                testBaseDir.resolve("path/to/module3"),
            ).joinToString(File.pathSeparator) { it.toFile().absolutePath }
        ],
        argumentRawValues = [
            listOf(
                testBaseDir.resolve("path/to/module1"),
                testBaseDir.resolve("path/to/module2"),
                testBaseDir.resolve("path/to/module3"),
            ).joinToString(File.pathSeparator) { it.toFile().absolutePath }
        ],
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> ["-Xmodule-path=$value"] },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xstring-concat",
        argument = X_STRING_CONCAT,
        argumentValues = ["indy-with-constants", "indy", "inline"],
        argumentRawValues = ["indy-with-constants", "indy", "inline"],
        invalidArgumentValues = ["non-existent-value"],
        invalidRawValues = ["non-existent-value"],
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> ["-Xstring-concat=$value"] },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xlambdas",
        argument = X_LAMBDAS,
        argumentValues = ["indy", "class"],
        argumentRawValues = ["indy", "class"],
        invalidArgumentValues = ["non-existent-value"],
        invalidRawValues = ["non-existent-value"],
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> ["-Xlambdas=$value"] },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xwhen-expressions",
        argument = X_WHEN_EXPRESSIONS,
        argumentValues = ["indy", "inline"],
        argumentRawValues = ["indy", "inline"],
        invalidArgumentValues = ["non-existent-value"],
        invalidRawValues = ["non-existent-value"],
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> ["-Xwhen-expressions=$value"] },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xsam-conversions",
        argument = X_SAM_CONVERSIONS,
        argumentValues = ["class", "indy"],
        argumentRawValues = ["class", "indy"],
        invalidArgumentValues = ["non-existent-value"],
        invalidRawValues = ["non-existent-value"],
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> ["-Xsam-conversions=$value"] },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xjspecify-annotations",
        argument = X_JSPECIFY_ANNOTATIONS,
        argumentValues = ["ignore", "strict", "warn"],
        argumentRawValues = ["ignore", "strict", "warn"],
        invalidArgumentValues = ["non-existent-value"],
        invalidRawValues = ["non-existent-value"],
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> ["-Xjspecify-annotations=$value"] },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xsupport-compatqual-checker-framework-annotations",
        argument = X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS,
        argumentValues = ["enable", "disable"],
        argumentRawValues = ["enable", "disable"],
        invalidArgumentValues = ["non-existent-value"],
        invalidRawValues = ["non-existent-value"],
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> ["-Xsupport-compatqual-checker-framework-annotations=$value"] },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xklib",
        argument = X_KLIB,
        argumentValues = [
            listOf(
                testBaseDir.resolve("path/to/lib1.klib"),
                testBaseDir.resolve("path/to/lib2.klib"),
                testBaseDir.resolve("path/to/lib3.klib"),
            ).joinToString(File.pathSeparator) { it.toFile().absolutePath }
        ],
        argumentRawValues = [
            listOf(
                testBaseDir.resolve("path/to/lib1.klib"),
                testBaseDir.resolve("path/to/lib2.klib"),
                testBaseDir.resolve("path/to/lib3.klib"),
            ).joinToString(File.pathSeparator) { it.toFile().absolutePath }
        ],
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> ["-Xklib=$value"] },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xjava-source-roots",
        argument = X_JAVA_SOURCE_ROOTS,
        argumentValues = [
            [
                testBaseDir.resolve("path/to/java/src1").toFile().absolutePath,
                testBaseDir.resolve("path/to/java/src2").toFile().absolutePath,
                testBaseDir.resolve("path/to/java/src3").toFile().absolutePath,
            ],
            [testBaseDir.resolve("path/with,comma").toFile().absolutePath]
        ],
        argumentRawValues = [
            arrayOf(
                testBaseDir.resolve("path/to/java/src1").toFile().absolutePath,
                testBaseDir.resolve("path/to/java/src2").toFile().absolutePath,
                testBaseDir.resolve("path/to/java/src3").toFile().absolutePath,
            ).joinToString(",")
        ],
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> ["-Xjava-source-roots=$value"] },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xscript-resolver-environment",
        argument = X_SCRIPT_RESOLVER_ENVIRONMENT,
        argumentValues = [
            ["key1=value1", "key2=value2"],
            ["optional="],
        ],
        argumentRawValues = [
            arrayOf("key1=value1", "key2=value2").joinToString(","),
            "optional=",
        ],
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> ["-Xscript-resolver-environment=$value"] },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xjsr305",
        argument = X_JSR305,
        argumentValues = [["strict", "under-migration:warn", "@com.example.Nullable:ignore"]],
        argumentRawValues = [arrayOf("strict", "under-migration:warn", "@com.example.Nullable:ignore").joinToString(",")],
        invalidArgumentValues = [
            ["stict"],
            ["@javax.annotation.Nullable:strct"],
            ["foo:bar:baz"],
        ],
        invalidRawValues = [
            "stict",
            "@javax.annotation.Nullable:strct",
            "foo:bar:baz",
        ],
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> ["-Xjsr305=$value"] },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xnullability-annotations",
        argument = X_NULLABILITY_ANNOTATIONS,
        argumentValues = [["@javax.annotation.Nullable:strict", "@javax.annotation.Nonnull:warn"]],
        argumentRawValues = [arrayOf("@javax.annotation.Nullable:strict", "@javax.annotation.Nonnull:warn").joinToString(",")],
        invalidArgumentValues = [
            ["@javax.annotation.Nullable:bogus"],
            ["@javax.annotation.Nullable"],
            ["@javax.annotation.Nullable=ignore"]
        ],
        invalidRawValues = [
            "@javax.annotation.Nullable:bogus",
            "@javax.annotation.Nullable",
            "@javax.annotation.Nullable=ignore"
        ],
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> ["-Xnullability-annotations=$value"] },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xprofile",
        argument = X_PROFILE,
        argumentValues = [
            testBaseDir.resolve("path/to/libasyncProfiler.so").toFile().absolutePath +
                    File.pathSeparator + "event=cpu,interval=1ms,threads,start" +
                    File.pathSeparator + testBaseDir.resolve("path/to/snapshots").toFile().absolutePath
        ],
        argumentRawValues = [
            testBaseDir.resolve("path/to/libasyncProfiler.so").toFile().absolutePath +
                    File.pathSeparator + "event=cpu,interval=1ms,threads,start" +
                    File.pathSeparator + testBaseDir.resolve("path/to/snapshots").toFile().absolutePath
        ],
        invalidArgumentValues = ["path/to/libasyncProfiler.so"],
        invalidRawValues = ["path/to/libasyncProfiler.so"],
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> ["-Xprofile=$value"] },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xignored-annotations-for-bridges",
        argument = X_IGNORED_ANNOTATIONS_FOR_BRIDGES,
        argumentValues = [
            ["com.example.MyAnnotation", "*"]
        ],
        argumentRawValues = [
            arrayOf("com.example.MyAnnotation", "*").joinToString(",")
        ],
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> ["-Xignored-annotations-for-bridges=$value"] },
    ),
]
