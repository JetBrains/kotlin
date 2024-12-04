/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

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
