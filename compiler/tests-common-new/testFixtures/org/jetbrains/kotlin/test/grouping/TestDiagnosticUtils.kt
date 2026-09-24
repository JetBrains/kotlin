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

/**
 * Like [toBoundedDiagnostic], but retains both ends of the text. A captured process output carries its most useful
 * part at the tail: the trap, the stack trace, or the last record the process managed to print before it died.
 */
fun String.toBoundedDiagnosticKeepingTail(maxLength: Int): String {
    if (length <= maxLength) return this

    val marker = "\n... [truncated; original length=$length chars; SHA-256=${sha256Hex()}; middle omitted] ...\n"
    val retainedLength = (maxLength - marker.length).coerceAtLeast(0)
    if (retainedLength == 0) return marker.take(maxLength)
    val headLength = retainedLength / 2
    val tailLength = retainedLength - headLength
    return take(headLength) + marker + takeLast(tailLength)
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
    return digest.digest().toHexString()
}
