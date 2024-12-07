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


object KlibConfigurationKeys {
    @JvmField
    val KLIB_RELATIVE_PATH_BASES = CompilerConfigurationKey.create<List<String>>("Provides a path from which relative paths in klib are being computed")

    @JvmField
    val KLIB_NORMALIZE_ABSOLUTE_PATH = CompilerConfigurationKey.create<Boolean>("Normalize absolute paths in klib (replace file separator with '/')")

    @JvmField
    val PRODUCE_KLIB_SIGNATURES_CLASH_CHECKS = CompilerConfigurationKey.create<Boolean>("Turn on the checks on uniqueness of signatures")

    @JvmField
    val ENABLE_IR_VISIBILITY_CHECKS_AFTER_INLINING = CompilerConfigurationKey.create<Boolean>("Check post-inlining IR for visibility violations")

    @JvmField
    val NO_DOUBLE_INLINING = CompilerConfigurationKey.create<Boolean>("Turns off double-inlining mode")

    @JvmField
    val SYNTHETIC_ACCESSORS_DUMP_DIR = CompilerConfigurationKey.create<String>("Path to a directory to dump synthetic accessors and their use sites")

    @JvmField
    val SYNTHETIC_ACCESSORS_WITH_NARROWED_VISIBILITY = CompilerConfigurationKey.create<Boolean>("Narrow the visibility of generated synthetic accessors to _internal_ if such accessors are only used in inline functions that are not a part of public ABI")

    @JvmField
    val DUPLICATED_UNIQUE_NAME_STRATEGY = CompilerConfigurationKey.create<DuplicatedUniqueNameStrategy>("Duplicated KLIB dependencies handling strategy")

}

var CompilerConfiguration.klibRelativePathBases: List<String>
    get() = getList(KlibConfigurationKeys.KLIB_RELATIVE_PATH_BASES)
    set(value) { put(KlibConfigurationKeys.KLIB_RELATIVE_PATH_BASES, value) }

var CompilerConfiguration.klibNormalizeAbsolutePath: Boolean
    get() = getBoolean(KlibConfigurationKeys.KLIB_NORMALIZE_ABSOLUTE_PATH)
    set(value) { put(KlibConfigurationKeys.KLIB_NORMALIZE_ABSOLUTE_PATH, value) }

var CompilerConfiguration.produceKlibSignaturesClashChecks: Boolean
    get() = getBoolean(KlibConfigurationKeys.PRODUCE_KLIB_SIGNATURES_CLASH_CHECKS)
    set(value) { put(KlibConfigurationKeys.PRODUCE_KLIB_SIGNATURES_CLASH_CHECKS, value) }

var CompilerConfiguration.enableIrVisibilityChecksAfterInlining: Boolean
    get() = getBoolean(KlibConfigurationKeys.ENABLE_IR_VISIBILITY_CHECKS_AFTER_INLINING)
    set(value) { put(KlibConfigurationKeys.ENABLE_IR_VISIBILITY_CHECKS_AFTER_INLINING, value) }

var CompilerConfiguration.noDoubleInlining: Boolean
    get() = getBoolean(KlibConfigurationKeys.NO_DOUBLE_INLINING)
    set(value) { put(KlibConfigurationKeys.NO_DOUBLE_INLINING, value) }

var CompilerConfiguration.syntheticAccessorsDumpDir: String?
    get() = get(KlibConfigurationKeys.SYNTHETIC_ACCESSORS_DUMP_DIR)
    set(value) { put(KlibConfigurationKeys.SYNTHETIC_ACCESSORS_DUMP_DIR, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.syntheticAccessorsWithNarrowedVisibility: Boolean
    get() = getBoolean(KlibConfigurationKeys.SYNTHETIC_ACCESSORS_WITH_NARROWED_VISIBILITY)
    set(value) { put(KlibConfigurationKeys.SYNTHETIC_ACCESSORS_WITH_NARROWED_VISIBILITY, value) }

var CompilerConfiguration.duplicatedUniqueNameStrategy: DuplicatedUniqueNameStrategy?
    get() = get(KlibConfigurationKeys.DUPLICATED_UNIQUE_NAME_STRATEGY)
    set(value) { put(KlibConfigurationKeys.DUPLICATED_UNIQUE_NAME_STRATEGY, requireNotNull(value) { "nullable values are not allowed" }) }

