/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.messages

interface CompilerMessageExtendedLocation {
    val location: CompilerMessageLocation
    val lineEnd: Int
    val columnEnd: Int

    companion object {
        @JvmStatic
        fun create(
            location: CompilerMessageLocation?,
            lineEnd: Int?,
            columnEnd: Int?,
        ): CompilerMessageExtendedLocation? =
            if (location == null) null else CompilerMessageExtendedLocationImpl(
                location,
                lineEnd ?: -1,
                columnEnd ?: -1,
            )
    }
}

val CompilerMessageExtendedLocation.path: String
    get() = location.path

val CompilerMessageExtendedLocation.line: Int
    get() = location.line

val CompilerMessageExtendedLocation.column: Int
    get() = location.column

val CompilerMessageExtendedLocation.lineContent: String?
    get() = location.lineContent


private data class CompilerMessageExtendedLocationImpl(
    override val location: CompilerMessageLocation,
    override val lineEnd: Int,
    override val columnEnd: Int,
) : CompilerMessageExtendedLocation {
    override fun toString(): String {
        val start =
            if (line == -1 && column == -1) ""
            else "$line:$column"
        val end =
            if (lineEnd == -1 && columnEnd == -1) ""
            else if (lineEnd == line) " - $columnEnd"
            else " - $lineEnd:$columnEnd"
        val loc = if (start.isEmpty() && end.isEmpty()) "" else " ($start$end)"
        return path + loc
    }
}
