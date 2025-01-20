/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.keys.generator

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.HmppCliModuleStructure
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.keys.generator.model.KeysContainer
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion

@Suppress("unused")
object CommonConfigurationKeysContainer : KeysContainer("org.jetbrains.kotlin.config", "CommonConfigurationKeys") {
    val LANGUAGE_VERSION_SETTINGS by key<LanguageVersionSettings>(
        "language version settings",
        defaultValue = "LanguageVersionSettingsImpl.DEFAULT",
        importsToAdd = listOf("org.jetbrains.kotlin.config.LanguageVersionSettingsImpl")
    )

    val DISABLE_INLINE by key<Boolean>("disable inline")
    val MODULE_NAME by key<String>("module name")
    val REPORT_OUTPUT_FILES by key<Boolean>("report output files")
    val LOOKUP_TRACKER by key<LookupTracker>("lookup tracker", throwOnNull = false)
    val EXPECT_ACTUAL_TRACKER by key<ExpectActualTracker>("expect actual tracker", throwOnNull = false)
    val INLINE_CONST_TRACKER by key<InlineConstTracker>("inline constant tracker", throwOnNull = false)
    val ENUM_WHEN_TRACKER by key<EnumWhenTracker>("enum when tracker", throwOnNull = false)
    val IMPORT_TRACKER by key<ImportTracker>("import tracker", throwOnNull = false)
    val METADATA_VERSION by key<BinaryVersion>("metadata version")
    val USE_FIR by key<Boolean>("front-end IR")
    val USE_LIGHT_TREE by key<Boolean>("light tree")
    val HMPP_MODULE_STRUCTURE by key<HmppCliModuleStructure>("HMPP module structure")
    val METADATA_KLIB by key<Boolean>("Produce metadata klib")
    val USE_FIR_EXTRA_CHECKERS by key<Boolean>("fir extra checkers")
    val USE_FIR_EXPERIMENTAL_CHECKERS by key<Boolean>("fir not-public-ready checkers")
    val PARALLEL_BACKEND_THREADS by key<Int>("Run codegen phase in parallel with N threads")
    val INCREMENTAL_COMPILATION by key<Boolean>("Enable incremental compilation")
    val ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS by key<Boolean>("Allow to compile any scripts along with regular Kotlin sources")
    val IGNORE_CONST_OPTIMIZATION_ERRORS by key<Boolean>("Ignore errors from IrConstTransformer")
    val EVALUATED_CONST_TRACKER by key<EvaluatedConstTracker>("Keeps track of all evaluated by IrInterpreter constants")

    val MESSAGE_COLLECTOR_KEY by key<MessageCollector>(
        "message collector",
        defaultValue = "MessageCollector.NONE",
        accessorName = "messageCollector",
    )

    val VERIFY_IR by key<IrVerificationMode>("IR verification mode")
    val ENABLE_IR_VISIBILITY_CHECKS by key<Boolean>("Check pre-lowering IR for visibility violations")
    val ENABLE_IR_VARARG_TYPES_CHECKS by key<Boolean>("Check IR for vararg types mismatches")
    val PHASE_CONFIG by key<PhaseConfig>("phase configuration")

    val DONT_CREATE_SEPARATE_SESSION_FOR_SCRIPTS by key<Boolean>(
        description = "don't create separate session for scripts",
        comment = "Should be used only in tests, impossible to set via compiler arguments"
    )
}
