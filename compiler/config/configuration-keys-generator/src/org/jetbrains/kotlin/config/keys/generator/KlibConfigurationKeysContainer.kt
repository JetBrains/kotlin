/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.keys.generator

import org.jetbrains.kotlin.config.DuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.config.keys.generator.model.KeysContainer

@Suppress("unused")
object KlibConfigurationKeysContainer : KeysContainer("org.jetbrains.kotlin.config", "KlibConfigurationKeys") {
    val KLIB_RELATIVE_PATH_BASES by key<List<String>>("Provides a path from which relative paths in klib are being computed")

    val KLIB_NORMALIZE_ABSOLUTE_PATH by key<Boolean>("Normalize absolute paths in klib (replace file separator with '/')")

    val PRODUCE_KLIB_SIGNATURES_CLASH_CHECKS by key<Boolean>("Turn on the checks on uniqueness of signatures")

    val SYNTHETIC_ACCESSORS_DUMP_DIR by key<String>("Path to a directory to dump synthetic accessors and their use sites")

    val SYNTHETIC_ACCESSORS_WITH_NARROWED_VISIBILITY by key<Boolean>(
        "Narrow the visibility of generated synthetic accessors to _internal_" +
                " if such accessors are only used in inline functions that are not a part of public ABI"
    )

    val DUPLICATED_UNIQUE_NAME_STRATEGY by key<DuplicatedUniqueNameStrategy>("Duplicated KLIB dependencies handling strategy")
}
