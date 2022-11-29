/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion

object CommonConfigurationKeys {
    @JvmField
    val LANGUAGE_VERSION_SETTINGS = CompilerConfigurationKey<LanguageVersionSettings>("language version settings")

    @JvmField
    val DISABLE_INLINE = CompilerConfigurationKey<Boolean>("disable inline")

    @JvmField
    val MODULE_NAME = CompilerConfigurationKey<String>("module name")

    @JvmField
    val REPORT_OUTPUT_FILES = CompilerConfigurationKey<Boolean>("report output files")

    @JvmField
    val LOOKUP_TRACKER = CompilerConfigurationKey.create<LookupTracker>("lookup tracker")

    @JvmField
    val EXPECT_ACTUAL_TRACKER = CompilerConfigurationKey.create<ExpectActualTracker>("expect actual tracker")

    @JvmField
    val INLINE_CONST_TRACKER = CompilerConfigurationKey.create<InlineConstTracker>("inline constant tracker")

    @JvmField
    val ENUM_WHEN_TRACKER = CompilerConfigurationKey.create<EnumWhenTracker>("enum when tracker")

    @JvmField
    val METADATA_VERSION = CompilerConfigurationKey.create<BinaryVersion>("metadata version")

    @JvmField
    val USE_FIR = CompilerConfigurationKey.create<Boolean>("front-end IR")

    @JvmField
    val EXPECT_ACTUAL_LINKER = CompilerConfigurationKey.create<Boolean>("Experimental expect/actual linker")

    @JvmField
    val USE_FIR_EXTENDED_CHECKERS = CompilerConfigurationKey.create<Boolean>("fir extended checkers")

    @JvmField
    val PARALLEL_BACKEND_THREADS =
        CompilerConfigurationKey.create<Int>("When using the IR backend, run lowerings by file in N parallel threads")

    @JvmField
    val KLIB_RELATIVE_PATH_BASES =
        CompilerConfigurationKey.create<Collection<String>>("Provides a path from which relative paths in klib are being computed")

    @JvmField
    val KLIB_NORMALIZE_ABSOLUTE_PATH =
        CompilerConfigurationKey.create<Boolean>("Normalize absolute paths in klib (replace file separator with '/')")

    @JvmField
    val PRODUCE_KLIB_SIGNATURES_CLASH_CHECKS =
        CompilerConfigurationKey.create<Boolean>("Turn on the checks on uniqueness of signatures")

    @JvmField
    val INCREMENTAL_COMPILATION =
        CompilerConfigurationKey.create<Boolean>("Enable incremental compilation")

    @JvmField
    val ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS =
        CompilerConfigurationKey.create<Boolean>("Allow to compile any scripts along with regular Kotlin sources")
}

var CompilerConfiguration.languageVersionSettings: LanguageVersionSettings
    get() = get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, LanguageVersionSettingsImpl.DEFAULT)
    set(value) = put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, value)

val LanguageVersionSettings.isLibraryToSourceAnalysisEnabled: Boolean
    get() = getFlag(AnalysisFlags.libraryToSourceAnalysis)
