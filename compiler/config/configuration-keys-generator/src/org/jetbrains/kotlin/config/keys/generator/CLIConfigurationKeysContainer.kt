/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.keys.generator

import org.jetbrains.kotlin.cli.common.config.ContentRoot
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.modules.ModuleChunk
import org.jetbrains.kotlin.config.keys.generator.model.KeysContainer
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.utils.KotlinPaths
import java.io.File

@Suppress("unused")
object CLIConfigurationKeysContainer : KeysContainer("org.jetbrains.kotlin.cli.common", "CLIConfigurationKeys") {
    val CONTENT_ROOTS by key<List<ContentRoot>>("content roots", comment = "Roots, including dependencies and own sources")

    val MESSAGE_COLLECTOR_KEY by deprecatedKey<MessageCollector>(
        initializer = "CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY",
        deprecation = Deprecated(
            "Please use CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY instead",
            ReplaceWith("CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY", "org.jetbrains.kotlin.config.CommonConfigurationKeys"),
            DeprecationLevel.WARNING,
        ),
        comment = "Used by kotest, Realm, Dokka, KSP compiler plugins",
        importsToAdd = listOf("org.jetbrains.kotlin.config.CommonConfigurationKeys")
    )

    val ORIGINAL_MESSAGE_COLLECTOR_KEY by key<MessageCollector>(
        "original message collector",
        comment = "Used by compiler plugins to access delegated message collector in GroupingMessageCollector"
    )

    val RENDER_DIAGNOSTIC_INTERNAL_NAME by key<Boolean>("render diagnostic internal name")

    val ALLOW_KOTLIN_PACKAGE by key<Boolean>("allow kotlin package")

    val PERF_MANAGER by key<PerformanceManager>("performance manager")

    val INTELLIJ_PLUGIN_ROOT by key<String>(
        "intellij plugin root",
        comment = "Used in Eclipse plugin (see KotlinCLICompiler)"
    )

    val METADATA_DESTINATION_DIRECTORY by key<File>("metadata destination directory", comment = "See K2MetadataCompilerArguments")

    val PATH_TO_KOTLIN_COMPILER_JAR by key<File>("jar of Kotlin compiler in Kotlin plugin", comment = "used in FIR IDE uast tests")

    val PRINT_VERSION by key<Boolean>("Print compiler version")
    val SCRIPT_MODE by key<Boolean>("Compile and evaluate kotlin script")
    val KOTLIN_PATHS by key<KotlinPaths>("Kotlin paths")

    val ALLOW_NO_SOURCE_FILES by key<Boolean>("allow no source files compilation")
    val MODULE_CHUNK by key<ModuleChunk>("Module chunk")
    val BUILD_FILE by key<File>("Build file")
    val FREE_ARGS_FOR_SCRIPT by key<List<String>>("Free args from arguments. Used only for scripts execution")
    val DEFAULT_EXTENSION_FOR_SCRIPTS by key<String>("Default extension for scripts", throwOnNull = false)
}
