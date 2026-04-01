/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("IncorrectFormatting", "unused")

package org.jetbrains.kotlin.cli.common

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

import java.io.File
import org.jetbrains.kotlin.cli.common.config.ContentRoot
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.modules.ModuleChunk
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.utils.KotlinPaths

object CLIConfigurationKeys {
    // Roots, including dependencies and own sources.
    @JvmField
    val CONTENT_ROOTS = CompilerConfigurationKey.create<List<ContentRoot>>("CONTENT_ROOTS")

    // Used by kotest, Realm, Dokka, KSP compiler plugins.
    @Deprecated(
        "Please use CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY instead",
        ReplaceWith("CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY", "org.jetbrains.kotlin.config.CommonConfigurationKeys"),
        DeprecationLevel.ERROR,
    )
    @JvmField
    val MESSAGE_COLLECTOR_KEY = CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY

    // Used by compiler plugins to access delegated message collector in GroupingMessageCollector.
    @JvmField
    val ORIGINAL_MESSAGE_COLLECTOR_KEY = CompilerConfigurationKey.create<MessageCollector>("ORIGINAL_MESSAGE_COLLECTOR_KEY")

    @JvmField
    val DIAGNOSTICS_COLLECTOR = CompilerConfigurationKey.create<BaseDiagnosticsCollector>("DIAGNOSTICS_COLLECTOR")

    @JvmField
    val RENDER_DIAGNOSTIC_INTERNAL_NAME = CompilerConfigurationKey.create<Boolean>("RENDER_DIAGNOSTIC_INTERNAL_NAME")

    @JvmField
    val TREAT_WARNINGS_AS_ERRORS = CompilerConfigurationKey.create<Boolean>("TREAT_WARNINGS_AS_ERRORS")

    @JvmField
    val ALLOW_KOTLIN_PACKAGE = CompilerConfigurationKey.create<Boolean>("ALLOW_KOTLIN_PACKAGE")

    // Used in Eclipse plugin (see KotlinCLICompiler).
    @JvmField
    val INTELLIJ_PLUGIN_ROOT = CompilerConfigurationKey.create<String>("INTELLIJ_PLUGIN_ROOT")

    // See K2MetadataCompilerArguments.
    @JvmField
    val METADATA_DESTINATION_DIRECTORY = CompilerConfigurationKey.create<File>("METADATA_DESTINATION_DIRECTORY")

    // Path to the Kotlin compiler jar in the Kotlin plugin. Used in FIR IDE uast tests.
    @JvmField
    val PATH_TO_KOTLIN_COMPILER_JAR = CompilerConfigurationKey.create<File>("PATH_TO_KOTLIN_COMPILER_JAR")

    @JvmField
    val PRINT_VERSION = CompilerConfigurationKey.create<Boolean>("PRINT_VERSION")

    @JvmField
    val SCRIPT_MODE = CompilerConfigurationKey.create<Boolean>("SCRIPT_MODE")

    // Runs Kotlin REPL (deprecated).
    @JvmField
    val REPL_MODE = CompilerConfigurationKey.create<Boolean>("REPL_MODE")

    @JvmField
    val KOTLIN_PATHS = CompilerConfigurationKey.create<KotlinPaths>("KOTLIN_PATHS")

    @JvmField
    val ALLOW_NO_SOURCE_FILES = CompilerConfigurationKey.create<Boolean>("ALLOW_NO_SOURCE_FILES")

    @JvmField
    val MODULE_CHUNK = CompilerConfigurationKey.create<ModuleChunk>("MODULE_CHUNK")

    @JvmField
    val BUILD_FILE = CompilerConfigurationKey.create<File>("BUILD_FILE")

    @JvmField
    val FREE_ARGS_FOR_SCRIPT = CompilerConfigurationKey.create<List<String>>("FREE_ARGS_FOR_SCRIPT")

    @JvmField
    val DEFAULT_EXTENSION_FOR_SCRIPTS = CompilerConfigurationKey.create<String>("DEFAULT_EXTENSION_FOR_SCRIPTS")

    // Defines what kind of application environment should be created. Should be set to `true` only in tests
    @JvmField
    val TEST_ENVIRONMENT = CompilerConfigurationKey.create<Boolean>("TEST_ENVIRONMENT")

}

var CompilerConfiguration.contentRoots: List<ContentRoot>
    get() = getList(CLIConfigurationKeys.CONTENT_ROOTS)
    set(value) { put(CLIConfigurationKeys.CONTENT_ROOTS, value) }

var CompilerConfiguration.originalMessageCollectorKey: MessageCollector?
    get() = get(CLIConfigurationKeys.ORIGINAL_MESSAGE_COLLECTOR_KEY)
    set(value) { put(CLIConfigurationKeys.ORIGINAL_MESSAGE_COLLECTOR_KEY, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.diagnosticsCollector: BaseDiagnosticsCollector
    get() = getOrDefault(CLIConfigurationKeys.DIAGNOSTICS_COLLECTOR) { error("diagnostic collector is not initialized") }
    set(value) { put(CLIConfigurationKeys.DIAGNOSTICS_COLLECTOR, value) }

var CompilerConfiguration.renderDiagnosticInternalName: Boolean
    get() = getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    set(value) { put(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME, value) }

var CompilerConfiguration.treatWarningsAsErrors: Boolean
    get() = getBoolean(CLIConfigurationKeys.TREAT_WARNINGS_AS_ERRORS)
    set(value) { put(CLIConfigurationKeys.TREAT_WARNINGS_AS_ERRORS, value) }

var CompilerConfiguration.allowKotlinPackage: Boolean
    get() = getBoolean(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE)
    set(value) { put(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE, value) }

var CompilerConfiguration.intellijPluginRoot: String?
    get() = get(CLIConfigurationKeys.INTELLIJ_PLUGIN_ROOT)
    set(value) { put(CLIConfigurationKeys.INTELLIJ_PLUGIN_ROOT, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.metadataDestinationDirectory: File?
    get() = get(CLIConfigurationKeys.METADATA_DESTINATION_DIRECTORY)
    set(value) { put(CLIConfigurationKeys.METADATA_DESTINATION_DIRECTORY, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.pathToKotlinCompilerJar: File?
    get() = get(CLIConfigurationKeys.PATH_TO_KOTLIN_COMPILER_JAR)
    set(value) { put(CLIConfigurationKeys.PATH_TO_KOTLIN_COMPILER_JAR, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.printVersion: Boolean
    get() = getBoolean(CLIConfigurationKeys.PRINT_VERSION)
    set(value) { put(CLIConfigurationKeys.PRINT_VERSION, value) }

var CompilerConfiguration.scriptMode: Boolean
    get() = getBoolean(CLIConfigurationKeys.SCRIPT_MODE)
    set(value) { put(CLIConfigurationKeys.SCRIPT_MODE, value) }

var CompilerConfiguration.replMode: Boolean
    get() = getBoolean(CLIConfigurationKeys.REPL_MODE)
    set(value) { put(CLIConfigurationKeys.REPL_MODE, value) }

var CompilerConfiguration.kotlinPaths: KotlinPaths?
    get() = get(CLIConfigurationKeys.KOTLIN_PATHS)
    set(value) { put(CLIConfigurationKeys.KOTLIN_PATHS, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.allowNoSourceFiles: Boolean
    get() = getBoolean(CLIConfigurationKeys.ALLOW_NO_SOURCE_FILES)
    set(value) { put(CLIConfigurationKeys.ALLOW_NO_SOURCE_FILES, value) }

var CompilerConfiguration.moduleChunk: ModuleChunk?
    get() = get(CLIConfigurationKeys.MODULE_CHUNK)
    set(value) { put(CLIConfigurationKeys.MODULE_CHUNK, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.buildFile: File?
    get() = get(CLIConfigurationKeys.BUILD_FILE)
    set(value) { put(CLIConfigurationKeys.BUILD_FILE, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.freeArgsForScript: List<String>
    get() = getList(CLIConfigurationKeys.FREE_ARGS_FOR_SCRIPT)
    set(value) { put(CLIConfigurationKeys.FREE_ARGS_FOR_SCRIPT, value) }

var CompilerConfiguration.defaultExtensionForScripts: String?
    get() = get(CLIConfigurationKeys.DEFAULT_EXTENSION_FOR_SCRIPTS)
    set(value) { putIfNotNull(CLIConfigurationKeys.DEFAULT_EXTENSION_FOR_SCRIPTS, value) }

var CompilerConfiguration.testEnvironment: Boolean
    get() = getBoolean(CLIConfigurationKeys.TEST_ENVIRONMENT)
    set(value) { put(CLIConfigurationKeys.TEST_ENVIRONMENT, value) }

