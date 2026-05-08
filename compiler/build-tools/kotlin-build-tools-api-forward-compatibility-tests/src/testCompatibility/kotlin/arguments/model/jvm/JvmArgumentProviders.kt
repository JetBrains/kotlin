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
    val strategies: List<Named<ExecutionPolicy>> = listOf(
        named("[in-process]", toolchain.createInProcessExecutionPolicy()),
        named("[daemon]", toolchain.daemonExecutionPolicyBuilder().build()),
    )
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
private val jvmArgumentTestDescriptors: List<JvmArgumentTestDescriptor<*>> = listOf(
    JvmArgumentTestDescriptor(
        argumentName = "Xabi-stability",
        argument = X_ABI_STABILITY,
        argumentValues = listOf("stable", "unstable"),
        argumentRawValues = listOf("stable", "unstable"),
        invalidArgumentValues = listOf("non-existent-value"),
        invalidRawValues = listOf("non-existent-value"),
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> listOf("-Xabi-stability=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xadd-modules",
        argument = X_ADD_MODULES,
        argumentValues = listOf(arrayOf("module1", "module2", "module3")),
        argumentRawValues = listOf(arrayOf("module1", "module2", "module3").joinToString(",")),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xadd-modules=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xassertions",
        argument = X_ASSERTIONS,
        argumentValues = listOf("always-enable", "always-disable", "jvm", "legacy"),
        argumentRawValues = listOf("always-enable", "always-disable", "jvm", "legacy"),
        invalidArgumentValues = listOf("non-existent-value"),
        invalidRawValues = listOf("non-existent-value"),
        valueString = { value -> value },
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
            ).joinToString(File.pathSeparator) { it.toFile().absolutePath }
        ),
        argumentRawValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/lib1.jar"),
                testBaseDir.resolve("path/to/lib2.jar"),
                testBaseDir.resolve("path/to/classes"),
            ).joinToString(File.pathSeparator) { it.toFile().absolutePath }
        ),
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> listOf("-classpath", value) },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "jdk-home",
        argument = JDK_HOME,
        argumentValues = listOf(testBaseDir.resolve("path/to/jdk").toFile().absolutePath),
        argumentRawValues = listOf(testBaseDir.resolve("path/to/jdk").toFile().absolutePath),
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> listOf("-jdk-home", value) },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "jvm-default",
        argument = JVM_DEFAULT,
        argumentValues = listOf("enable", "no-compatibility", "disable"),
        argumentRawValues = listOf("enable", "no-compatibility", "disable"),
        invalidArgumentValues = listOf("non-existent-value"),
        invalidRawValues = listOf("non-existent-value"),
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> listOf("-jvm-default", value) },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "script-templates",
        argument = SCRIPT_TEMPLATES,
        argumentValues = listOf(arrayOf("org.example.Template1", "org.example.Template2")),
        argumentRawValues = listOf(arrayOf("org.example.Template1", "org.example.Template2").joinToString(",")),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-script-templates", value) },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xfriend-paths",
        argument = X_FRIEND_PATHS,
        argumentValues = listOf(
            arrayOf(
                testBaseDir.resolve("path/to/friend1").toFile().absolutePath,
                testBaseDir.resolve("path/to/friend2").toFile().absolutePath,
                testBaseDir.resolve("path/to/friend3").toFile().absolutePath,
            ),
            arrayOf(testBaseDir.resolve("path/with,comma").toFile().absolutePath)
        ),
        argumentRawValues = listOf(
            arrayOf(
                testBaseDir.resolve("path/to/friend1").toFile().absolutePath,
                testBaseDir.resolve("path/to/friend2").toFile().absolutePath,
                testBaseDir.resolve("path/to/friend3").toFile().absolutePath,
            ).joinToString(",")
        ),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xfriend-paths=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xjdk-release",
        argument = X_JDK_RELEASE,
        argumentValues = listOf(
            "1.6", "1.7", "1.8", "8", "9", "10", "11", "12", "13", "14",
            "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"
        ),
        argumentRawValues = listOf(
            "1.6", "1.7", "1.8", "8", "9", "10", "11", "12", "13", "14",
            "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"
        ),
        invalidArgumentValues = listOf("non-existent-value"),
        invalidRawValues = listOf("non-existent-value"),
        valueString = { value -> value },
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
            ).joinToString(File.pathSeparator) { it.toFile().absolutePath }
        ),
        argumentRawValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/module1"),
                testBaseDir.resolve("path/to/module2"),
                testBaseDir.resolve("path/to/module3"),
            ).joinToString(File.pathSeparator) { it.toFile().absolutePath }
        ),
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> listOf("-Xmodule-path=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xstring-concat",
        argument = X_STRING_CONCAT,
        argumentValues = listOf("indy-with-constants", "indy", "inline"),
        argumentRawValues = listOf("indy-with-constants", "indy", "inline"),
        invalidArgumentValues = listOf("non-existent-value"),
        invalidRawValues = listOf("non-existent-value"),
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> listOf("-Xstring-concat=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xlambdas",
        argument = X_LAMBDAS,
        argumentValues = listOf("indy", "class"),
        argumentRawValues = listOf("indy", "class"),
        invalidArgumentValues = listOf("non-existent-value"),
        invalidRawValues = listOf("non-existent-value"),
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> listOf("-Xlambdas=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xwhen-expressions",
        argument = X_WHEN_EXPRESSIONS,
        argumentValues = listOf("indy", "inline"),
        argumentRawValues = listOf("indy", "inline"),
        invalidArgumentValues = listOf("non-existent-value"),
        invalidRawValues = listOf("non-existent-value"),
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> listOf("-Xwhen-expressions=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xsam-conversions",
        argument = X_SAM_CONVERSIONS,
        argumentValues = listOf("class", "indy"),
        argumentRawValues = listOf("class", "indy"),
        invalidArgumentValues = listOf("non-existent-value"),
        invalidRawValues = listOf("non-existent-value"),
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> listOf("-Xsam-conversions=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xjspecify-annotations",
        argument = X_JSPECIFY_ANNOTATIONS,
        argumentValues = listOf("ignore", "strict", "warn"),
        argumentRawValues = listOf("ignore", "strict", "warn"),
        invalidArgumentValues = listOf("non-existent-value"),
        invalidRawValues = listOf("non-existent-value"),
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> listOf("-Xjspecify-annotations=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xsupport-compatqual-checker-framework-annotations",
        argument = X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS,
        argumentValues = listOf("enable", "disable"),
        argumentRawValues = listOf("enable", "disable"),
        invalidArgumentValues = listOf("non-existent-value"),
        invalidRawValues = listOf("non-existent-value"),
        valueString = { value -> value },
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
            ).joinToString(File.pathSeparator) { it.toFile().absolutePath }
        ),
        argumentRawValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/lib1.klib"),
                testBaseDir.resolve("path/to/lib2.klib"),
                testBaseDir.resolve("path/to/lib3.klib"),
            ).joinToString(File.pathSeparator) { it.toFile().absolutePath }
        ),
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> listOf("-Xklib=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xjava-source-roots",
        argument = X_JAVA_SOURCE_ROOTS,
        argumentValues = listOf(
            arrayOf(
                testBaseDir.resolve("path/to/java/src1").toFile().absolutePath,
                testBaseDir.resolve("path/to/java/src2").toFile().absolutePath,
                testBaseDir.resolve("path/to/java/src3").toFile().absolutePath,
            ),
            arrayOf(testBaseDir.resolve("path/with,comma").toFile().absolutePath)
        ),
        argumentRawValues = listOf(
            arrayOf(
                testBaseDir.resolve("path/to/java/src1").toFile().absolutePath,
                testBaseDir.resolve("path/to/java/src2").toFile().absolutePath,
                testBaseDir.resolve("path/to/java/src3").toFile().absolutePath,
            ).joinToString(",")
        ),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xjava-source-roots=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xscript-resolver-environment",
        argument = X_SCRIPT_RESOLVER_ENVIRONMENT,
        argumentValues = listOf(
            arrayOf("key1=value1", "key2=value2"),
            arrayOf("optional="),
        ),
        argumentRawValues = listOf(
            arrayOf("key1=value1", "key2=value2").joinToString(","),
            "optional=",
        ),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xscript-resolver-environment=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xjsr305",
        argument = X_JSR305,
        argumentValues = listOf(arrayOf("strict", "under-migration:warn", "@com.example.Nullable:ignore")),
        argumentRawValues = listOf(arrayOf("strict", "under-migration:warn", "@com.example.Nullable:ignore").joinToString(",")),
        invalidArgumentValues = listOf(
            arrayOf("non-existent-mode"),
            arrayOf("under-migration=warn"),
            arrayOf("foo:bar:baz"),
        ),
        invalidRawValues = listOf(
            "non-existent-mode",
            "under-migration=warn",
            "foo:bar:baz",
        ),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xjsr305=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xnullability-annotations",
        argument = X_NULLABILITY_ANNOTATIONS,
        argumentValues = listOf(arrayOf("@javax.annotation.Nullable:strict", "@javax.annotation.Nonnull:warn")),
        argumentRawValues = listOf(arrayOf("@javax.annotation.Nullable:strict", "@javax.annotation.Nonnull:warn").joinToString(",")),
        invalidArgumentValues = listOf(
            arrayOf("@javax.annotation.Nullable:bogus"),
            arrayOf("@javax.annotation.Nullable"),
            arrayOf("@javax.annotation.Nullable=ignore")
        ),
        invalidRawValues = listOf(
            "@javax.annotation.Nullable:bogus",
            "@javax.annotation.Nullable",
            "@javax.annotation.Nullable=ignore"
        ),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xnullability-annotations=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xprofile",
        argument = X_PROFILE,
        argumentValues = listOf(
            testBaseDir.resolve("path/to/libasyncProfiler.so").toFile().absolutePath +
                    File.pathSeparator + "event=cpu,interval=1ms,threads,start" +
                    File.pathSeparator + testBaseDir.resolve("path/to/snapshots").toFile().absolutePath
        ),
        argumentRawValues = listOf(
            testBaseDir.resolve("path/to/libasyncProfiler.so").toFile().absolutePath +
                    File.pathSeparator + "event=cpu,interval=1ms,threads,start" +
                    File.pathSeparator + testBaseDir.resolve("path/to/snapshots").toFile().absolutePath
        ),
        invalidArgumentValues = listOf("path/to/libasyncProfiler.so"),
        invalidRawValues = listOf("path/to/libasyncProfiler.so"),
        valueString = { value -> value },
        expectedArgumentStringsFor = { value -> listOf("-Xprofile=$value") },
    ),
    JvmArgumentTestDescriptor(
        argumentName = "Xignored-annotations-for-bridges",
        argument = X_IGNORED_ANNOTATIONS_FOR_BRIDGES,
        argumentValues = listOf(
            arrayOf("com.example.MyAnnotation", "*")
        ),
        argumentRawValues = listOf(
            arrayOf("com.example.MyAnnotation", "*").joinToString(",")
        ),
        valueString = { value -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value -> listOf("-Xignored-annotations-for-bridges=$value") },
    ),
)
