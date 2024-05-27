/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

object KlibConfigurationKeys {
    @JvmField
    val KLIB_RELATIVE_PATH_BASES: CompilerConfigurationKey<List<String>> =
        CompilerConfigurationKey.create("Provides a path from which relative paths in klib are being computed")

    @JvmField
    val KLIB_NORMALIZE_ABSOLUTE_PATH: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("Normalize absolute paths in klib (replace file separator with '/')")

    @JvmField
    val PRODUCE_KLIB_SIGNATURES_CLASH_CHECKS: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("Turn on the checks on uniqueness of signatures")

}
