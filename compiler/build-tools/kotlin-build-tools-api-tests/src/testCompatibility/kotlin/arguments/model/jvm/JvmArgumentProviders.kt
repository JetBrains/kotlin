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
import org.jetbrains.kotlin.buildtools.tests.arguments.model.ArgumentTestDescriptor
import org.jetbrains.kotlin.buildtools.tests.arguments.model.argumentTestDescriptor
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
        return namedArgumentConfiguration { it.runsEnumTest }.map { Arguments.of(it) }.stream()
    }
}

internal class NullableJvmCompilerArgumentsWithBtaVersionsArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedArgumentConfiguration { it.runsNullableTest }.map { Arguments.of(it) }.stream()
    }
}

private fun namedArgumentConfiguration(argumentPredicate: (ArgumentTestDescriptor<*>) -> Boolean = { true }): List<Named<JvmArgumentConfiguration<*>>> {
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
private val jvmCompilerArguments: List<ArgumentTestDescriptor<*>> = listOf(
    argumentTestDescriptor<AbiStabilityMode> {
        argumentName = "Xabi-stability"
        argumentId = X_ABI_STABILITY.id
        availableSinceVersion = X_ABI_STABILITY.availableSinceVersion
        argumentValues = AbiStabilityMode.entries.toList()
        runsEnumTest = true
        runsNullableTest = true
        valueString = { value -> value?.stringValue }
        expectedArgumentStringsFor = { value -> listOf("-Xabi-stability=$value") }
    },
    argumentTestDescriptor<List<String>> {
        argumentName = "Xadd-modules"
        argumentId = X_ADD_MODULES.id
        availableSinceVersion = X_ADD_MODULES.availableSinceVersion
        argumentValues = listOf(listOf("module1", "module2", "module3"))
        valueString = { value -> value?.joinToString(",") }
        expectedArgumentStringsFor = { value -> listOf("-Xadd-modules=$value") }
    },
    argumentTestDescriptor<AssertionsMode> {
        argumentName = "Xassertions"
        argumentId = X_ASSERTIONS.id
        availableSinceVersion = X_ASSERTIONS.availableSinceVersion
        argumentValues = AssertionsMode.entries.toList()
        runsEnumTest = true
        runsNullableTest = true
        valueString = { value -> value?.stringValue }
        expectedArgumentStringsFor = { value -> listOf("-Xassertions=$value") }
    },
    argumentTestDescriptor<List<Path>> {
        argumentName = "classpath"
        argumentId = CLASSPATH.id
        availableSinceVersion = CLASSPATH.availableSinceVersion
        argumentValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/lib1.jar"),
                testBaseDir.resolve("path/to/lib2.jar"),
                testBaseDir.resolve("path/to/classes"),
            )
        )
        runsNullableTest = true
        valueString = { value -> value?.joinToString(File.pathSeparator) { it.toFile().absolutePath } }
        expectedArgumentStringsFor = { value -> listOf("-classpath", value) }
    },
    argumentTestDescriptor<Path> {
        argumentName = "jdk-home"
        argumentId = JDK_HOME.id
        availableSinceVersion = JDK_HOME.availableSinceVersion
        argumentValues = listOf(testBaseDir.resolve("path/to/jdk"))
        runsNullableTest = true
        valueString = { value -> value?.toFile()?.absolutePath }
        expectedArgumentStringsFor = { value -> listOf("-jdk-home", value) }
    },
    argumentTestDescriptor<JvmDefaultMode> {
        argumentName = "jvm-default"
        argumentId = JVM_DEFAULT.id
        availableSinceVersion = JVM_DEFAULT.availableSinceVersion
        argumentValues = JvmDefaultMode.entries.toList()
        runsEnumTest = true
        runsNullableTest = true
        valueString = { value -> value?.stringValue }
        expectedArgumentStringsFor = { value -> listOf("-jvm-default", value) }
    },
    argumentTestDescriptor<List<String>> {
        argumentName = "script-templates"
        argumentId = SCRIPT_TEMPLATES.id
        availableSinceVersion = SCRIPT_TEMPLATES.availableSinceVersion
        argumentValues = listOf(listOf("org.example.Template1", "org.example.Template2"))
        valueString = { value -> value?.joinToString(",") }
        expectedArgumentStringsFor = { value -> listOf("-script-templates", value) }
    },
    argumentTestDescriptor<List<Path>> {
        argumentName = "Xfriend-paths"
        argumentId = X_FRIEND_PATHS.id
        availableSinceVersion = X_FRIEND_PATHS.availableSinceVersion
        argumentValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/friend1"),
                testBaseDir.resolve("path/to/friend2"),
                testBaseDir.resolve("path/to/friend3"),
            )
        )
        valueString = { value -> value?.joinToString(",") { it.toFile().absolutePath } }
        expectedArgumentStringsFor = { value -> listOf("-Xfriend-paths=$value") }
    },
    argumentTestDescriptor<JdkRelease> {
        argumentName = "Xjdk-release"
        argumentId = X_JDK_RELEASE.id
        availableSinceVersion = X_JDK_RELEASE.availableSinceVersion
        argumentValues = JdkRelease.entries.toList()
        runsEnumTest = true
        runsNullableTest = true
        valueString = { value -> value?.stringValue }
        expectedArgumentStringsFor = { value -> listOf("-Xjdk-release=$value") }
    },
    argumentTestDescriptor<List<Path>> {
        argumentName = "Xmodule-path"
        argumentId = X_MODULE_PATH.id
        availableSinceVersion = X_MODULE_PATH.availableSinceVersion
        argumentValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/module1"),
                testBaseDir.resolve("path/to/module2"),
                testBaseDir.resolve("path/to/module3"),
            )
        )
        valueString = { value -> value?.joinToString(File.pathSeparator) { it.toFile().absolutePath } }
        expectedArgumentStringsFor = { value -> listOf("-Xmodule-path=$value") }
    },
    argumentTestDescriptor<StringConcatMode> {
        argumentName = "Xstring-concat"
        argumentId = X_STRING_CONCAT.id
        availableSinceVersion = X_STRING_CONCAT.availableSinceVersion
        argumentValues = StringConcatMode.entries.toList()
        runsEnumTest = true
        runsNullableTest = true
        valueString = { value -> value?.stringValue }
        expectedArgumentStringsFor = { value -> listOf("-Xstring-concat=$value") }
    },
    argumentTestDescriptor<LambdasMode> {
        argumentName = "Xlambdas"
        argumentId = X_LAMBDAS.id
        availableSinceVersion = X_LAMBDAS.availableSinceVersion
        argumentValues = LambdasMode.entries.toList()
        runsEnumTest = true
        valueString = { value -> value?.stringValue }
        expectedArgumentStringsFor = { value -> listOf("-Xlambdas=$value") }
    },
    argumentTestDescriptor<WhenExpressionsMode> {
        argumentName = "Xwhen-expressions"
        argumentId = X_WHEN_EXPRESSIONS.id
        availableSinceVersion = X_WHEN_EXPRESSIONS.availableSinceVersion
        argumentValues = WhenExpressionsMode.entries.toList()
        runsEnumTest = true
        runsNullableTest = true
        valueString = { value -> value?.stringValue }
        expectedArgumentStringsFor = { value -> listOf("-Xwhen-expressions=$value") }
    },
    argumentTestDescriptor<SamConversionsMode> {
        argumentName = "Xsam-conversions"
        argumentId = X_SAM_CONVERSIONS.id
        availableSinceVersion = X_SAM_CONVERSIONS.availableSinceVersion
        argumentValues = SamConversionsMode.entries.toList()
        runsEnumTest = true
        runsNullableTest = true
        valueString = { value -> value?.stringValue }
        expectedArgumentStringsFor = { value -> listOf("-Xsam-conversions=$value") }
    },
    argumentTestDescriptor<JspecifyAnnotationsMode> {
        argumentName = "Xjspecify-annotations"
        argumentId = X_JSPECIFY_ANNOTATIONS.id
        availableSinceVersion = X_JSPECIFY_ANNOTATIONS.availableSinceVersion
        argumentValues = JspecifyAnnotationsMode.entries.toList()
        runsEnumTest = true
        runsNullableTest = true
        valueString = { value -> value?.stringValue }
        expectedArgumentStringsFor = { value -> listOf("-Xjspecify-annotations=$value") }
    },
    argumentTestDescriptor<CompatqualAnnotationsMode> {
        argumentName = "Xsupport-compatqual-checker-framework-annotations"
        argumentId = X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS.id
        availableSinceVersion = X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS.availableSinceVersion
        argumentValues = CompatqualAnnotationsMode.entries.toList()
        runsEnumTest = true
        runsNullableTest = true
        valueString = { value -> value?.stringValue }
        expectedArgumentStringsFor = { value -> listOf("-Xsupport-compatqual-checker-framework-annotations=$value") }
    },
    argumentTestDescriptor<List<Path>> {
        argumentName = "Xklib"
        argumentId = X_KLIB.id
        availableSinceVersion = X_KLIB.availableSinceVersion
        argumentValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/lib1.klib"),
                testBaseDir.resolve("path/to/lib2.klib"),
                testBaseDir.resolve("path/to/lib3.klib"),
            )
        )
        valueString = { value -> value?.joinToString(File.pathSeparator) { it.toFile().absolutePath } }
        expectedArgumentStringsFor = { value -> listOf("-Xklib=$value") }
    },
    argumentTestDescriptor<List<Path>> {
        argumentName = "Xjava-source-roots"
        argumentId = X_JAVA_SOURCE_ROOTS.id
        availableSinceVersion = X_JAVA_SOURCE_ROOTS.availableSinceVersion
        argumentValues = listOf(
            listOf(
                testBaseDir.resolve("path/to/java/src1"),
                testBaseDir.resolve("path/to/java/src2"),
                testBaseDir.resolve("path/to/java/src3"),
            )
        )
        valueString = { value -> value?.joinToString(",") { it.toFile().absolutePath } }
        expectedArgumentStringsFor = { value -> listOf("-Xjava-source-roots=$value") }
    },
    argumentTestDescriptor<List<String>> {
        argumentName = "Xignored-annotations-for-bridges"
        argumentId = X_IGNORED_ANNOTATIONS_FOR_BRIDGES.id
        availableSinceVersion = X_IGNORED_ANNOTATIONS_FOR_BRIDGES.availableSinceVersion
        argumentValues = listOf(listOf("com.example.MyAnnotation", "*"))
        valueString = { value -> value?.joinToString(",") }
        skipBtaV1 = true
        expectedArgumentStringsFor = { value -> listOf("-Xignored-annotations-for-bridges=$value") }
    },
    argumentTestDescriptor<List<String>> {
        argumentName = "Xscript-resolver-environment"
        argumentId = X_SCRIPT_RESOLVER_ENVIRONMENT.id
        availableSinceVersion = X_SCRIPT_RESOLVER_ENVIRONMENT.availableSinceVersion
        argumentValues = listOf(listOf("key1=value1", "key2=value2"))
        valueString = { value -> value?.joinToString(",") }
        expectedArgumentStringsFor = { value -> listOf("-Xscript-resolver-environment=$value") }
    },
    argumentTestDescriptor<List<Jsr305>> {
        argumentName = "Xjsr305"
        argumentId = X_JSR305.id
        availableSinceVersion = X_JSR305.availableSinceVersion
        argumentValues = listOf(
            listOf(
                Jsr305.Global(Mode.STRICT),
                Jsr305.UnderMigration(Mode.WARN),
                Jsr305.SpecificAnnotation("com.example.Nullable", Mode.IGNORE),
            )
        )
        valueString = { value ->
            value?.joinToString(",") { item ->
                when (item) {
                    is Jsr305.Global -> item.mode.stringValue
                    is Jsr305.UnderMigration -> "under-migration:${item.mode.stringValue}"
                    is Jsr305.SpecificAnnotation -> "${item.annotationFqName}:${item.mode.stringValue}"
                }
            }
        }
        expectedArgumentStringsFor = { value -> listOf("-Xjsr305=$value") }
    },
    argumentTestDescriptor<List<NullabilityAnnotation>> {
        argumentName = "Xnullability-annotations"
        argumentId = X_NULLABILITY_ANNOTATIONS.id
        availableSinceVersion = X_NULLABILITY_ANNOTATIONS.availableSinceVersion
        argumentValues = listOf(
            listOf(
                NullabilityAnnotation("javax.annotation.Nullable", NullabilityAnnotation.Mode.STRICT),
                NullabilityAnnotation("javax.annotation.Nonnull", NullabilityAnnotation.Mode.WARN),
            )
        )
        valueString = { value -> value?.joinToString(",") { "${it.annotationFqName}:${it.mode.stringValue}" } }
        expectedArgumentStringsFor = { value -> listOf("-Xnullability-annotations=$value") }
    },

    argumentTestDescriptor<ProfileCompilerCommand> {
        argumentName = "Xprofile"
        argumentId = X_PROFILE.id
        availableSinceVersion = X_PROFILE.availableSinceVersion
        argumentValues = listOf(
            ProfileCompilerCommand(
                profilerPath = testBaseDir.resolve("path/to/libasyncProfiler.so"),
                command = "event=cpu,interval=1ms,threads,start",
                outputDir = testBaseDir.resolve("/path/to/snapshots")
            )
        )
        valueString = { value ->
            value?.let { value.profilerPath.toFile().absolutePath + File.pathSeparator + value.command + File.pathSeparator + value.outputDir.toFile().absolutePath }
        }
        expectedArgumentStringsFor = { value -> listOf("-Xprofile=$value") }
    },
)
