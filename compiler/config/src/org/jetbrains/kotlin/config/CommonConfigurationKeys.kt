/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.incremental.components.*
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
    val IMPORT_TRACKER = CompilerConfigurationKey.create<ImportTracker>("import tracker")

    @JvmField
    val METADATA_VERSION = CompilerConfigurationKey.create<BinaryVersion>("metadata version")

    @JvmField
    val USE_FIR = CompilerConfigurationKey.create<Boolean>("front-end IR")

    @JvmField
    val USE_LIGHT_TREE = CompilerConfigurationKey.create<Boolean>("light tree")

    @JvmField
    val HMPP_MODULE_STRUCTURE = CompilerConfigurationKey.create<HmppCliModuleStructure>("HMPP module structure")

    @JvmField
    val METADATA_KLIB = CompilerConfigurationKey.create<Boolean>("Produce metadata klib")

    @JvmField
    val USE_FIR_EXTENDED_CHECKERS = CompilerConfigurationKey.create<Boolean>("fir extended checkers")

    @JvmField
    val PARALLEL_BACKEND_THREADS =
        CompilerConfigurationKey.create<Int>("When using the IR backend, run lowerings by file in N parallel threads")

    @JvmField
    val INCREMENTAL_COMPILATION =
        CompilerConfigurationKey.create<Boolean>("Enable incremental compilation")

    @JvmField
    val ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS =
        CompilerConfigurationKey.create<Boolean>("Allow to compile any scripts along with regular Kotlin sources")

    @JvmField
    val IGNORE_CONST_OPTIMIZATION_ERRORS = CompilerConfigurationKey.create<Boolean>("Ignore errors from IrConstTransformer")

    @JvmField
    val EVALUATED_CONST_TRACKER =
        CompilerConfigurationKey.create<EvaluatedConstTracker>("Keeps track of all evaluated by IrInterpreter constants")

    @JvmField
    val MESSAGE_COLLECTOR_KEY = CompilerConfigurationKey.create<MessageCollector>("message collector")

    @JvmField
    val VERIFY_IR = CompilerConfigurationKey.create<IrVerificationMode>("IR verification mode")

    @JvmField
    val ENABLE_IR_VISIBILITY_CHECKS = CompilerConfigurationKey.create<Boolean>("Check pre-lowering IR for visibility violations")
}

var CompilerConfiguration.languageVersionSettings: LanguageVersionSettings
    get() = get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, LanguageVersionSettingsImpl.DEFAULT)
    set(value) = put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, value)

val LanguageVersionSettings.isLibraryToSourceAnalysisEnabled: Boolean
    get() = getFlag(AnalysisFlags.libraryToSourceAnalysis)

val LanguageVersionSettings.areExpectActualClassesStable: Boolean
    get() {
        return getFlag(AnalysisFlags.muteExpectActualClassesWarning) || supportsFeature(LanguageFeature.ExpectActualClasses)
    }

var CompilerConfiguration.messageCollector: MessageCollector
    get() = get(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
    set(value) = put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, value)
