/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks.custom

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

fun printTimeStamp() {
    println("Timestamp: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}")
}

fun <T : Enum<T>> printTimeDiff(
    expectedLargeNanos: Long,
    expectedSmallNanos: Long,
    expectedLargeTimeCompileMode: Enum<T>,
    expectedSmallTimeCompileMode: Enum<T>,
    description: String? = null,
) {
    val diff = expectedLargeNanos - expectedSmallNanos
    val ratio = expectedLargeNanos.toDouble() / expectedSmallNanos

    println(
        buildString {
            append("${expectedLargeTimeCompileMode}/${expectedSmallTimeCompileMode} diff")
            if (description != null) {
                append(" ($description)")
            }
            append(": ${TimeUnit.NANOSECONDS.toMillis(diff)} ms (ratio: ${String.format(Locale.ENGLISH, "%.4f", ratio)})")
        }
    )

    assert(diff > 0) { "The number of generated files is too small to provide meaningful performance difference or the problem is already fixed." }

    println()
}