/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

/**
 * Shared Java literal parsing helpers used by both [ConstantEvaluator] and
 * annotation argument evaluation in [JavaAnnotationOverAst].
 *
 * These functions parse the textual form of Java literal tokens (integer, long,
 * float, double) and unescape string/character literal contents. They are
 * intentionally forgiving: on malformed input a zero/empty value is returned
 * rather than throwing, because upstream AST nodes may carry partially-recovered
 * text from the KMP Java parser.
 */
internal object JavaLiteralParser {
    /**
     * Parses the text of an `INTEGER_LITERAL` token. Returns either [Int] or [Long]
     * depending on whether the value fits into an Int. Recognizes hex (`0x`), binary
     * (`0b`), and octal (leading `0`) prefixes, as well as underscore separators.
     */
    fun parseIntegerLiteral(text: String): Any {
        val cleaned = text.replace("_", "")
        return when {
            cleaned.startsWith("0x") || cleaned.startsWith("0X") ->
                cleaned.substring(2).toIntOrNull(16) ?: cleaned.substring(2).toLongOrNull(16) ?: 0
            cleaned.startsWith("0b") || cleaned.startsWith("0B") ->
                cleaned.substring(2).toIntOrNull(2) ?: cleaned.substring(2).toLongOrNull(2) ?: 0
            cleaned.startsWith("0") && cleaned.length > 1 && cleaned.all { it in '0'..'7' } ->
                cleaned.toIntOrNull(8) ?: cleaned.toLongOrNull(8) ?: 0
            else -> cleaned.toIntOrNull() ?: cleaned.toLongOrNull() ?: 0
        }
    }

    /**
     * Parses the text of a `LONG_LITERAL` token (with trailing `L`/`l` suffix).
     * Recognizes hex, binary, and octal prefixes, as well as underscore separators.
     */
    fun parseLongLiteral(text: String): Long {
        val cleaned = text.replace("_", "").removeSuffix("L").removeSuffix("l")
        return when {
            cleaned.startsWith("0x") || cleaned.startsWith("0X") ->
                cleaned.substring(2).toLongOrNull(16) ?: 0L
            cleaned.startsWith("0b") || cleaned.startsWith("0B") ->
                cleaned.substring(2).toLongOrNull(2) ?: 0L
            cleaned.startsWith("0") && cleaned.length > 1 ->
                cleaned.toLongOrNull(8) ?: 0L
            else -> cleaned.toLongOrNull() ?: 0L
        }
    }

    /** Parses the text of a `FLOAT_LITERAL` token (with optional `F`/`f` suffix). */
    fun parseFloatLiteral(text: String): Float {
        val cleaned = text.replace("_", "").removeSuffix("F").removeSuffix("f")
        return cleaned.toFloatOrNull() ?: 0f
    }

    /** Parses the text of a `DOUBLE_LITERAL` token (with optional `D`/`d` suffix). */
    fun parseDoubleLiteral(text: String): Double {
        val cleaned = text.replace("_", "").removeSuffix("D").removeSuffix("d")
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    /**
     * Unescapes the textual body of a Java string/character literal (the content
     * between the surrounding quotes). Supports standard escapes (`\n`, `\t`, `\r`,
     * `\b`, `\f`, `\'`, `\"`, `\\`), unicode escapes (`\uXXXX`), and 1–3 digit octal
     * escapes.
     */
    fun unescapeJavaString(text: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            if (text[i] == '\\' && i + 1 < text.length) {
                when (text[i + 1]) {
                    'n' -> {
                        sb.append('\n'); i += 2
                    }
                    't' -> {
                        sb.append('\t'); i += 2
                    }
                    'r' -> {
                        sb.append('\r'); i += 2
                    }
                    'b' -> {
                        sb.append('\b'); i += 2
                    }
                    'f' -> {
                        sb.append('\u000C'); i += 2
                    }
                    '\'' -> {
                        sb.append('\''); i += 2
                    }
                    '"' -> {
                        sb.append('"'); i += 2
                    }
                    '\\' -> {
                        sb.append('\\'); i += 2
                    }
                    'u' -> {
                        if (i + 5 < text.length) {
                            val hex = text.substring(i + 2, i + 6)
                            val code = hex.toIntOrNull(16)
                            if (code != null) {
                                sb.append(code.toChar())
                                i += 6
                            } else {
                                sb.append(text[i])
                                i++
                            }
                        } else {
                            sb.append(text[i])
                            i++
                        }
                    }
                    in '0'..'7' -> {
                        // Octal escape: 1–3 digits
                        var end = i + 2
                        while (end < text.length && end < i + 4 && text[end] in '0'..'7') end++
                        val octal = text.substring(i + 1, end)
                        val code = octal.toIntOrNull(8)
                        if (code != null) {
                            sb.append(code.toChar())
                            i = end
                        } else {
                            sb.append(text[i])
                            i++
                        }
                    }
                    else -> {
                        sb.append(text[i]); i++
                    }
                }
            } else {
                sb.append(text[i])
                i++
            }
        }
        return sb.toString()
    }
}
