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
    override fun <V, T> mapFrom(value: T, key: CommonCompilerArgument<V>): V =
        when (key) {
            CommonCompilerArguments.KOTLIN_HOME -> {
                if (value == null) return null as V

                val pathValue = value as Path
                pathValue.absolutePathStringOrThrow() as V
            }

            CommonCompilerArguments.X_PHASES_TO_DUMP -> {
                if (value == null) return emptyArray<String>() as V

                val listValue: List<String> = value as List<String>
                listValue.toTypedArray() as V
            }

            CommonCompilerArguments.X_PHASES_TO_DUMP_BEFORE -> {
                if (value == null) return emptyArray<String>() as V

                val listValue: List<String> = value as List<String>
                listValue.toTypedArray() as V
            }

            CommonCompilerArguments.X_PHASES_TO_DUMP_AFTER -> {
                if (value == null) return emptyArray<String>() as V

                val listValue: List<String> = value as List<String>
                listValue.toTypedArray() as V
            }

            CommonCompilerArguments.X_PHASES_TO_VALIDATE -> {
                if (value == null) return emptyArray<String>() as V

                val listValue: List<String> = value as List<String>
                listValue.toTypedArray() as V
            }

            CommonCompilerArguments.X_PHASES_TO_VALIDATE_BEFORE -> {
                if (value == null) return emptyArray<String>() as V

                val listValue: List<String> = value as List<String>
                listValue.toTypedArray() as V
            }

            CommonCompilerArguments.X_PHASES_TO_VALIDATE_AFTER -> {
                if (value == null) return emptyArray<String>() as V

                val listValue: List<String> = value as List<String>
                listValue.toTypedArray() as V
            }

            CommonCompilerArguments.X_DISABLE_PHASES -> {
                if (value == null) return emptyArray<String>() as V

                val listValue: List<String> = value as List<String>
                listValue.toTypedArray() as V
            }

            CommonCompilerArguments.X_VERBOSE_PHASES -> {
                if (value == null) return emptyArray<String>() as V

                val listValue: List<String> = value as List<String>
                listValue.toTypedArray() as V
            }

            CommonCompilerArguments.X_SUPPRESS_WARNING -> {
                if (value == null) return emptyArray<String>() as V

                val listValue: List<String> = value as List<String>
                listValue.toTypedArray() as V
            }

            CommonCompilerArguments.OPT_IN -> {
                if (value == null) return emptyArray<String>() as V

                val listValue: List<String> = value as List<String>
                listValue.toTypedArray() as V
            }

            CommonCompilerArguments.X_ANNOTATION_DEFAULT_TARGET -> {
                if (value == null) return null as V

                val mode = value as AnnotationDefaultTargetMode
                mode.stringValue as V
            }
            CommonCompilerArguments.X_NAME_BASED_DESTRUCTURING -> {
                if (value == null) return null as V

                val mode = value as NameBasedDestructuringMode
                mode.stringValue as V
            }
            CommonCompilerArguments.X_VERIFY_IR -> {
                if (value == null) return null as V

                val mode = value as VerifyIrMode
                mode.stringValue as V
            }

            else -> {
                value as V
            }
        }

    override fun <T, V> mapTo(value: V, key: CommonCompilerArgument<V>): T =
        when (key) {
            CommonCompilerArguments.KOTLIN_HOME -> {
                if (value == null) return null as T

                val stringValue = value as String
                Path(stringValue) as T
            }

            CommonCompilerArguments.X_PHASES_TO_DUMP -> {
                if (value == null) return emptyList<String>() as T

                val arrayValue = value as Array<String>
                arrayValue.toList() as T
            }

            CommonCompilerArguments.X_PHASES_TO_DUMP_BEFORE -> {
                if (value == null) return emptyList<String>() as T

                val arrayValue = value as Array<String>
                arrayValue.toList() as T
            }

            CommonCompilerArguments.X_PHASES_TO_DUMP_AFTER -> {
                if (value == null) return emptyList<String>() as T

                val arrayValue = value as Array<String>
                arrayValue.toList() as T
            }

            CommonCompilerArguments.X_PHASES_TO_VALIDATE -> {
                if (value == null) return emptyList<String>() as T

                val arrayValue = value as Array<String>
                arrayValue.toList() as T
            }

            CommonCompilerArguments.X_PHASES_TO_VALIDATE_BEFORE -> {
                if (value == null) return emptyList<String>() as T

                val arrayValue = value as Array<String>
                arrayValue.toList() as T
            }

            CommonCompilerArguments.X_DISABLE_PHASES -> {
                if (value == null) return emptyList<String>() as T

                val arrayValue = value as Array<String>
                arrayValue.toList() as T
            }

            CommonCompilerArguments.X_PHASES_TO_VALIDATE_AFTER -> {
                if (value == null) return emptyList<String>() as T

                val arrayValue = value as Array<String>
                arrayValue.toList() as T
            }

            CommonCompilerArguments.X_VERBOSE_PHASES -> {
                if (value == null) return emptyList<String>() as T

                val arrayValue = value as Array<String>
                arrayValue.toList() as T
            }

            CommonCompilerArguments.X_SUPPRESS_WARNING -> {
                if (value == null) return emptyList<String>() as T

                val arrayValue = value as Array<String>
                arrayValue.toList() as T
            }

            CommonCompilerArguments.OPT_IN -> {
                if (value == null) return emptyList<String>() as T

                val arrayValue = value as Array<String>
                arrayValue.toList() as T
            }

            CommonCompilerArguments.X_ANNOTATION_DEFAULT_TARGET -> {
                if (value == null) return null as T

                val stringValue = value as String
                AnnotationDefaultTargetMode.entries.firstOrNull { it.stringValue == stringValue } as T
                    ?: throw CompilerArgumentsParseException("Unknown -Xannotation-default-target value: $stringValue")
            }
            CommonCompilerArguments.X_NAME_BASED_DESTRUCTURING -> {
                if (value == null) return null as T

                val stringValue = value as String
                NameBasedDestructuringMode.entries.firstOrNull { it.stringValue == stringValue } as T
                    ?: throw CompilerArgumentsParseException("Unknown -Xname-based-destructuring value: $stringValue")
            }
            CommonCompilerArguments.X_VERIFY_IR -> {
                if (value == null) return null as T

                val stringValue = value as String
                VerifyIrMode.entries.firstOrNull { it.stringValue == stringValue } as T
                    ?: throw CompilerArgumentsParseException("Unknown -Xverify-ir value: $stringValue")
            }

            else -> {
                value as T
            }
        }
}

@Suppress("ClassName", "UNCHECKED_CAST")
@OptIn(ExperimentalCompilerArgument::class)
private object JvmCompilerArgumentPre2_4_0ValueAdapter : CommonCompilerArgumentPre2_4_0ValueAdapter(), JvmCompilerArgumentValueAdapter {

    override fun <V, T> mapFrom(
        value: T,
        key: JvmCompilerArguments.JvmCompilerArgument<V>,
    ): V = when (key) {
        JvmCompilerArguments.JDK_HOME -> {
            if (value == null) return null as V

            val pathValue = value as Path
            pathValue.absolutePathStringOrThrow() as V
        }

        JvmCompilerArguments.X_PROFILE -> {
            if (value == null) return null as V

            val profileCompilerCommand = value as ProfileCompilerCommand
            with(profileCompilerCommand) {
                profilerPath.absolutePathStringOrThrow() +
                        "${File.pathSeparator}${command}" +
                        "${File.pathSeparator}" +
                        outputDir.absolutePathStringOrThrow()
            } as V
        }

        JvmCompilerArguments.JVM_DEFAULT -> {
            if (value == null) return null as V

            val mode = value as JvmDefaultMode
            mode.stringValue as V
        }

        JvmCompilerArguments.X_ABI_STABILITY -> {
            if (value == null) return null as V

            val mode = value as AbiStabilityMode
            mode.stringValue as V
        }

        JvmCompilerArguments.X_ASSERTIONS -> {
            if (value == null) return null as V

            val mode = value as AssertionsMode
            mode.stringValue as V
        }

        JvmCompilerArguments.X_JSPECIFY_ANNOTATIONS -> {
            if (value == null) return null as V

            val mode = value as JspecifyAnnotationsMode
            mode.stringValue as V
        }

        JvmCompilerArguments.X_LAMBDAS -> {
            if (value == null) return null as V

            val mode = value as LambdasMode
            mode.stringValue as V
        }

        JvmCompilerArguments.X_SAM_CONVERSIONS -> {
            if (value == null) return null as V

            val mode = value as SamConversionsMode
            mode.stringValue as V
        }

        JvmCompilerArguments.X_STRING_CONCAT -> {
            if (value == null) return null as V

            val mode = value as StringConcatMode
            mode.stringValue as V
        }

        JvmCompilerArguments.X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS -> {
            if (value == null) return null as V

            val mode = value as CompatqualAnnotationsMode
            mode.stringValue as V
        }

        JvmCompilerArguments.X_WHEN_EXPRESSIONS -> {
            if (value == null) return null as V

            val mode = value as WhenExpressionsMode
            mode.stringValue as V
        }

        JvmCompilerArguments.X_JDK_RELEASE -> {
            if (value == null) return null as V

            val mode = value as JdkRelease
            mode.stringValue as V
        }

        JvmCompilerArguments.X_ADD_MODULES -> {
            if (value == null) return emptyArray<String>() as V

            val listValue: List<String> = value as List<String>
            listValue.toTypedArray() as V
        }

        JvmCompilerArguments.CLASSPATH -> {
            if (value == null) return null as V

            val listValue = value as List<Path>
            listValue.joinToString(File.pathSeparator) { it.absolutePathStringOrThrow() } as V
        }

        JvmCompilerArguments.X_KLIB -> {
            if (value == null) return null as V

            val listValue = value as List<Path>
            listValue.joinToString(File.pathSeparator) { it.absolutePathStringOrThrow() } as V
        }

        JvmCompilerArguments.X_MODULE_PATH -> {
            if (value == null) return null as V

            val listValue = value as List<Path>
            listValue.joinToString(File.pathSeparator) { it.absolutePathStringOrThrow() } as V
        }

        JvmCompilerArguments.X_FRIEND_PATHS -> {
            if (value == null) return emptyArray<Path>() as V

            val listValue = value as List<Path>
            listValue.map { it.absolutePathStringOrThrow() }.toTypedArray() as V
        }

        JvmCompilerArguments.X_JAVA_SOURCE_ROOTS -> {
            if (value == null) return emptyArray<Path>() as V

            val listValue = value as List<Path>
            listValue.map { it.absolutePathStringOrThrow() }.toTypedArray() as V
        }

        JvmCompilerArguments.SCRIPT_TEMPLATES -> {
            if (value == null) return emptyArray<String>() as V

            val listValue = value as List<String>
            listValue.toTypedArray() as V
        }

        else -> value as V
    }

    override fun <T, V> mapTo(
        value: V,
        key: JvmCompilerArguments.JvmCompilerArgument<V>,
    ): T =
        when (key) {
            JvmCompilerArguments.JDK_HOME -> {
                if (value == null) return null as T

                val stringValue = value as String
                Path(stringValue) as T
            }

            JvmCompilerArguments.X_PROFILE -> {
                if (value == null) return null as T

                val stringValue = value as String
                val parts = stringValue.split(File.pathSeparator)
                require(parts.size == 3) { "Invalid async profiler settings format: $stringValue" }
                ProfileCompilerCommand(Path(parts[0]), parts[1], Path(parts[2])) as T
            }

            JvmCompilerArguments.JVM_DEFAULT -> {
                if (value == null) return null as T

                val stringValue = value as String
                JvmDefaultMode.entries.firstOrNull { it.stringValue == stringValue } as T
                    ?: throw CompilerArgumentsParseException("Unknown -jvm-default value: $stringValue")
            }

            JvmCompilerArguments.X_ABI_STABILITY -> {
                if (value == null) return null as T

                val stringValue = value as String
                AbiStabilityMode.entries.firstOrNull { it.stringValue == stringValue } as T
                    ?: throw CompilerArgumentsParseException("Unknown -Xabi-stability value: $stringValue")
            }

            JvmCompilerArguments.X_ASSERTIONS -> {
                if (value == null) return null as T

                val stringValue = value as String
                AssertionsMode.entries.firstOrNull { it.stringValue == stringValue } as T
                    ?: throw CompilerArgumentsParseException("Unknown -Xassertions value: $stringValue")
            }

            JvmCompilerArguments.X_JSPECIFY_ANNOTATIONS -> {
                if (value == null) return null as T

                val stringValue = value as String
                JspecifyAnnotationsMode.entries.firstOrNull { it.stringValue == stringValue } as T
                    ?: throw CompilerArgumentsParseException("Unknown -Xjspecify-annotations value: $stringValue")
            }

            JvmCompilerArguments.X_LAMBDAS -> {
                if (value == null) return null as T

                val stringValue = value as String
                LambdasMode.entries.firstOrNull { it.stringValue == stringValue } as T
                    ?: throw CompilerArgumentsParseException("Unknown -Xlambdas value: $stringValue")
            }

            JvmCompilerArguments.X_SAM_CONVERSIONS -> {
                if (value == null) return null as T

                val stringValue = value as String
                SamConversionsMode.entries.firstOrNull { it.stringValue == stringValue } as T
                    ?: throw CompilerArgumentsParseException("Unknown -Xsam-conversions value: $stringValue")
            }

            JvmCompilerArguments.X_STRING_CONCAT -> {
                if (value == null) return null as T

                val stringValue = value as String
                StringConcatMode.entries.firstOrNull { it.stringValue == stringValue } as T
                    ?: throw CompilerArgumentsParseException("Unknown -Xstring-concat value: $stringValue")
            }

            JvmCompilerArguments.X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS -> {
                if (value == null) return null as T

                val stringValue = value as String
                CompatqualAnnotationsMode.entries.firstOrNull { it.stringValue == stringValue } as T
                    ?: throw CompilerArgumentsParseException("Unknown -Xsupport-compatqual-checker-framework-annotations value: $stringValue")
            }

            JvmCompilerArguments.X_WHEN_EXPRESSIONS -> {
                if (value == null) return null as T

                val stringValue = value as String
                WhenExpressionsMode.entries.firstOrNull { it.stringValue == stringValue } as T
                    ?: throw CompilerArgumentsParseException("Unknown -Xwhen-expressions value: $stringValue")
            }

            JvmCompilerArguments.X_JDK_RELEASE -> {
                if (value == null) return null as T

                val stringValue = value as String
                JdkRelease.entries.firstOrNull { it.stringValue == stringValue } as T
                    ?: throw CompilerArgumentsParseException("Unknown -Xjdk-release value: $stringValue")
            }

            JvmCompilerArguments.X_ADD_MODULES -> {
                if (value == null) return emptyList<String>() as T

                val arrayValue = value as Array<String>
                arrayValue.toList() as T
            }

            JvmCompilerArguments.CLASSPATH -> {
                if (value == null) return null as T

                val stringValue = value as String
                stringValue.split(File.pathSeparator).map { Path(it) } as T
            }

            JvmCompilerArguments.X_KLIB -> {
                if (value == null) return null as T

                val stringValue = value as String
                stringValue.split(File.pathSeparator).map { Path(it) } as T
            }

            JvmCompilerArguments.X_MODULE_PATH -> {
                if (value == null) return null as T

                val stringValue = value as String
                stringValue.split(File.pathSeparator).map { Path(it) } as T
            }

            JvmCompilerArguments.X_FRIEND_PATHS -> {
                if (value == null) return emptyList<Path>() as T

                val arrayValue = value as Array<String>
                arrayValue.map { Path(it) } as T
            }

            JvmCompilerArguments.X_JAVA_SOURCE_ROOTS -> {
                if (value == null) return emptyList<Path>() as T

                val arrayValue = value as Array<String>
                arrayValue.map { Path(it) } as T
            }

            JvmCompilerArguments.SCRIPT_TEMPLATES -> {
                if (value == null) return emptyList<String>() as T

                val arrayValue = value as Array<String>
                arrayValue.toList() as T
            }

            else -> value as T
        }
}