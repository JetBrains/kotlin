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
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.util.PerformanceManager

@Suppress("unused")
object CommonConfigurationKeysContainer : KeysContainer("org.jetbrains.kotlin.config", "CommonConfigurationKeys") {
    val LANGUAGE_VERSION_SETTINGS by key<LanguageVersionSettings>(
        defaultValue = "LanguageVersionSettingsImpl.DEFAULT",
        importsToAdd = listOf("org.jetbrains.kotlin.config.LanguageVersionSettingsImpl")
    )

    val DISABLE_INLINE by key<Boolean>()
    val MODULE_NAME by key<String>()
    val REPORT_OUTPUT_FILES by key<Boolean>()
    val LOOKUP_TRACKER by key<LookupTracker>(throwOnNull = false)
    val EXPECT_ACTUAL_TRACKER by key<ExpectActualTracker>(throwOnNull = false)
    val INLINE_CONST_TRACKER by key<InlineConstTracker>(throwOnNull = false)
    val FILE_MAPPING_TRACKER by key<ICFileMappingTracker>(throwOnNull = false)
    val ENUM_WHEN_TRACKER by key<EnumWhenTracker>(throwOnNull = false)
    val IMPORT_TRACKER by key<ImportTracker>(throwOnNull = false)
    val METADATA_VERSION by key<BinaryVersion>()
    val USE_FIR by key<Boolean>()
    val USE_LIGHT_TREE by key<Boolean>()
    val HMPP_MODULE_STRUCTURE by key<HmppCliModuleStructure>()
    val METADATA_KLIB by key<Boolean>()
    val USE_FIR_EXTRA_CHECKERS by key<Boolean>()
    val USE_FIR_EXPERIMENTAL_CHECKERS by key<Boolean>("Enables FIR experimental (not ready for public use) checkers.")
    val DUMP_INFERENCE_LOGS by key<Boolean>()
    val PARALLEL_BACKEND_THREADS by key<Int>("Runs the codegen phase in parallel with N threads.")
    val DUMP_MODEL by key<String>()
    val INCREMENTAL_COMPILATION by key<Boolean>()
    val ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS by key<Boolean>()
    val IGNORE_CONST_OPTIMIZATION_ERRORS by key<Boolean>()
    val EVALUATED_CONST_TRACKER by key<EvaluatedConstTracker>("Keeps track of all constants evaluated by IrInterpreter.")

    val MESSAGE_COLLECTOR_KEY by key<MessageCollector>(defaultValue = "MessageCollector.NONE", accessorName = "messageCollector")

    val VERIFY_IR by key<IrVerificationMode>()
    val ENABLE_IR_VISIBILITY_CHECKS by key<Boolean>("Checks pre-lowering IR for visibility violations.")
    val ENABLE_IR_VARARG_TYPES_CHECKS by key<Boolean>("Checks IR for vararg types mismatches.")
    val ENABLE_IR_NESTED_OFFSETS_CHECKS by key<Boolean>("Checks that offsets of nested IR elements conform to offsets of their containers.")

    val PHASE_CONFIG by key<PhaseConfig>()

    val DONT_CREATE_SEPARATE_SESSION_FOR_SCRIPTS by key<Boolean>("Should be used only in tests, impossible to set via compiler arguments.")

    val DONT_SORT_SOURCE_FILES by key<Boolean>()

    val SCRIPTING_HOST_CONFIGURATION by key<Any>("Internal for passing configuration in the scripting pipeline, impossible to set via compiler arguments.")

    val PERF_MANAGER by key<PerformanceManager>(
        "A helper that can be used to measure performance (compiler phases, JIT and GC info) or collect stats (e.g. number of lines in a project). It might be inaccurate if use in multithreading mode."
    )

    val DETAILED_PERF by key<Boolean>(
        "Enables detailed performance stats that might slow down the general compiler performance. See the description of `-Xdetailed-perf` for more details."
    )

    val TARGET_PLATFORM by key<TargetPlatform>()
}
