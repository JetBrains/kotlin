/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("IncorrectFormatting", "unused")

package org.jetbrains.kotlin.config

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.ICFileMappingTracker
import org.jetbrains.kotlin.incremental.components.ImportTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.util.PerformanceManager

object CommonConfigurationKeys {
    @JvmField
    val LANGUAGE_VERSION_SETTINGS = CompilerConfigurationKey.create<LanguageVersionSettings>("LANGUAGE_VERSION_SETTINGS")

    @JvmField
    val DISABLE_INLINE = CompilerConfigurationKey.create<Boolean>("DISABLE_INLINE")

    @JvmField
    val MODULE_NAME = CompilerConfigurationKey.create<String>("MODULE_NAME")

    @JvmField
    val REPORT_OUTPUT_FILES = CompilerConfigurationKey.create<Boolean>("REPORT_OUTPUT_FILES")

    @JvmField
    val LOOKUP_TRACKER = CompilerConfigurationKey.create<LookupTracker>("LOOKUP_TRACKER")

    @JvmField
    val EXPECT_ACTUAL_TRACKER = CompilerConfigurationKey.create<ExpectActualTracker>("EXPECT_ACTUAL_TRACKER")

    @JvmField
    val INLINE_CONST_TRACKER = CompilerConfigurationKey.create<InlineConstTracker>("INLINE_CONST_TRACKER")

    @JvmField
    val FILE_MAPPING_TRACKER = CompilerConfigurationKey.create<ICFileMappingTracker>("FILE_MAPPING_TRACKER")

    @JvmField
    val ENUM_WHEN_TRACKER = CompilerConfigurationKey.create<EnumWhenTracker>("ENUM_WHEN_TRACKER")

    @JvmField
    val IMPORT_TRACKER = CompilerConfigurationKey.create<ImportTracker>("IMPORT_TRACKER")

    @JvmField
    val METADATA_VERSION = CompilerConfigurationKey.create<BinaryVersion>("METADATA_VERSION")

    @JvmField
    val USE_FIR = CompilerConfigurationKey.create<Boolean>("USE_FIR")

    @JvmField
    val USE_LIGHT_TREE = CompilerConfigurationKey.create<Boolean>("USE_LIGHT_TREE")

    @JvmField
    val HMPP_MODULE_STRUCTURE = CompilerConfigurationKey.create<HmppCliModuleStructure>("HMPP_MODULE_STRUCTURE")

    @JvmField
    val METADATA_KLIB = CompilerConfigurationKey.create<Boolean>("METADATA_KLIB")

    @JvmField
    val USE_FIR_EXTRA_CHECKERS = CompilerConfigurationKey.create<Boolean>("USE_FIR_EXTRA_CHECKERS")

    // Enables FIR experimental (not ready for public use) checkers.
    @JvmField
    val USE_FIR_EXPERIMENTAL_CHECKERS = CompilerConfigurationKey.create<Boolean>("USE_FIR_EXPERIMENTAL_CHECKERS")

    @JvmField
    val DUMP_INFERENCE_LOGS = CompilerConfigurationKey.create<Boolean>("DUMP_INFERENCE_LOGS")

    // Runs the codegen phase in parallel with N threads.
    @JvmField
    val PARALLEL_BACKEND_THREADS = CompilerConfigurationKey.create<Int>("PARALLEL_BACKEND_THREADS")

    @JvmField
    val DUMP_MODEL = CompilerConfigurationKey.create<String>("DUMP_MODEL")

    @JvmField
    val INCREMENTAL_COMPILATION = CompilerConfigurationKey.create<Boolean>("INCREMENTAL_COMPILATION")

    @JvmField
    val ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS = CompilerConfigurationKey.create<Boolean>("ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS")

    @JvmField
    val IGNORE_CONST_OPTIMIZATION_ERRORS = CompilerConfigurationKey.create<Boolean>("IGNORE_CONST_OPTIMIZATION_ERRORS")

    // Keeps track of all constants evaluated by IrInterpreter.
    @JvmField
    val EVALUATED_CONST_TRACKER = CompilerConfigurationKey.create<EvaluatedConstTracker>("EVALUATED_CONST_TRACKER")

    @JvmField
    val MESSAGE_COLLECTOR_KEY = CompilerConfigurationKey.create<MessageCollector>("MESSAGE_COLLECTOR_KEY")

    @JvmField
    val VERIFY_IR = CompilerConfigurationKey.create<IrVerificationMode>("VERIFY_IR")

    // Checks pre-lowering IR for visibility violations.
    @JvmField
    val ENABLE_IR_VISIBILITY_CHECKS = CompilerConfigurationKey.create<Boolean>("ENABLE_IR_VISIBILITY_CHECKS")

    // Checks IR for vararg types mismatches.
    @JvmField
    val ENABLE_IR_VARARG_TYPES_CHECKS = CompilerConfigurationKey.create<Boolean>("ENABLE_IR_VARARG_TYPES_CHECKS")

    // Checks that offsets of nested IR elements conform to offsets of their containers.
    @JvmField
    val ENABLE_IR_NESTED_OFFSETS_CHECKS = CompilerConfigurationKey.create<Boolean>("ENABLE_IR_NESTED_OFFSETS_CHECKS")

    @JvmField
    val PHASE_CONFIG = CompilerConfigurationKey.create<PhaseConfig>("PHASE_CONFIG")

    // Should be used only in tests, impossible to set via compiler arguments.
    @JvmField
    val DONT_CREATE_SEPARATE_SESSION_FOR_SCRIPTS = CompilerConfigurationKey.create<Boolean>("DONT_CREATE_SEPARATE_SESSION_FOR_SCRIPTS")

    @JvmField
    val DONT_SORT_SOURCE_FILES = CompilerConfigurationKey.create<Boolean>("DONT_SORT_SOURCE_FILES")

    // Internal for passing configuration in the scripting pipeline, impossible to set via compiler arguments.
    @JvmField
    val SCRIPTING_HOST_CONFIGURATION = CompilerConfigurationKey.create<Any>("SCRIPTING_HOST_CONFIGURATION")

    // A helper that can be used to measure performance (compiler phases, JIT and GC info) or collect stats (e.g. number of lines in a project). It might be inaccurate if use in multithreading mode.
    @JvmField
    val PERF_MANAGER = CompilerConfigurationKey.create<PerformanceManager>("PERF_MANAGER")

    // Enables detailed performance stats that might slow down the general compiler performance. See the description of `-Xdetailed-perf` for more details.
    @JvmField
    val DETAILED_PERF = CompilerConfigurationKey.create<Boolean>("DETAILED_PERF")

    @JvmField
    val TARGET_PLATFORM = CompilerConfigurationKey.create<TargetPlatform>("TARGET_PLATFORM")

}

var CompilerConfiguration.languageVersionSettings: LanguageVersionSettings
    get() = get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, LanguageVersionSettingsImpl.DEFAULT)
    set(value) { put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, value) }

var CompilerConfiguration.disableInline: Boolean
    get() = getBoolean(CommonConfigurationKeys.DISABLE_INLINE)
    set(value) { put(CommonConfigurationKeys.DISABLE_INLINE, value) }

var CompilerConfiguration.moduleName: String?
    get() = get(CommonConfigurationKeys.MODULE_NAME)
    set(value) { put(CommonConfigurationKeys.MODULE_NAME, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.reportOutputFiles: Boolean
    get() = getBoolean(CommonConfigurationKeys.REPORT_OUTPUT_FILES)
    set(value) { put(CommonConfigurationKeys.REPORT_OUTPUT_FILES, value) }

var CompilerConfiguration.lookupTracker: LookupTracker?
    get() = get(CommonConfigurationKeys.LOOKUP_TRACKER)
    set(value) { putIfNotNull(CommonConfigurationKeys.LOOKUP_TRACKER, value) }

var CompilerConfiguration.expectActualTracker: ExpectActualTracker?
    get() = get(CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER)
    set(value) { putIfNotNull(CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER, value) }

var CompilerConfiguration.inlineConstTracker: InlineConstTracker?
    get() = get(CommonConfigurationKeys.INLINE_CONST_TRACKER)
    set(value) { putIfNotNull(CommonConfigurationKeys.INLINE_CONST_TRACKER, value) }

var CompilerConfiguration.fileMappingTracker: ICFileMappingTracker?
    get() = get(CommonConfigurationKeys.FILE_MAPPING_TRACKER)
    set(value) { putIfNotNull(CommonConfigurationKeys.FILE_MAPPING_TRACKER, value) }

var CompilerConfiguration.enumWhenTracker: EnumWhenTracker?
    get() = get(CommonConfigurationKeys.ENUM_WHEN_TRACKER)
    set(value) { putIfNotNull(CommonConfigurationKeys.ENUM_WHEN_TRACKER, value) }

var CompilerConfiguration.importTracker: ImportTracker?
    get() = get(CommonConfigurationKeys.IMPORT_TRACKER)
    set(value) { putIfNotNull(CommonConfigurationKeys.IMPORT_TRACKER, value) }

var CompilerConfiguration.metadataVersion: BinaryVersion?
    get() = get(CommonConfigurationKeys.METADATA_VERSION)
    set(value) { put(CommonConfigurationKeys.METADATA_VERSION, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.useFir: Boolean
    get() = getBoolean(CommonConfigurationKeys.USE_FIR)
    set(value) { put(CommonConfigurationKeys.USE_FIR, value) }

var CompilerConfiguration.useLightTree: Boolean
    get() = getBoolean(CommonConfigurationKeys.USE_LIGHT_TREE)
    set(value) { put(CommonConfigurationKeys.USE_LIGHT_TREE, value) }

var CompilerConfiguration.hmppModuleStructure: HmppCliModuleStructure?
    get() = get(CommonConfigurationKeys.HMPP_MODULE_STRUCTURE)
    set(value) { put(CommonConfigurationKeys.HMPP_MODULE_STRUCTURE, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.metadataKlib: Boolean
    get() = getBoolean(CommonConfigurationKeys.METADATA_KLIB)
    set(value) { put(CommonConfigurationKeys.METADATA_KLIB, value) }

var CompilerConfiguration.useFirExtraCheckers: Boolean
    get() = getBoolean(CommonConfigurationKeys.USE_FIR_EXTRA_CHECKERS)
    set(value) { put(CommonConfigurationKeys.USE_FIR_EXTRA_CHECKERS, value) }

var CompilerConfiguration.useFirExperimentalCheckers: Boolean
    get() = getBoolean(CommonConfigurationKeys.USE_FIR_EXPERIMENTAL_CHECKERS)
    set(value) { put(CommonConfigurationKeys.USE_FIR_EXPERIMENTAL_CHECKERS, value) }

var CompilerConfiguration.dumpInferenceLogs: Boolean
    get() = getBoolean(CommonConfigurationKeys.DUMP_INFERENCE_LOGS)
    set(value) { put(CommonConfigurationKeys.DUMP_INFERENCE_LOGS, value) }

var CompilerConfiguration.parallelBackendThreads: Int?
    get() = get(CommonConfigurationKeys.PARALLEL_BACKEND_THREADS)
    set(value) { put(CommonConfigurationKeys.PARALLEL_BACKEND_THREADS, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.dumpModel: String?
    get() = get(CommonConfigurationKeys.DUMP_MODEL)
    set(value) { put(CommonConfigurationKeys.DUMP_MODEL, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.incrementalCompilation: Boolean
    get() = getBoolean(CommonConfigurationKeys.INCREMENTAL_COMPILATION)
    set(value) { put(CommonConfigurationKeys.INCREMENTAL_COMPILATION, value) }

var CompilerConfiguration.allowAnyScriptsInSourceRoots: Boolean
    get() = getBoolean(CommonConfigurationKeys.ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS)
    set(value) { put(CommonConfigurationKeys.ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS, value) }

var CompilerConfiguration.ignoreConstOptimizationErrors: Boolean
    get() = getBoolean(CommonConfigurationKeys.IGNORE_CONST_OPTIMIZATION_ERRORS)
    set(value) { put(CommonConfigurationKeys.IGNORE_CONST_OPTIMIZATION_ERRORS, value) }

var CompilerConfiguration.evaluatedConstTracker: EvaluatedConstTracker?
    get() = get(CommonConfigurationKeys.EVALUATED_CONST_TRACKER)
    set(value) { put(CommonConfigurationKeys.EVALUATED_CONST_TRACKER, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.messageCollector: MessageCollector
    get() = get(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
    set(value) { put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, value) }

var CompilerConfiguration.verifyIr: IrVerificationMode?
    get() = get(CommonConfigurationKeys.VERIFY_IR)
    set(value) { put(CommonConfigurationKeys.VERIFY_IR, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.enableIrVisibilityChecks: Boolean
    get() = getBoolean(CommonConfigurationKeys.ENABLE_IR_VISIBILITY_CHECKS)
    set(value) { put(CommonConfigurationKeys.ENABLE_IR_VISIBILITY_CHECKS, value) }

var CompilerConfiguration.enableIrVarargTypesChecks: Boolean
    get() = getBoolean(CommonConfigurationKeys.ENABLE_IR_VARARG_TYPES_CHECKS)
    set(value) { put(CommonConfigurationKeys.ENABLE_IR_VARARG_TYPES_CHECKS, value) }

var CompilerConfiguration.enableIrNestedOffsetsChecks: Boolean
    get() = getBoolean(CommonConfigurationKeys.ENABLE_IR_NESTED_OFFSETS_CHECKS)
    set(value) { put(CommonConfigurationKeys.ENABLE_IR_NESTED_OFFSETS_CHECKS, value) }

var CompilerConfiguration.phaseConfig: PhaseConfig?
    get() = get(CommonConfigurationKeys.PHASE_CONFIG)
    set(value) { put(CommonConfigurationKeys.PHASE_CONFIG, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.dontCreateSeparateSessionForScripts: Boolean
    get() = getBoolean(CommonConfigurationKeys.DONT_CREATE_SEPARATE_SESSION_FOR_SCRIPTS)
    set(value) { put(CommonConfigurationKeys.DONT_CREATE_SEPARATE_SESSION_FOR_SCRIPTS, value) }

var CompilerConfiguration.dontSortSourceFiles: Boolean
    get() = getBoolean(CommonConfigurationKeys.DONT_SORT_SOURCE_FILES)
    set(value) { put(CommonConfigurationKeys.DONT_SORT_SOURCE_FILES, value) }

var CompilerConfiguration.scriptingHostConfiguration: Any?
    get() = get(CommonConfigurationKeys.SCRIPTING_HOST_CONFIGURATION)
    set(value) { put(CommonConfigurationKeys.SCRIPTING_HOST_CONFIGURATION, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.perfManager: PerformanceManager?
    get() = get(CommonConfigurationKeys.PERF_MANAGER)
    set(value) { put(CommonConfigurationKeys.PERF_MANAGER, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.detailedPerf: Boolean
    get() = getBoolean(CommonConfigurationKeys.DETAILED_PERF)
    set(value) { put(CommonConfigurationKeys.DETAILED_PERF, value) }

var CompilerConfiguration.targetPlatform: TargetPlatform?
    get() = get(CommonConfigurationKeys.TARGET_PLATFORM)
    set(value) { put(CommonConfigurationKeys.TARGET_PLATFORM, requireNotNull(value) { "nullable values are not allowed" }) }

