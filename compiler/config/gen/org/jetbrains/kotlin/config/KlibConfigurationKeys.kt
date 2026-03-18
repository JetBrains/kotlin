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

import org.jetbrains.kotlin.konan.file.ZipFileSystemAccessor
import org.jetbrains.kotlin.library.KotlinAbiVersion

object KlibConfigurationKeys {
    // Provides a path from which relative paths in klib are being computed.
    @JvmField
    val KLIB_RELATIVE_PATH_BASES = CompilerConfigurationKey.create<List<String>>("KLIB_RELATIVE_PATH_BASES")

    // Normalize absolute paths in klib (replace file separator with '/').
    @JvmField
    val KLIB_NORMALIZE_ABSOLUTE_PATH = CompilerConfigurationKey.create<Boolean>("KLIB_NORMALIZE_ABSOLUTE_PATH")

    // Turn on the checks on uniqueness of signatures.
    @JvmField
    val PRODUCE_KLIB_SIGNATURES_CLASH_CHECKS = CompilerConfigurationKey.create<Boolean>("PRODUCE_KLIB_SIGNATURES_CLASH_CHECKS")

    // Duplicated KLIB dependencies handling strategy.
    @JvmField
    val DUPLICATED_UNIQUE_NAME_STRATEGY = CompilerConfigurationKey.create<DuplicatedUniqueNameStrategy>("DUPLICATED_UNIQUE_NAME_STRATEGY")

    @JvmField
    val CUSTOM_KLIB_ABI_VERSION = CompilerConfigurationKey.create<KotlinAbiVersion>("CUSTOM_KLIB_ABI_VERSION")

    @JvmField
    val KLIB_ABI_COMPATIBILITY_LEVEL = CompilerConfigurationKey.create<KlibAbiCompatibilityLevel>("KLIB_ABI_COMPATIBILITY_LEVEL")

    // Zip file system accessor, used for klib reading.
    @JvmField
    val ZIP_FILE_SYSTEM_ACCESSOR = CompilerConfigurationKey.create<ZipFileSystemAccessor>("ZIP_FILE_SYSTEM_ACCESSOR")

    // Skip library compatibility checks for stdlib and kotlin.test library.
    @JvmField
    val SKIP_LIBRARY_SPECIAL_COMPATIBILITY_CHECKS = CompilerConfigurationKey.create<Boolean>("SKIP_LIBRARY_SPECIAL_COMPATIBILITY_CHECKS")

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

var CompilerConfiguration.duplicatedUniqueNameStrategy: DuplicatedUniqueNameStrategy?
    get() = get(KlibConfigurationKeys.DUPLICATED_UNIQUE_NAME_STRATEGY)
    set(value) { put(KlibConfigurationKeys.DUPLICATED_UNIQUE_NAME_STRATEGY, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.customKlibAbiVersion: KotlinAbiVersion?
    get() = get(KlibConfigurationKeys.CUSTOM_KLIB_ABI_VERSION)
    set(value) { putIfNotNull(KlibConfigurationKeys.CUSTOM_KLIB_ABI_VERSION, value) }

var CompilerConfiguration.klibAbiCompatibilityLevel: KlibAbiCompatibilityLevel
    get() = get(KlibConfigurationKeys.KLIB_ABI_COMPATIBILITY_LEVEL, KlibAbiCompatibilityLevel.LATEST_STABLE)
    set(value) { put(KlibConfigurationKeys.KLIB_ABI_COMPATIBILITY_LEVEL, value) }

var CompilerConfiguration.zipFileSystemAccessor: ZipFileSystemAccessor?
    get() = get(KlibConfigurationKeys.ZIP_FILE_SYSTEM_ACCESSOR)
    set(value) { put(KlibConfigurationKeys.ZIP_FILE_SYSTEM_ACCESSOR, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.skipLibrarySpecialCompatibilityChecks: Boolean
    get() = getBoolean(KlibConfigurationKeys.SKIP_LIBRARY_SPECIAL_COMPATIBILITY_CHECKS)
    set(value) { put(KlibConfigurationKeys.SKIP_LIBRARY_SPECIAL_COMPATIBILITY_CHECKS, value) }

