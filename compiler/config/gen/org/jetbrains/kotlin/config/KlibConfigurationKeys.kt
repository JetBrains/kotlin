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

import org.jetbrains.kotlin.library.KotlinAbiVersion

object KlibConfigurationKeys {
    @JvmField
    val KLIB_RELATIVE_PATH_BASES = CompilerConfigurationKey.create<List<String>>("Provides a path from which relative paths in klib are being computed")

    @JvmField
    val KLIB_NORMALIZE_ABSOLUTE_PATH = CompilerConfigurationKey.create<Boolean>("Normalize absolute paths in klib (replace file separator with '/')")

    @JvmField
    val PRODUCE_KLIB_SIGNATURES_CLASH_CHECKS = CompilerConfigurationKey.create<Boolean>("Turn on the checks on uniqueness of signatures")

    @JvmField
    val SYNTHETIC_ACCESSORS_DUMP_DIR = CompilerConfigurationKey.create<String>("Path to a directory to dump synthetic accessors and their use sites")

    @JvmField
    val DUPLICATED_UNIQUE_NAME_STRATEGY = CompilerConfigurationKey.create<DuplicatedUniqueNameStrategy>("Duplicated KLIB dependencies handling strategy")

    @JvmField
    val CUSTOM_KLIB_ABI_VERSION = CompilerConfigurationKey.create<KotlinAbiVersion>("Custom KLIB ABI version")

    @JvmField
    val KLIB_ABI_COMPATIBILITY_LEVEL = CompilerConfigurationKey.create<KlibAbiCompatibilityLevel>("KLIB ABI compatibility level")

    @JvmField
    val PARTIAL_LINKAGE_MODE = CompilerConfigurationKey.create<PartialLinkageMode>("Sets partial linkage mode")

    @JvmField
    val PARTIAL_LINKAGE_LOG_LEVEL = CompilerConfigurationKey.create<PartialLinkageLogLevel>("Sets partial linkage log level")

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

var CompilerConfiguration.syntheticAccessorsDumpDir: String?
    get() = get(KlibConfigurationKeys.SYNTHETIC_ACCESSORS_DUMP_DIR)
    set(value) { put(KlibConfigurationKeys.SYNTHETIC_ACCESSORS_DUMP_DIR, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.duplicatedUniqueNameStrategy: DuplicatedUniqueNameStrategy?
    get() = get(KlibConfigurationKeys.DUPLICATED_UNIQUE_NAME_STRATEGY)
    set(value) { put(KlibConfigurationKeys.DUPLICATED_UNIQUE_NAME_STRATEGY, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.customKlibAbiVersion: KotlinAbiVersion?
    get() = get(KlibConfigurationKeys.CUSTOM_KLIB_ABI_VERSION)
    set(value) { putIfNotNull(KlibConfigurationKeys.CUSTOM_KLIB_ABI_VERSION, value) }

var CompilerConfiguration.klibAbiCompatibilityLevel: KlibAbiCompatibilityLevel
    get() = get(KlibConfigurationKeys.KLIB_ABI_COMPATIBILITY_LEVEL, KlibAbiCompatibilityLevel.LATEST_STABLE)
    set(value) { put(KlibConfigurationKeys.KLIB_ABI_COMPATIBILITY_LEVEL, value) }

var CompilerConfiguration.partialLinkageMode: PartialLinkageMode
    get() = get(KlibConfigurationKeys.PARTIAL_LINKAGE_MODE, PartialLinkageMode.DEFAULT)
    set(value) { put(KlibConfigurationKeys.PARTIAL_LINKAGE_MODE, value) }

var CompilerConfiguration.partialLinkageLogLevel: PartialLinkageLogLevel
    get() = get(KlibConfigurationKeys.PARTIAL_LINKAGE_LOG_LEVEL, PartialLinkageLogLevel.DEFAULT)
    set(value) { put(KlibConfigurationKeys.PARTIAL_LINKAGE_LOG_LEVEL, value) }

