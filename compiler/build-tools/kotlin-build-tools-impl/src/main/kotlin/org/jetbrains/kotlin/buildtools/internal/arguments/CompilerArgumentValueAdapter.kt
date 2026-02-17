/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.enums.*
import org.jetbrains.kotlin.buildtools.api.arguments.types.ProfileCompilerCommand
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.reflect.KClass
import kotlin.reflect.full.functions

/**
 * Handles forward compatibility when the API version is older than the implementation version.
 * 
 * Converts between old API argument types (e.g., `String`) and new implementation types (e.g., `Path`)
 * to maintain compatibility when argument type definitions evolve between API and implementation.
 */
@OptIn(ExperimentalCompilerArgument::class)
internal interface CompilerArgumentValueAdapter<V> {

    fun <T> mapFrom(value: Any?, key: V): T?
    fun <T> mapTo(value: Any?, key: V): T?

    companion object {
        //TODO(KT-84598): Expose API Version via Public Property
        private val requiresPre240ForwardCompatibility: Boolean =
            JvmPlatformToolchain::class.functions.none { it.name == "discoverScriptExtensionsOperationBuilder" }

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> getOrNull(keyClass: KClass<T>): CompilerArgumentValueAdapter<T>? {
            return when (keyClass) {
                JvmCompilerArguments.JvmCompilerArgument::class if requiresPre240ForwardCompatibility -> {
                    JvmCompilerArgumentPre2_4_0ValueAdapter as CompilerArgumentValueAdapter<T>
                }
                else -> null
            }
        }
    }
}

@Suppress("ClassName")
@OptIn(ExperimentalCompilerArgument::class)
private object JvmCompilerArgumentPre2_4_0ValueAdapter : CompilerArgumentValueAdapter<JvmCompilerArguments.JvmCompilerArgument<*>> {

    @Suppress("UNCHECKED_CAST")
    override fun <T> mapFrom(
        value: Any?,
        key: JvmCompilerArguments.JvmCompilerArgument<*>,
    ): T? {
        if (value == null) return null as T?
        return when (key) {
            JvmCompilerArguments.JDK_HOME -> {
                val pathValue = value as Path
                pathValue.absolutePathStringOrThrow() as T
            }

            JvmCompilerArguments.X_PROFILE -> {
                val profileCompilerCommand = value as ProfileCompilerCommand
                with(profileCompilerCommand) {
                    "${profilerPath.toFile().absolutePath}" +
                            "${File.pathSeparator}${command}" +
                            "${File.pathSeparator}" +
                            "${outputDir.toFile().absolutePath}"
                } as T
            }

            JvmCompilerArguments.JVM_DEFAULT -> {
                val mode = value as JvmDefaultMode
                mode.stringValue as T
            }

            JvmCompilerArguments.X_ABI_STABILITY -> {
                val mode = value as AbiStabilityMode
                mode.stringValue as T
            }

            JvmCompilerArguments.X_ASSERTIONS -> {
                val mode = value as AssertionsMode
                mode.stringValue as T
            }

            JvmCompilerArguments.X_JSPECIFY_ANNOTATIONS -> {
                val mode = value as JspecifyAnnotationsMode
                mode.stringValue as T
            }

            JvmCompilerArguments.X_LAMBDAS -> {
                val mode = value as LambdasMode
                mode.stringValue as T
            }

            JvmCompilerArguments.X_SAM_CONVERSIONS -> {
                val mode = value as SamConversionsMode
                mode.stringValue as T
            }

            JvmCompilerArguments.X_STRING_CONCAT -> {
                val mode = value as StringConcatMode
                mode.stringValue as T
            }

            JvmCompilerArguments.X_SUPPORT_COMPATQUAL_CHECKER_FRAMEWORK_ANNOTATIONS -> {
                val mode = value as CompatqualAnnotationsMode
                mode.stringValue as T
            }

            else -> value as T
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> mapTo(
        value: Any?,
        key: JvmCompilerArguments.JvmCompilerArgument<*>,
    ): T? {
        if (value == null) return null as T?

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

            else -> value as T
        }
    }
}