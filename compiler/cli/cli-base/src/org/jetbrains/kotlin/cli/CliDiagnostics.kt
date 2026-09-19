/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.BaseSourcelessDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.strongWarningWithoutSource

object CliDiagnostics : KtDiagnosticsContainer() {
    val COMPILER_PLUGIN_ARG_IS_EXPERIMENTAL: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    val DEPRECATED_CLI_ARG: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    val REMOVED_CLI_ARG: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    val REDUNDANT_CLI_ARG: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    val CLI_ARG_DISABLES_STABLE_FEATURE: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    val CLASSPATH_RESOLUTION_WARNING: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    val CLASSPATH_RESOLUTION_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()
    val JAVA_MODULE_RESOLUTION_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()
    val ROOTS_RESOLUTION_WARNING: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    val ROOTS_RESOLUTION_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()

    val CLI_ARGFILE_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()

    val CLI_ARGUMENT_WITHOUT_VALUE: KtSourcelessDiagnosticFactory by errorWithoutSource()
    val CLI_BOOLEAN_ARGUMENT_WITH_INCORRECT_VALUE: KtSourcelessDiagnosticFactory by errorWithoutSource()
    val CLI_BOOLEAN_LANGUAGE_FEATURE_ARGUMENT_WITH_VALUE: KtSourcelessDiagnosticFactory by errorWithoutSource()
    val CLI_STRING_LANGUAGE_FEATURE_ARGUMENT_WITH_INCORRECT_VALUE: KtSourcelessDiagnosticFactory by errorWithoutSource()
    val CLI_INVALID_ARGUMENT: KtSourcelessDiagnosticFactory by errorWithoutSource()

    val CLI_UNKNOWN_EXTRA_ARGUMENT: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    // Names of extra (-X...) arguments which have been passed in an obsolete form ("-Xaaa bbb", instead of "-Xaaa=bbb")
    val CLI_EXTRA_ARGUMENT_IN_OBSOLETE_FORM: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    // Arguments where [Argument.deprecatedName] was used; the key is the deprecated name, the value is the new name ([Argument.value])
    val CLI_ARGUMENT_WITH_DEPRECATED_NAME: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()

    // Reports from internal arguments parsers
    val CLI_INTERNAL_ARGUMENT_WARNING: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    val CLI_INTERNAL_ARGUMENT_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()

    val CLI_ARGUMENT_VALUE_PASSED_MULTIPLE_TIMES: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    val CLI_UNSAFE_INTERNAL_ARGUMENT: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()

    val UNSUPPORTED_LANGUAGE_VERSION: KtSourcelessDiagnosticFactory by errorWithoutSource()
    val DEPRECATED_LANGUAGE_VERSION: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    val EXPERIMENTAL_LANGUAGE_VERSION: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()

    val COMPILER_PLUGIN_INITIALIZATION_WARNING: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    val COMPILER_PLUGIN_INITIALIZATION_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()

    val INITIALIZATION_WARNING: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()

    val COMPILER_ARGUMENTS_WARNING: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    val COMPILER_ARGUMENTS_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()

    val JAVAC_INTEGRATION_WARNING: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    val JAVAC_INTEGRATION_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()

    val KOTLIN_PACKAGE_USAGE: KtSourcelessDiagnosticFactory by errorWithoutSource()
    val IO_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()
    val COMPILER_EXCEPTION: KtSourcelessDiagnosticFactory by errorWithoutSource()

    val SCRIPTING_WARNING: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    val SCRIPTING_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()

    val WEB_ARGUMENT_WARNING: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    val WEB_ARGUMENT_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()
    val JS_IC_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()

    val KONAN_ARGUMENT_WARNING: KtSourcelessDiagnosticFactory by warningWithoutSource()
    val KONAN_ARGUMENT_STRONG_WARNING: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    val KONAN_ARGUMENT_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()
    val KONAN_COMPILATION_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()

    val JVM_CLI_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()
    val JVM_CLI_WARNING: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()

    val METADATA_CLI_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = Messages

    object Messages : BaseSourcelessDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("CLI") { map ->
            map.put(COMPILER_PLUGIN_ARG_IS_EXPERIMENTAL, MESSAGE_PLACEHOLDER)
            map.put(DEPRECATED_CLI_ARG, MESSAGE_PLACEHOLDER)
            map.put(REMOVED_CLI_ARG, MESSAGE_PLACEHOLDER)
            map.put(REDUNDANT_CLI_ARG, MESSAGE_PLACEHOLDER)
            map.put(CLI_ARG_DISABLES_STABLE_FEATURE, MESSAGE_PLACEHOLDER)
            map.put(CLASSPATH_RESOLUTION_WARNING, MESSAGE_PLACEHOLDER)
            map.put(CLASSPATH_RESOLUTION_ERROR, MESSAGE_PLACEHOLDER)
            map.put(JAVA_MODULE_RESOLUTION_ERROR, MESSAGE_PLACEHOLDER)
            map.put(ROOTS_RESOLUTION_WARNING, MESSAGE_PLACEHOLDER)
            map.put(ROOTS_RESOLUTION_ERROR, MESSAGE_PLACEHOLDER)

            map.put(CLI_ARGUMENT_WITHOUT_VALUE, MESSAGE_PLACEHOLDER)
            map.put(CLI_BOOLEAN_ARGUMENT_WITH_INCORRECT_VALUE, MESSAGE_PLACEHOLDER)
            map.put(CLI_BOOLEAN_LANGUAGE_FEATURE_ARGUMENT_WITH_VALUE, MESSAGE_PLACEHOLDER)
            map.put(CLI_STRING_LANGUAGE_FEATURE_ARGUMENT_WITH_INCORRECT_VALUE, MESSAGE_PLACEHOLDER)
            map.put(CLI_INVALID_ARGUMENT, MESSAGE_PLACEHOLDER)

            map.put(CLI_UNKNOWN_EXTRA_ARGUMENT, MESSAGE_PLACEHOLDER)
            map.put(CLI_EXTRA_ARGUMENT_IN_OBSOLETE_FORM, MESSAGE_PLACEHOLDER)
            map.put(CLI_ARGUMENT_WITH_DEPRECATED_NAME, MESSAGE_PLACEHOLDER)
            map.put(CLI_ARGFILE_ERROR, MESSAGE_PLACEHOLDER)
            map.put(CLI_INTERNAL_ARGUMENT_ERROR, MESSAGE_PLACEHOLDER)
            map.put(CLI_INTERNAL_ARGUMENT_WARNING, MESSAGE_PLACEHOLDER)
            map.put(CLI_ARGUMENT_VALUE_PASSED_MULTIPLE_TIMES, MESSAGE_PLACEHOLDER)
            map.put(CLI_UNSAFE_INTERNAL_ARGUMENT, MESSAGE_PLACEHOLDER)

            map.put(UNSUPPORTED_LANGUAGE_VERSION, MESSAGE_PLACEHOLDER)
            map.put(DEPRECATED_LANGUAGE_VERSION, MESSAGE_PLACEHOLDER)
            map.put(EXPERIMENTAL_LANGUAGE_VERSION, MESSAGE_PLACEHOLDER)

            map.put(COMPILER_PLUGIN_INITIALIZATION_WARNING, MESSAGE_PLACEHOLDER)
            map.put(COMPILER_PLUGIN_INITIALIZATION_ERROR, MESSAGE_PLACEHOLDER)

            map.put(INITIALIZATION_WARNING, MESSAGE_PLACEHOLDER)

            map.put(COMPILER_ARGUMENTS_WARNING, MESSAGE_PLACEHOLDER)
            map.put(COMPILER_ARGUMENTS_ERROR, MESSAGE_PLACEHOLDER)

            map.put(JAVAC_INTEGRATION_WARNING, MESSAGE_PLACEHOLDER)
            map.put(JAVAC_INTEGRATION_ERROR, MESSAGE_PLACEHOLDER)

            map.put(KOTLIN_PACKAGE_USAGE, MESSAGE_PLACEHOLDER)
            map.put(IO_ERROR, MESSAGE_PLACEHOLDER)
            map.put(COMPILER_EXCEPTION, MESSAGE_PLACEHOLDER)

            map.put(SCRIPTING_WARNING, MESSAGE_PLACEHOLDER)
            map.put(SCRIPTING_ERROR, MESSAGE_PLACEHOLDER)

            map.put(WEB_ARGUMENT_WARNING, MESSAGE_PLACEHOLDER)
            map.put(WEB_ARGUMENT_ERROR, MESSAGE_PLACEHOLDER)
            map.put(JS_IC_ERROR, MESSAGE_PLACEHOLDER)

            map.put(KONAN_ARGUMENT_WARNING, MESSAGE_PLACEHOLDER)
            map.put(KONAN_ARGUMENT_STRONG_WARNING, MESSAGE_PLACEHOLDER)
            map.put(KONAN_ARGUMENT_ERROR, MESSAGE_PLACEHOLDER)
            map.put(KONAN_COMPILATION_ERROR, MESSAGE_PLACEHOLDER)

            map.put(JVM_CLI_ERROR, MESSAGE_PLACEHOLDER)
            map.put(JVM_CLI_WARNING, MESSAGE_PLACEHOLDER)

            map.put(METADATA_CLI_ERROR, MESSAGE_PLACEHOLDER)
        }
    }
}
