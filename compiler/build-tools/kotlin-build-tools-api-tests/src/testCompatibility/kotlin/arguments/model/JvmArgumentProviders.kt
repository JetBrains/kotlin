/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments.model

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
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_KLIB
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_LAMBDAS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_MODULE_PATH
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_SAM_CONVERSIONS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_SCRIPT_RESOLVER_ENVIRONMENT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_STRING_CONCAT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_WHEN_EXPRESSIONS
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.JvmCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.enums.*
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

internal class EnumJvmCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.isEnum }.map { Arguments.of(it) }.stream()
    }
}

internal class NullableJvmCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.isNullable }.map { Arguments.of(it) }.stream()
    }
}

internal class JvmArgumentDescriptor<T>(
    override val argumentName: String,
    val argumentKey: JvmCompilerArgument<T>,
    override val argumentValues: List<T>,
    override val isEnum: Boolean,
    override val isNullable: Boolean,
    override val valueString: (T?) -> String?,
    override val expectedArgumentStringsFor: (String) -> List<String>,
) : ArgumentDescriptor<T>

private fun namedArgumentConfiguration(argumentPredicate: (JvmArgumentDescriptor<*>) -> Boolean = { true }): List<Named<JvmArgumentConfiguration<*>>> {
    val btaVersions = BtaVersionsCompilationTestArgumentProvider.namedStrategyArguments()
    val compilerArguments = jvmCompilerArguments.filter { argumentPredicate(it) }.map { named("[${it.argumentName}]", it) }

    return btaVersions.flatMap { namedKotlinToolchains ->
        compilerArguments.mapNotNull { namedArgumentDescriptor ->
            // Skip BTAv1 for Xignored-annotations-for-bridges argument
            if (namedArgumentDescriptor.payload.argumentName == "Xignored-annotations-for-bridges" &&
                namedKotlinToolchains.name.contains("[v1]")
            ) {
                return@mapNotNull null
            }

            named(
                namedKotlinToolchains.name + namedArgumentDescriptor.name,
                JvmArgumentConfiguration(namedKotlinToolchains.payload, namedArgumentDescriptor.payload)
            )
        }
    }
}

private val testBaseDir: Path = Paths.get("").toAbsolutePath()

@OptIn(ExperimentalCompilerArgument::class)
private val jvmCompilerArguments: List<JvmArgumentDescriptor<*>> = listOf(
    JvmArgumentDescriptor(
        argumentName = "Xabi-stability",
        argumentKey = X_ABI_STABILITY,
        argumentValues = AbiStabilityMode.entries.toList(),
        isEnum = true,
        isNullable = true,
        valueString = { value: AbiStabilityMode? -> value?.stringValue },
        expectedArgumentStringsFor = { value: String -> listOf("-Xabi-stability=$value") }),
    JvmArgumentDescriptor(
        argumentName = "Xadd-modules",
        argumentKey = X_ADD_MODULES,
        argumentValues = listOf(listOf("module1", "module2", "module3")),
        isEnum = false,
        isNullable = false,
        valueString = { value: List<String>? -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value: String -> listOf("-Xadd-modules=$value") }),
    JvmArgumentDescriptor(
        argumentName = "Xassertions",
        argumentKey = X_ASSERTIONS,
        argumentValues = AssertionsMode.entries.toList(),
        isEnum = true,
        isNullable = true,
        valueString = { value: AssertionsMode? -> value?.stringValue },
        expectedArgumentStringsFor = { value: String -> listOf("-Xassertions=$value") }),
    JvmArgumentDescriptor(
        argumentName = "classpath",
        argumentKey = CLASSPATH,
        argumentValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/lib1.jar"),
                testBaseDir.resolve("path/to/lib2.jar"),
                testBaseDir.resolve("path/to/classes"),
            )
        ),
        isEnum = false,
        isNullable = true,
        valueString = { value: List<Path>? -> value?.joinToString(File.pathSeparator) { it.toFile().absolutePath } },
        expectedArgumentStringsFor = { value: String -> listOf("-classpath", value) }),
    JvmArgumentDescriptor(
        argumentName = "jdk-home",
        argumentKey = JDK_HOME,
        argumentValues = listOf(testBaseDir.resolve("path/to/jdk")),
        isEnum = false,
        isNullable = true,
        valueString = { value: Path? -> value?.toFile()?.absolutePath },
        expectedArgumentStringsFor = { value: String -> listOf("-jdk-home", value) }),
    JvmArgumentDescriptor(
        argumentName = "jvm-default",
        argumentKey = JVM_DEFAULT,
        argumentValues = JvmDefaultMode.entries.toList(),
        isEnum = true,
        isNullable = true,
        valueString = { value: JvmDefaultMode? -> value?.stringValue },
        expectedArgumentStringsFor = { value: String -> listOf("-jvm-default", value) }),
    JvmArgumentDescriptor(
        argumentName = "script-templates",
        argumentKey = SCRIPT_TEMPLATES,
        argumentValues = listOf(listOf("org.example.Template1", "org.example.Template2")),
        isEnum = false,
        isNullable = false,
        valueString = { value: List<String>? -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value: String -> listOf("-script-templates", value) }),
    JvmArgumentDescriptor(
        argumentName = "Xfriend-paths",
        argumentKey = X_FRIEND_PATHS,
        argumentValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/friend1"),
                testBaseDir.resolve("path/to/friend2"),
                testBaseDir.resolve("path/to/friend3"),
            )
        ),
        isEnum = false,
        isNullable = false,
        valueString = { value: List<Path>? -> value?.joinToString(",") { it.toFile().absolutePath } },
        expectedArgumentStringsFor = { value: String -> listOf("-Xfriend-paths=$value") }),
    JvmArgumentDescriptor(
        argumentName = "Xjdk-release",
        argumentKey = X_JDK_RELEASE,
        argumentValues = JdkRelease.entries.toList(),
        isEnum = true,
        isNullable = true,
        valueString = { value: JdkRelease? -> value?.stringValue },
        expectedArgumentStringsFor = { value: String -> listOf("-Xjdk-release=$value") }),
    JvmArgumentDescriptor(
        argumentName = "Xmodule-path",
        argumentKey = X_MODULE_PATH,
        argumentValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/module1"),
                testBaseDir.resolve("path/to/module2"),
                testBaseDir.resolve("path/to/module3"),
            )
        ),
        isEnum = false,
        isNullable = false,
        valueString = { value: List<Path>? -> value?.joinToString(File.pathSeparator) { it.toFile().absolutePath } },
        expectedArgumentStringsFor = { value: String -> listOf("-Xmodule-path=$value") }),
    JvmArgumentDescriptor(
        argumentName = "Xstring-concat",
        argumentKey = X_STRING_CONCAT,
        argumentValues = StringConcatMode.entries.toList(),
        isEnum = true,
        isNullable = true,
        valueString = { value: StringConcatMode? -> value?.stringValue },
        expectedArgumentStringsFor = { value: String -> listOf("-Xstring-concat=$value") }),
    JvmArgumentDescriptor(
        argumentName = "Xlambdas",
        argumentKey = X_LAMBDAS,
        argumentValues = LambdasMode.entries.toList(),
        isEnum = true,
        isNullable = false,
        valueString = { value: LambdasMode? -> value?.stringValue },
        expectedArgumentStringsFor = { value: String -> listOf("-Xlambdas=$value") }),
    JvmArgumentDescriptor(
        argumentName = "Xwhen-expressions",
        argumentKey = X_WHEN_EXPRESSIONS,
        argumentValues = WhenExpressionsMode.entries.toList(),
        isEnum = true,
        isNullable = true,
        valueString = { value: WhenExpressionsMode? -> value?.stringValue },
        expectedArgumentStringsFor = { value: String -> listOf("-Xwhen-expressions=$value") }),
    JvmArgumentDescriptor(
        argumentName = "Xsam-conversions",
        argumentKey = X_SAM_CONVERSIONS,
        argumentValues = SamConversionsMode.entries.toList(),
        isEnum = true,
        isNullable = true,
        valueString = { value: SamConversionsMode? -> value?.stringValue },
        expectedArgumentStringsFor = { value: String -> listOf("-Xsam-conversions=$value") }),
    JvmArgumentDescriptor(
        argumentName = "Xjspecify-annotations",
        argumentKey = X_JSPECIFY_ANNOTATIONS,
        argumentValues = JspecifyAnnotationsMode.entries.toList(),
        isEnum = true,
        isNullable = true,
        valueString = { value: JspecifyAnnotationsMode? -> value?.stringValue },
        expectedArgumentStringsFor = { value: String -> listOf("-Xjspecify-annotations=$value") }),
    JvmArgumentDescriptor(
        argumentName = "Xsupport-compatqual-checker-framework-annotations",
        argumentKey = X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS,
        argumentValues = CompatqualAnnotationsMode.entries.toList(),
        isEnum = true,
        isNullable = true,
        valueString = { value: CompatqualAnnotationsMode? -> value?.stringValue },
        expectedArgumentStringsFor = { value: String -> listOf("-Xsupport-compatqual-checker-framework-annotations=$value") }),
    JvmArgumentDescriptor(
        argumentName = "Xklib",
        argumentKey = X_KLIB,
        argumentValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/lib1.klib"),
                testBaseDir.resolve("path/to/lib2.klib"),
                testBaseDir.resolve("path/to/lib3.klib"),
            )
        ),
        isEnum = false,
        isNullable = false,
        valueString = { value: List<Path>? -> value?.joinToString(File.pathSeparator) { it.toFile().absolutePath } },
        expectedArgumentStringsFor = { value: String -> listOf("-Xklib=$value") }),
    JvmArgumentDescriptor(
        argumentName = "Xjava-source-roots",
        argumentKey = X_JAVA_SOURCE_ROOTS,
        argumentValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/java/src1"),
                testBaseDir.resolve("path/to/java/src2"),
                testBaseDir.resolve("path/to/java/src3"),
            )
        ),
        isEnum = false,
        isNullable = false,
        valueString = { value: List<Path>? -> value?.joinToString(",") { it.toFile().absolutePath } },
        expectedArgumentStringsFor = { value: String -> listOf("-Xjava-source-roots=$value") }),
    JvmArgumentDescriptor(
        argumentName = "Xignored-annotations-for-bridges",
        argumentKey = X_IGNORED_ANNOTATIONS_FOR_BRIDGES,
        argumentValues = listOf(listOf("com.example.MyAnnotation", "*")),
        isEnum = false,
        isNullable = false,
        valueString = { value: List<String>? -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value: String -> listOf("-Xignored-annotations-for-bridges=$value") }),
    JvmArgumentDescriptor(
        argumentName = "Xscript-resolver-environment",
        argumentKey = X_SCRIPT_RESOLVER_ENVIRONMENT,
        argumentValues = listOf(listOf("key1=value1", "key2=value2")),
        isEnum = false,
        isNullable = false,
        valueString = { value: List<String>? -> value?.joinToString(",") },
        expectedArgumentStringsFor = { value: String -> listOf("-Xscript-resolver-environment=$value") }),
)
