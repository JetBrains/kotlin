/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.kotlin.cli.common

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfigurationService
import org.jetbrains.kotlin.cli.common.config.ContentRoot
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import java.io.File

object CLIConfigurationKeys {
    // Roots, including dependencies and own sources
    @JvmField
    val CONTENT_ROOTS: CompilerConfigurationKey<List<ContentRoot>> = CompilerConfigurationKey.create("content roots")

    // Used by kotest, Realm, Dokka, KSP compiler plugins
    @Deprecated(
        "Please use CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY instead",
        ReplaceWith("CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY", "org.jetbrains.kotlin.config.CommonConfigurationKeys"),
        DeprecationLevel.WARNING,
    )
    @JvmField
    val MESSAGE_COLLECTOR_KEY: CompilerConfigurationKey<MessageCollector> = CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY

    // Used by compiler plugins to access delegated message collector in GroupingMessageCollector
    @JvmField
    val ORIGINAL_MESSAGE_COLLECTOR_KEY: CompilerConfigurationKey<MessageCollector> =
        CompilerConfigurationKey.create("original message collector")

    @JvmField
    val RENDER_DIAGNOSTIC_INTERNAL_NAME: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("render diagnostic internal name")

    @JvmField
    val ALLOW_KOTLIN_PACKAGE: CompilerConfigurationKey<Boolean> = CompilerConfigurationKey.create("allow kotlin package")

    @JvmField
    val PERF_MANAGER: CompilerConfigurationKey<CommonCompilerPerformanceManager> = CompilerConfigurationKey.create("performance manager")

    // Used in Eclipse plugin (see KotlinCLICompiler)
    @JvmField
    val INTELLIJ_PLUGIN_ROOT: CompilerConfigurationKey<String> = CompilerConfigurationKey.create("intellij plugin root")

    // See K2MetadataCompilerArguments
    @JvmField
    val METADATA_DESTINATION_DIRECTORY: CompilerConfigurationKey<File> = CompilerConfigurationKey.create("metadata destination directory")

    @JvmField
    val PHASE_CONFIG: CompilerConfigurationKey<PhaseConfig> = CompilerConfigurationKey.create("phase configuration")

    @JvmField
    val FLEXIBLE_PHASE_CONFIG: CompilerConfigurationKey<PhaseConfigurationService> =
        CompilerConfigurationKey.create("flexible phase configuration")

    // used in FIR IDE uast tests
    @JvmField
    val PATH_TO_KOTLIN_COMPILER_JAR: CompilerConfigurationKey<File> =
        CompilerConfigurationKey.create("jar of Kotlin compiler in Kotlin plugin")
}
