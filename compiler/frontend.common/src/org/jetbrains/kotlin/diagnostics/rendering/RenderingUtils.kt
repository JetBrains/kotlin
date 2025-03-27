/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics.rendering

import org.jetbrains.kotlin.config.LanguageFeature

fun String.toDeprecationWarningMessage(deprecatingFeature: LanguageFeature): String {
    return buildString {
        append(this@toDeprecationWarningMessage)
        when {
            endsWith(".") -> append(" ")
            lastOrNull()?.isWhitespace() == true -> {}
            else -> append(". ")
        }
        appendDeprecationWarningSuffix(deprecatingFeature)
    }
}

fun StringBuilder.appendDeprecationWarningSuffix(deprecatingFeature: LanguageFeature, ticket: String? = null) {
    append("This will become an error")
    val sinceVersion = deprecatingFeature.sinceVersion
    if (sinceVersion != null) {
        append(" in language version ")
        append(sinceVersion.versionString)
    } else {
        append(" in a future release")
    }
    append(".")

    ticket?.let {
        append(" See https://youtrack.jetbrains.com/issue/")
        append(it)
        append(".")
    }
}