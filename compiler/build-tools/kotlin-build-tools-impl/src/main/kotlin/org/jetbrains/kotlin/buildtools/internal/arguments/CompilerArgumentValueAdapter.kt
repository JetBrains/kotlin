/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.*
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.CommonCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.enums.*
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
            CommonCompilerArguments.KOTLIN_HOME,
            CommonCompilerArguments.X_DUMP_DIRECTORY,
            CommonCompilerArguments.X_DUMP_PERF,
                -> {
                if (value == null) return null as V

                val pathValue = value as Path
                pathValue.absolutePathStringOrThrow() as V
            }

            CommonCompilerArguments.X_PHASES_TO_DUMP,
            CommonCompilerArguments.X_PHASES_TO_DUMP_BEFORE,
            CommonCompilerArguments.X_PHASES_TO_DUMP_AFTER,
            CommonCompilerArguments.X_PHASES_TO_VALIDATE,
            CommonCompilerArguments.X_PHASES_TO_VALIDATE_BEFORE,
            CommonCompilerArguments.X_PHASES_TO_VALIDATE_AFTER,
            CommonCompilerArguments.X_DISABLE_PHASES,
            CommonCompilerArguments.X_VERBOSE_PHASES,
            CommonCompilerArguments.X_SUPPRESS_WARNING,
            CommonCompilerArguments.OPT_IN,
                -> {
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

            CommonCompilerArguments.X_WARNING_LEVEL -> {
                if (value == null) return emptyArray<String>() as V

                val listValue: List<WarningLevel> = value as List<WarningLevel>
                listValue.map { item -> "${item.warningName}:${item.severity.stringValue}" }.toTypedArray() as V
            }

            else -> {
                value as V
            }
        }

    override fun <T, V> mapTo(value: V, key: CommonCompilerArgument<V>): T =
        when (key) {
            CommonCompilerArguments.KOTLIN_HOME,
            CommonCompilerArguments.X_DUMP_DIRECTORY,
            CommonCompilerArguments.X_DUMP_PERF,
                -> {
                if (value == null) return null as T

                val stringValue = value as String
                Path(stringValue) as T
            }

            CommonCompilerArguments.X_PHASES_TO_DUMP,
            CommonCompilerArguments.X_PHASES_TO_DUMP_BEFORE,
            CommonCompilerArguments.X_PHASES_TO_DUMP_AFTER,
            CommonCompilerArguments.X_PHASES_TO_VALIDATE,
            CommonCompilerArguments.X_PHASES_TO_VALIDATE_BEFORE,
            CommonCompilerArguments.X_PHASES_TO_VALIDATE_AFTER,
            CommonCompilerArguments.X_DISABLE_PHASES,
            CommonCompilerArguments.X_VERBOSE_PHASES,
            CommonCompilerArguments.X_SUPPRESS_WARNING,
            CommonCompilerArguments.OPT_IN,
                -> {
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

            CommonCompilerArguments.X_WARNING_LEVEL -> {
                if (value == null) return emptyList<WarningLevel>() as T

                val arrayValue = value as Array<String>
                arrayValue.map {
                    val parts = it.split(":", limit = 2)
                    require(parts.size == 2) { "Invalid -Xwarning-level format: $it" }

                    val level = WarningLevel.Severity.entries.firstOrNull { entry -> entry.stringValue == parts[1] }
                        ?: throw CompilerArgumentsParseException("Unknown -Xwarning-level level: $it")
                    WarningLevel(parts[0], level)
                } as T
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

        JvmCompilerArguments.CLASSPATH,
        JvmCompilerArguments.X_KLIB,
        JvmCompilerArguments.X_MODULE_PATH,
            -> {
            if (value == null) return null as V

            val listValue = value as List<Path>
            listValue.joinToString(File.pathSeparator) { it.absolutePathStringOrThrow() } as V
        }

        JvmCompilerArguments.X_FRIEND_PATHS,
        JvmCompilerArguments.X_JAVA_SOURCE_ROOTS,
            -> {
            if (value == null) return emptyArray<String>() as V

            val listValue = value as List<Path>
            listValue.map { it.absolutePathStringOrThrow() }.toTypedArray() as V
        }

        JvmCompilerArguments.X_ADD_MODULES,
        JvmCompilerArguments.SCRIPT_TEMPLATES,
        JvmCompilerArguments.X_SCRIPT_RESOLVER_ENVIRONMENT,
            -> {
            if (value == null) return emptyArray<String>() as V

            val listValue = value as List<String>
            listValue.toTypedArray() as V
        }

        JvmCompilerArguments.X_NULLABILITY_ANNOTATIONS -> {
            if (value == null) return emptyArray<String>() as V

            val listValue = value as List<NullabilityAnnotation>
            listValue.map { item -> "${item.annotationFqName}:${item.mode.stringValue}" }.toTypedArray() as V
        }

        JvmCompilerArguments.X_JSR305 -> {
            if (value == null) return emptyArray<String>() as V

            val listValue = value as List<Jsr305>
            listValue.map { item ->
                when (item) {
                    is Jsr305.Global -> item.mode.stringValue
                    is Jsr305.UnderMigration -> "under-migration:${item.mode.stringValue}"
                    is Jsr305.SpecificAnnotation -> "${item.annotationFqName}:${item.mode.stringValue}"
                }
            }.toTypedArray() as V
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
                require(parts.size == 3) { "Invalid -Xprofile format: $stringValue" }
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

            JvmCompilerArguments.CLASSPATH,
            JvmCompilerArguments.X_KLIB,
            JvmCompilerArguments.X_MODULE_PATH,
                -> {
                if (value == null) return null as T

                val stringValue = value as String
                stringValue.split(File.pathSeparator).map { Path(it) } as T
            }

            JvmCompilerArguments.X_FRIEND_PATHS,
            JvmCompilerArguments.X_JAVA_SOURCE_ROOTS,
                -> {
                if (value == null) return emptyList<Path>() as T

                val arrayValue = value as Array<String>
                arrayValue.also { array -> array.asList().checkNoneContains(",") }.map { Path(it) } as T
            }

            JvmCompilerArguments.X_ADD_MODULES,
            JvmCompilerArguments.SCRIPT_TEMPLATES,
            JvmCompilerArguments.X_SCRIPT_RESOLVER_ENVIRONMENT,
                -> {
                if (value == null) return emptyList<String>() as T

                val arrayValue = value as Array<String>
                arrayValue.toList() as T
            }

            JvmCompilerArguments.X_NULLABILITY_ANNOTATIONS -> {
                if (value == null) return emptyList<NullabilityAnnotation>() as T

                val arrayValue = value as Array<String>
                arrayValue.map {
                    val parts = it.split(":")
                    require(parts.size == 2) { "Invalid -Xnullability-annotations format: $this" }

                    val nullabilityAnnotationMode =
                        NullabilityAnnotation.Mode.entries.firstOrNull { entry -> entry.stringValue == parts[1] }
                            ?: throw CompilerArgumentsParseException("Unknown -Xnullability-annotations mode: $it")
                    NullabilityAnnotation(parts[0].removePrefix("@"), nullabilityAnnotationMode)
                } as T
            }

            JvmCompilerArguments.X_JSR305 -> {
                if (value == null) return emptyList<Jsr305>() as T

                val arrayValue = value as Array<String>
                fun jsr305mode(stringValue: String) = Jsr305.Mode.entries.firstOrNull { entry -> entry.stringValue == stringValue }
                    ?: throw CompilerArgumentsParseException("Unknown -Xjsr305 mode: $stringValue")

                arrayValue.map {
                    val parts = it.split(":")
                    when (parts.size) {
                        1 -> Jsr305.Global(jsr305mode(parts[0]))
                        2 if parts[0] == "under-migration" -> Jsr305.UnderMigration(jsr305mode(parts[1]))
                        2 -> Jsr305.SpecificAnnotation(parts[0].removePrefix("@"), jsr305mode(parts[1]))
                        else -> throw CompilerArgumentsParseException("Invalid -Xjsr30 format: $it")
                    }
                } as T
            }

            else -> value as T
        }
}
