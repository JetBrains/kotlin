/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.config.LanguageFeature

object CliArgumentStringBuilder {
    const val languagePrefix = "-XXLanguage:"

    private val LanguageFeature.State.sign: String
        get() = when (this) {
            LanguageFeature.State.ENABLED -> "+"
            LanguageFeature.State.DISABLED -> "-"
            LanguageFeature.State.ENABLED_WITH_WARNING -> "+" // not supported normally
            LanguageFeature.State.ENABLED_WITH_ERROR -> "-" // not supported normally
        }

    fun LanguageFeature.buildArgumentString(state: LanguageFeature.State): String {
        return "$languagePrefix${state.sign}$name"
    }
}