/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.utils.printer

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail

/** Renders [value] as a Kotlin string literal, escaping the characters which cannot appear in one verbatim. */
@KaImplementationDetail
public fun renderKotlinStringLiteral(value: String): String = buildString(value.length + 2) {
    append('"')
    for (character in value) {
        val escaped = character.escapedInKotlinLiteral(quote = '"')
        if (escaped != null) append(escaped) else append(character)
    }
    append('"')
}

/** Renders [value] as a Kotlin character literal, escaping it when it cannot appear in one verbatim. */
@KaImplementationDetail
public fun renderKotlinCharLiteral(value: Char): String = "'" + (value.escapedInKotlinLiteral(quote = '\'') ?: value) + "'"

/**
 * The escape sequence for [this] character inside a Kotlin literal delimited by [quote], or `null` if the character
 * can be rendered as is.
 *
 * Non-printable characters have to be escaped as the rendered text is expected to be readable,
 * and some of them (e.g., a line separator) would break the literal.
 */
@KaImplementationDetail
public fun Char.escapedInKotlinLiteral(quote: Char): String? = when (this) {
    '\\' -> ESCAPE + '\\'
    quote -> ESCAPE + quote
    '$' -> "$ESCAPE$"
    '\n' -> ESCAPE + 'n'
    '\r' -> ESCAPE + 'r'
    '\t' -> ESCAPE + 't'
    '\b' -> ESCAPE + 'b'
    else -> if (isPrintable()) null else ESCAPE + UNICODE_ESCAPE_MARKER + "%04X".format(code)
}

/**
 * Mirrors the printability check of `org.jetbrains.kotlin.constant.CharValue`.
 */
private fun Char.isPrintable(): Boolean = when (Character.getType(this).toByte()) {
    Character.UNASSIGNED,
    Character.LINE_SEPARATOR,
    Character.PARAGRAPH_SEPARATOR,
    Character.CONTROL,
    Character.FORMAT,
    Character.PRIVATE_USE,
    Character.SURROGATE,
        -> false

    else -> true
}

private const val ESCAPE = "\\"
private const val UNICODE_ESCAPE_MARKER = 'u'
