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
import org.jetbrains.kotlin.incremental.components.ImportTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion

object CommonConfigurationKeys {
    @JvmField
    val LANGUAGE_VERSION_SETTINGS = CompilerConfigurationKey.create<LanguageVersionSettings>("language version settings")

    @JvmField
    val DISABLE_INLINE = CompilerConfigurationKey.create<Boolean>("disable inline")

    @JvmField
    val MODULE_NAME = CompilerConfigurationKey.create<String>("module name")

    @JvmField
    val REPORT_OUTPUT_FILES = CompilerConfigurationKey.create<Boolean>("report output files")

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
    val USE_FIR_EXTRA_CHECKERS = CompilerConfigurationKey.create<Boolean>("fir extra checkers")

    @JvmField
    val USE_FIR_EXPERIMENTAL_CHECKERS = CompilerConfigurationKey.create<Boolean>("fir not-public-ready checkers")

    @JvmField
    val DUMP_CONSTRAINTS = CompilerConfigurationKey.create<Boolean>("render the constraints dump file")

    @JvmField
    val PARALLEL_BACKEND_THREADS = CompilerConfigurationKey.create<Int>("Run codegen phase in parallel with N threads")

    @JvmField
    val INCREMENTAL_COMPILATION = CompilerConfigurationKey.create<Boolean>("Enable incremental compilation")

    @JvmField
    val ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS = CompilerConfigurationKey.create<Boolean>("Allow to compile any scripts along with regular Kotlin sources")

    @JvmField
    val IGNORE_CONST_OPTIMIZATION_ERRORS = CompilerConfigurationKey.create<Boolean>("Ignore errors from IrConstTransformer")

    @JvmField
    val EVALUATED_CONST_TRACKER = CompilerConfigurationKey.create<EvaluatedConstTracker>("Keeps track of all evaluated by IrInterpreter constants")

    @JvmField
    val MESSAGE_COLLECTOR_KEY = CompilerConfigurationKey.create<MessageCollector>("message collector")

    @JvmField
    val VERIFY_IR = CompilerConfigurationKey.create<IrVerificationMode>("IR verification mode")

    @JvmField
    val ENABLE_IR_VISIBILITY_CHECKS = CompilerConfigurationKey.create<Boolean>("Check pre-lowering IR for visibility violations")

    @JvmField
    val ENABLE_IR_VARARG_TYPES_CHECKS = CompilerConfigurationKey.create<Boolean>("Check IR for vararg types mismatches")

    @JvmField
    val PHASE_CONFIG = CompilerConfigurationKey.create<PhaseConfig>("phase configuration")

    // Should be used only in tests, impossible to set via compiler arguments
    @JvmField
    val DONT_CREATE_SEPARATE_SESSION_FOR_SCRIPTS = CompilerConfigurationKey.create<Boolean>("don't create separate session for scripts")

    // Should be used only in tests, impossible to set via compiler arguments
    @JvmField
    val DONT_SORT_SOURCE_FILES = CompilerConfigurationKey.create<Boolean>("don't sort source files in FS order")

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

var CompilerConfiguration.dumpConstraints: Boolean
    get() = getBoolean(CommonConfigurationKeys.DUMP_CONSTRAINTS)
    set(value) { put(CommonConfigurationKeys.DUMP_CONSTRAINTS, value) }

var CompilerConfiguration.parallelBackendThreads: Int?
    get() = get(CommonConfigurationKeys.PARALLEL_BACKEND_THREADS)
    set(value) { put(CommonConfigurationKeys.PARALLEL_BACKEND_THREADS, requireNotNull(value) { "nullable values are not allowed" }) }

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

var CompilerConfiguration.phaseConfig: PhaseConfig?
    get() = get(CommonConfigurationKeys.PHASE_CONFIG)
    set(value) { put(CommonConfigurationKeys.PHASE_CONFIG, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.dontCreateSeparateSessionForScripts: Boolean
    get() = getBoolean(CommonConfigurationKeys.DONT_CREATE_SEPARATE_SESSION_FOR_SCRIPTS)
    set(value) { put(CommonConfigurationKeys.DONT_CREATE_SEPARATE_SESSION_FOR_SCRIPTS, value) }

var CompilerConfiguration.dontSortSourceFiles: Boolean
    get() = getBoolean(CommonConfigurationKeys.DONT_SORT_SOURCE_FILES)
    set(value) { put(CommonConfigurationKeys.DONT_SORT_SOURCE_FILES, value) }

