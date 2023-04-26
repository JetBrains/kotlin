/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.statistics

internal fun formatTime(ms: Long): String {
    val seconds = ms.toDouble() / 1_000
    return seconds.asString(2) + " s"
}

private const val kbSize = 1024
private const val mbSize = kbSize * 1024
private const val gbSize = mbSize * 1024

fun formatSize(sizeInBytes: Long): String = when {
    sizeInBytes / gbSize >= 1 -> "${(sizeInBytes.toDouble() / gbSize).asString(1)} GB"
    sizeInBytes / mbSize >= 1 -> "${(sizeInBytes.toDouble() / mbSize).asString(1)} MB"
    sizeInBytes / kbSize >= 1 -> "${(sizeInBytes.toDouble() / kbSize).asString(1)} KB"
    else -> "$sizeInBytes B"
}

internal fun Double.asString(decPoints: Int): String = "%,.${decPoints}f".format(this)