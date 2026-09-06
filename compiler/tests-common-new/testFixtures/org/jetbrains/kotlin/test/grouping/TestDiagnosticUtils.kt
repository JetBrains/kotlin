/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

import java.security.MessageDigest

fun String.toBoundedDiagnostic(maxLength: Int): String {
    if (length <= maxLength) return this

    val suffix = "... [truncated; original length=$length chars; SHA-256=${sha256Hex()}]"
    val prefixLength = (maxLength - suffix.length).coerceAtLeast(0)
    return take(prefixLength) + suffix.take(maxLength - prefixLength)
}

fun String.sha256Hex(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    var offset = 0
    while (offset < length) {
        var end = minOf(offset + 4096, length)
        if (end < length && Character.isHighSurrogate(this[end - 1])) end--
        if (end == offset) end++
        digest.update(substring(offset, end).toByteArray(Charsets.UTF_8))
        offset = end
    }

    val hexDigits = "0123456789abcdef"
    return buildString(64) {
        for (byte in digest.digest()) {
            val value = byte.toInt() and 0xFF
            append(hexDigits[value ushr 4])
            append(hexDigits[value and 0x0F])
        }
    }
}
