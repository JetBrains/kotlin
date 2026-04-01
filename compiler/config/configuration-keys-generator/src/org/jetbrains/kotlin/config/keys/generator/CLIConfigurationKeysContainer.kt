/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.keys.generator

import org.jetbrains.kotlin.cli.common.config.ContentRoot
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.modules.ModuleChunk
import org.jetbrains.kotlin.config.keys.generator.model.KeysContainer
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.utils.KotlinPaths
import java.io.File

@Suppress("unused")
object CLIConfigurationKeysContainer : KeysContainer("org.jetbrains.kotlin.cli.common", "CLIConfigurationKeys") {
    val CONTENT_ROOTS by key<List<ContentRoot>>("Roots, including dependencies and own sources.")

    val MESSAGE_COLLECTOR_KEY by deprecatedKey<MessageCollector>(
        initializer = "CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY",
        deprecation = Deprecated(
            "Please use CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY instead",
            ReplaceWith("CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY", "org.jetbrains.kotlin.config.CommonConfigurationKeys"),
            DeprecationLevel.ERROR,
        ),
        comment = "Used by kotest, Realm, Dokka, KSP compiler plugins.",
        importsToAdd = listOf("org.jetbrains.kotlin.config.CommonConfigurationKeys")
    )

    val ORIGINAL_MESSAGE_COLLECTOR_KEY by key<MessageCollector>(
        "Used by compiler plugins to access delegated message collector in GroupingMessageCollector."
    )

    val DIAGNOSTICS_COLLECTOR by key<BaseDiagnosticsCollector>(lazyDefaultValue = """error("diagnostic collector is not initialized")""")

    val RENDER_DIAGNOSTIC_INTERNAL_NAME by key<Boolean>()
    val TREAT_WARNINGS_AS_ERRORS by key<Boolean>()

    val ALLOW_KOTLIN_PACKAGE by key<Boolean>()

    val INTELLIJ_PLUGIN_ROOT by key<String>("Used in Eclipse plugin (see KotlinCLICompiler).")

    val METADATA_DESTINATION_DIRECTORY by key<File>("See K2MetadataCompilerArguments.")

    val PATH_TO_KOTLIN_COMPILER_JAR by key<File>("Path to the Kotlin compiler jar in the Kotlin plugin. Used in FIR IDE uast tests.")

    val PRINT_VERSION by key<Boolean>()
    val SCRIPT_MODE by key<Boolean>()
    val REPL_MODE by key<Boolean>("Runs Kotlin REPL (deprecated).")
    val KOTLIN_PATHS by key<KotlinPaths>()

    val ALLOW_NO_SOURCE_FILES by key<Boolean>()
    val MODULE_CHUNK by key<ModuleChunk>()
    val BUILD_FILE by key<File>()
    val FREE_ARGS_FOR_SCRIPT by key<List<String>>()
    val DEFAULT_EXTENSION_FOR_SCRIPTS by key<String>(throwOnNull = false)

    val TEST_ENVIRONMENT by key<Boolean>("Defines what kind of application environment should be created. Should be set to `true` only in tests")
}
