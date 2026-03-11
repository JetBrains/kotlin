/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.CommonCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.enums.*
import org.jetbrains.kotlin.buildtools.api.arguments.types.ProfileCompilerCommand
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.reflect.full.functions

/**
 * Handles forward compatibility when the API version is older than the implementation version.
 * 
 * Converts between old API argument types (e.g., `String`) and new implementation types (e.g., `Path`)
 * to maintain compatibility when argument type definitions evolve between API and implementation.
 */
internal interface CommonToolArgumentValueAdapter {
    fun <V, T> mapFrom(value: T, key: CommonToolArguments.CommonToolArgument<V>): V
    fun <T, V> mapTo(value: V, key: CommonToolArguments.CommonToolArgument<V>): T
}

internal interface CommonCompilerArgumentValueAdapter : CommonToolArgumentValueAdapter {
    fun <V, T> mapFrom(value: T, key: CommonCompilerArgument<V>): V
    fun <T, V> mapTo(value: V, key: CommonCompilerArgument<V>): T
}

internal interface JvmCompilerArgumentValueAdapter : CommonCompilerArgumentValueAdapter {
    fun <V, T> mapFrom(value: T, key: JvmCompilerArguments.JvmCompilerArgument<V>): V
    fun <T, V> mapTo(value: V, key: JvmCompilerArguments.JvmCompilerArgument<V>): T

    companion object {
        //TODO(KT-84598): Expose API Version via Public Property
        private val requiresPre240ForwardCompatibility: Boolean =
            JvmPlatformToolchain::class.functions.none { it.name == "discoverScriptExtensionsOperationBuilder" }

        @Suppress("UNCHECKED_CAST")
        fun getOrNull(): JvmCompilerArgumentValueAdapter? =
            if (requiresPre240ForwardCompatibility) {
                JvmCompilerArgumentPre2_4_0ValueAdapter
            } else {
                null
            }
    }
}

@Suppress("ClassName", "UNCHECKED_CAST")
private abstract class CommonToolArgumentPre2_4_0ValueAdapter : CommonToolArgumentValueAdapter {
    override fun <V, T> mapFrom(value: T, key: CommonToolArguments.CommonToolArgument<V>): V {
        return value as V
    }

    override fun <T, V> mapTo(value: V, key: CommonToolArguments.CommonToolArgument<V>): T {
        return value as T
    }
}

@OptIn(ExperimentalCompilerArgument::class)
@Suppress("ClassName", "UNCHECKED_CAST")
private abstract class CommonCompilerArgumentPre2_4_0ValueAdapter : CommonToolArgumentPre2_4_0ValueAdapter(),
    CommonCompilerArgumentValueAdapter {
    override fun <V, T> mapFrom(value: T, key: CommonCompilerArgument<V>): V {
        if (value == null) return null as V

        return when (key) {
            CommonCompilerArguments.KOTLIN_HOME -> {
                val pathValue = value as Path
                pathValue.absolutePathStringOrThrow() as V
            }

            CommonCompilerArguments.X_PHASES_TO_DUMP -> {
                val listValue: List<String> = value as List<String>
                listValue.toTypedArray() as V
            }

            CommonCompilerArguments.X_PHASES_TO_DUMP_BEFORE -> {
                val listValue: List<String> = value as List<String>
                listValue.toTypedArray() as V
            }

            CommonCompilerArguments.X_PHASES_TO_DUMP_AFTER -> {
                val listValue: List<String> = value as List<String>
                listValue.toTypedArray() as V
            }

            CommonCompilerArguments.X_PHASES_TO_VALIDATE -> {
                val listValue: List<String> = value as List<String>
                listValue.toTypedArray() as V
            }

            CommonCompilerArguments.X_PHASES_TO_VALIDATE_BEFORE -> {
                val listValue: List<String> = value as List<String>
                listValue.toTypedArray() as V
            }

            CommonCompilerArguments.X_PHASES_TO_VALIDATE_AFTER -> {
                val listValue: List<String> = value as List<String>
                listValue.toTypedArray() as V
            }

            else -> {
                value as V
            }
        }
    }

    override fun <T, V> mapTo(value: V, key: CommonCompilerArgument<V>): T {
        if (value == null) return null as T

        return when (key) {
            CommonCompilerArguments.KOTLIN_HOME -> {
                val stringValue = value as String
                Path(stringValue) as T
            }

            CommonCompilerArguments.X_PHASES_TO_DUMP -> {
                val arrayValue = value as Array<String>
                arrayValue.toList() as T
            }

            CommonCompilerArguments.X_PHASES_TO_DUMP_BEFORE -> {
                val arrayValue = value as Array<String>
                arrayValue.toList() as T
            }

            CommonCompilerArguments.X_PHASES_TO_DUMP_AFTER -> {
                val arrayValue = value as Array<String>
                arrayValue.toList() as T
            }

            CommonCompilerArguments.X_PHASES_TO_VALIDATE -> {
                val arrayValue = value as Array<String>
                arrayValue.toList() as T
            }

            CommonCompilerArguments.X_PHASES_TO_VALIDATE_BEFORE -> {
                val arrayValue = value as Array<String>
                arrayValue.toList() as T
            }

            CommonCompilerArguments.X_PHASES_TO_VALIDATE_AFTER -> {
                val arrayValue = value as Array<String>
                arrayValue.toList() as T
            }

            else -> {
                value as T
            }
        }
    }
}

@Suppress("ClassName", "UNCHECKED_CAST")
@OptIn(ExperimentalCompilerArgument::class)
private object JvmCompilerArgumentPre2_4_0ValueAdapter : CommonCompilerArgumentPre2_4_0ValueAdapter(), JvmCompilerArgumentValueAdapter {

    override fun <V, T> mapFrom(
        value: T,
        key: JvmCompilerArguments.JvmCompilerArgument<V>,
    ): V {
        if (value == null) return null as V
        return when (key) {
            JvmCompilerArguments.JDK_HOME -> {
                val pathValue = value as Path
                pathValue.absolutePathStringOrThrow() as V
            }

            JvmCompilerArguments.X_PROFILE -> {
                val profileCompilerCommand = value as ProfileCompilerCommand
                with(profileCompilerCommand) {
                    profilerPath.absolutePathStringOrThrow() +
                            "${File.pathSeparator}${command}" +
                            "${File.pathSeparator}" +
                            outputDir.absolutePathStringOrThrow()
                } as V
            }

            JvmCompilerArguments.JVM_DEFAULT -> {
                val mode = value as JvmDefaultMode
                mode.stringValue as V
            }

            JvmCompilerArguments.X_ABI_STABILITY -> {
                val mode = value as AbiStabilityMode
                mode.stringValue as V
            }

            JvmCompilerArguments.X_ASSERTIONS -> {
                val mode = value as AssertionsMode
                mode.stringValue as V
            }

            JvmCompilerArguments.X_JSPECIFY_ANNOTATIONS -> {
                val mode = value as JspecifyAnnotationsMode
                mode.stringValue as V
            }

            JvmCompilerArguments.X_LAMBDAS -> {
                val mode = value as LambdasMode
                mode.stringValue as V
            }

            JvmCompilerArguments.X_SAM_CONVERSIONS -> {
                val mode = value as SamConversionsMode
                mode.stringValue as V
            }

            JvmCompilerArguments.X_STRING_CONCAT -> {
                val mode = value as StringConcatMode
                mode.stringValue as V
            }

            JvmCompilerArguments.X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS -> {
                val mode = value as CompatqualAnnotationsMode
                mode.stringValue as V
            }

            JvmCompilerArguments.X_WHEN_EXPRESSIONS -> {
                val mode = value as WhenExpressionsMode
                mode.stringValue as V
            }

            JvmCompilerArguments.X_JDK_RELEASE -> {
                val mode = value as JdkRelease
                mode.stringValue as V
            }

            JvmCompilerArguments.X_ADD_MODULES -> {
                val listValue: List<String> = value as List<String>
                listValue.toTypedArray() as V
            }

            JvmCompilerArguments.CLASSPATH -> {
                val listValue = value as List<Path>
                listValue.joinToString(File.pathSeparator) { it.absolutePathStringOrThrow() } as V
            }

            JvmCompilerArguments.X_KLIB -> {
                val listValue = value as List<Path>
                listValue.joinToString(File.pathSeparator) { it.absolutePathStringOrThrow() } as V
            }

            JvmCompilerArguments.X_MODULE_PATH -> {
                val listValue = value as List<Path>
                listValue.joinToString(File.pathSeparator) { it.absolutePathStringOrThrow() } as V
            }

            JvmCompilerArguments.X_FRIEND_PATHS -> {
                val listValue = value as List<Path>
                listValue.map { it.absolutePathStringOrThrow() }.toTypedArray() as V
            }

            JvmCompilerArguments.X_JAVA_SOURCE_ROOTS -> {
                val listValue = value as List<Path>
                listValue.map { it.absolutePathStringOrThrow() }.toTypedArray() as V
            }

            JvmCompilerArguments.SCRIPT_TEMPLATES -> {
                val listValue = value as List<String>
                listValue.toTypedArray() as V
            }

            else -> value as V
        }
    }

    override fun <T, V> mapTo(
        value: V,
        key: JvmCompilerArguments.JvmCompilerArgument<V>,
    ): T {
        if (value == null) return null as T

        return when (key) {
            JvmCompilerArguments.JDK_HOME -> {
                val stringValue = value as String
                Path(stringValue) as T
            }

            JvmCompilerArguments.X_PROFILE -> {
                val stringValue = value as String
                val parts = stringValue.split(File.pathSeparator)
                require(parts.size == 3) { "Invalid async profiler settings format: $stringValue" }

                ProfileCompilerCommand(Path(parts[0]), parts[1], Path(parts[2])) as T
            }

            JvmCompilerArguments.JVM_DEFAULT -> {
                val stringValue = value as String

                JvmDefaultMode.entries.firstOrNull { it.stringValue == stringValue } as T
                    ?: throw CompilerArgumentsParseException("Unknown -jvm-default value: $stringValue")
            }

            JvmCompilerArguments.X_ABI_STABILITY -> {
                val stringValue = value as String

                AbiStabilityMode.entries.firstOrNull { it.stringValue == stringValue } as T
                    ?: throw CompilerArgumentsParseException("Unknown -Xabi-stability value: $stringValue")
            }

            JvmCompilerArguments.X_ASSERTIONS -> {
                val stringValue = value as String

                AssertionsMode.entries.firstOrNull { it.stringValue == stringValue } as T
                    ?: throw CompilerArgumentsParseException("Unknown -Xassertions value: $stringValue")
            }

            JvmCompilerArguments.X_JSPECIFY_ANNOTATIONS -> {
                val stringValue = value as String

                JspecifyAnnotationsMode.entries.firstOrNull { it.stringValue == stringValue } as T
                    ?: throw CompilerArgumentsParseException("Unknown -Xjspecify-annotations value: $stringValue")
            }

            JvmCompilerArguments.X_LAMBDAS -> {
                val stringValue = value as String

                LambdasMode.entries.firstOrNull { it.stringValue == stringValue } as T
                    ?: throw CompilerArgumentsParseException("Unknown -Xlambdas value: $stringValue")
            }

            JvmCompilerArguments.X_SAM_CONVERSIONS -> {
                val stringValue = value as String

                SamConversionsMode.entries.firstOrNull { it.stringValue == stringValue } as T
                    ?: throw CompilerArgumentsParseException("Unknown -Xsam-conversions value: $stringValue")
            }

            JvmCompilerArguments.X_STRING_CONCAT -> {
                val stringValue = value as String

                StringConcatMode.entries.firstOrNull { it.stringValue == stringValue } as T
                    ?: throw CompilerArgumentsParseException("Unknown -Xstring-concat value: $stringValue")
            }

            JvmCompilerArguments.X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS -> {
                val stringValue = value as String

                CompatqualAnnotationsMode.entries.firstOrNull { it.stringValue == stringValue } as T
                    ?: throw CompilerArgumentsParseException("Unknown -Xsupport-compatqual-checker-framework-annotations value: $stringValue")
            }

            JvmCompilerArguments.X_WHEN_EXPRESSIONS -> {
                val stringValue = value as String

                WhenExpressionsMode.entries.firstOrNull { it.stringValue == stringValue } as T
                    ?: throw CompilerArgumentsParseException("Unknown -Xwhen-expressions value: $stringValue")
            }

            JvmCompilerArguments.X_JDK_RELEASE -> {
                val stringValue = value as String

                JdkRelease.entries.firstOrNull { it.stringValue == stringValue } as T
                    ?: throw CompilerArgumentsParseException("Unknown -Xjdk-release value: $stringValue")
            }

            JvmCompilerArguments.X_ADD_MODULES -> {
                val arrayValue = value as Array<String>
                arrayValue.toList() as T
            }

            JvmCompilerArguments.CLASSPATH -> {
                val stringValue = value as String
                stringValue.split(File.pathSeparator).map { Path(it) } as T
            }

            JvmCompilerArguments.X_KLIB -> {
                val stringValue = value as String
                stringValue.split(File.pathSeparator).map { Path(it) } as T
            }

            JvmCompilerArguments.X_MODULE_PATH -> {
                val stringValue = value as String
                stringValue.split(File.pathSeparator).map { Path(it) } as T
            }

            JvmCompilerArguments.X_FRIEND_PATHS -> {
                val arrayValue = value as Array<String>
                arrayValue.map { Path(it) } as T
            }

            JvmCompilerArguments.X_JAVA_SOURCE_ROOTS -> {
                val arrayValue = value as Array<String>
                arrayValue.map { Path(it) } as T
            }

            JvmCompilerArguments.SCRIPT_TEMPLATES -> {
                val arrayValue = value as Array<String>
                arrayValue.toList() as T
            }

            else -> value as T
        }
    }
}