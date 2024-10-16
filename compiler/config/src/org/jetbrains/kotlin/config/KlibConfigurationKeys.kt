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

    @JvmField
    val ENABLE_IR_VISIBILITY_CHECKS_AFTER_INLINING =
        CompilerConfigurationKey.create<Boolean>("Check post-inlining IR for visibility violations")

    @JvmField
    val NO_DOUBLE_INLINING: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("Turns off double-inlining mode")

    @JvmField
    val SYNTHETIC_ACCESSORS_DUMP_DIR: CompilerConfigurationKey<String?> =
        CompilerConfigurationKey.create("Path to a directory to dump synthetic accessors and their use sites")

    @JvmField
    val SYNTHETIC_ACCESSORS_WITH_NARROWED_VISIBILITY: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create(
            "Narrow the visibility of generated synthetic accessors to _internal_" +
                    " if such accessors are only used in inline functions that are not a part of public ABI"
        )

    @JvmField
    val DUPLICATED_UNIQUE_NAME_STRATEGY: CompilerConfigurationKey<DuplicatedUniqueNameStrategy> =
        CompilerConfigurationKey.create("Duplicated KLIB dependencies handling strategy")
}

enum class DuplicatedUniqueNameStrategy(val alias: String) {
    DENY(Aliases.DENY),
    ALLOW_ALL_WITH_WARNING(Aliases.ALLOW_ALL_WITH_WARNING),
    ALLOW_FIRST_WITH_WARNING(Aliases.ALLOW_FIRST_WITH_WARNING),
    ;

    override fun toString() = alias

    private object Aliases {
        const val DENY = "deny"
        const val ALLOW_ALL_WITH_WARNING = "allow-all-with-warning"
        const val ALLOW_FIRST_WITH_WARNING = "allow-first-with-warning"
    }

    companion object {
        const val ALL_ALIASES = "${Aliases.DENY}|${Aliases.ALLOW_ALL_WITH_WARNING}|${Aliases.ALLOW_FIRST_WITH_WARNING}"

        fun parseOrDefault(flagValue: String?, default: DuplicatedUniqueNameStrategy): DuplicatedUniqueNameStrategy =
            entries.singleOrNull { it.alias == flagValue } ?: default
    }
}
