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

import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
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
    val METADATA_VERSION = CompilerConfigurationKey.create<BinaryVersion>("metadata version")

    @JvmField
    val LIST_PHASES = CompilerConfigurationKey.create<Boolean>("list names of backend phases")

    @JvmField
    val DISABLED_PHASES = CompilerConfigurationKey.create<Set<String>>("disable backend phases")

    @JvmField
    val PHASES_TO_DUMP_STATE_BEFORE = CompilerConfigurationKey.create<Set<String>>("backend phases where we dump compiler state before the phase")

    @JvmField
    val PHASES_TO_DUMP_STATE_AFTER = CompilerConfigurationKey.create<Set<String>>("backend phases where we dump compiler state after the phase")

    @JvmField
    val PHASES_TO_DUMP_STATE = CompilerConfigurationKey.create<Set<String>>("backend phases where we dump compiler state both before and after the phase")

    @JvmField
    val VERBOSE_PHASES = CompilerConfigurationKey.create<Set<String>>("verbose backend phases")

    @JvmField
    val PROFILE_PHASES = CompilerConfigurationKey.create<Boolean>("profile backend phase execution")

    @JvmField
    val EXCLUDED_ELEMENTS_FROM_DUMPING = CompilerConfigurationKey.create<Set<String>>("lowering elements which shouldn't be dumped at all")
}

var CompilerConfiguration.languageVersionSettings: LanguageVersionSettings
    get() = get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, LanguageVersionSettingsImpl.DEFAULT)
    set(value) = put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, value)
