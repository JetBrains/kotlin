/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.keys.generator

import org.jetbrains.kotlin.config.DuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.config.KlibAbiCompatibilityLevel
import org.jetbrains.kotlin.config.keys.generator.model.KeysContainer
import org.jetbrains.kotlin.library.KotlinAbiVersion

@Suppress("unused")
object KlibConfigurationKeysContainer : KeysContainer("org.jetbrains.kotlin.config", "KlibConfigurationKeys") {
    val KLIB_RELATIVE_PATH_BASES by key<List<String>>("Provides a path from which relative paths in klib are being computed")

    val KLIB_NORMALIZE_ABSOLUTE_PATH by key<Boolean>("Normalize absolute paths in klib (replace file separator with '/')")

    val PRODUCE_KLIB_SIGNATURES_CLASH_CHECKS by key<Boolean>("Turn on the checks on uniqueness of signatures")

    val DUPLICATED_UNIQUE_NAME_STRATEGY by key<DuplicatedUniqueNameStrategy>("Duplicated KLIB dependencies handling strategy")

    val CUSTOM_KLIB_ABI_VERSION by key<KotlinAbiVersion>("Custom KLIB ABI version", throwOnNull = false)

    val KLIB_ABI_COMPATIBILITY_LEVEL by key<KlibAbiCompatibilityLevel>(
        "KLIB ABI compatibility level",
        defaultValue = "KlibAbiCompatibilityLevel.LATEST_STABLE"
    )
}
