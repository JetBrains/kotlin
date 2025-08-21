/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.util

import org.jetbrains.kotlin.config.LanguageFeature
import java.util.regex.Pattern

val LANGUAGE_FEATURE_PATTERN: Pattern = Pattern.compile("""([+\-])(\w+)\s*""")

fun String.parseLanguageFeature(): Pair<LanguageFeature, LanguageFeature.State> {
    val matcher = LANGUAGE_FEATURE_PATTERN.matcher(this)
    if (!matcher.find()) {
        error(
            """Wrong syntax in the '// LANGUAGE: ...' directive:
                   found: '${this}'
                   Must be '((+|-|warn:)LanguageFeatureName)+'
                   where '+' means 'enable', '-' means 'disable', 'warn:' means 'enable with warning'
                   and language feature names are names of enum entries in LanguageFeature enum class"""
        )
    }
    val mode = when (val mode = matcher.group(1)) {
        "+" -> LanguageFeature.State.ENABLED
        "-" -> LanguageFeature.State.DISABLED
        else -> error("Unknown mode for language feature: $mode")
    }
    val name = matcher.group(2)
    val feature = LanguageFeature.fromString(name) ?: error("Language feature with name \"$name\" not found")
    return Pair(feature, mode)
}

