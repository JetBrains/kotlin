/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.messages

import java.io.Serializable

interface CompilerMessageSourceLocation : Serializable {
    val path: String
    val line: Int
    val column: Int
    // NOTE: Seems that the end-of-location data do not belong here conceptually, and now causes confusion with other usages
    // TODO: consider removing it and switching REPL/Scripting diagnostis to the higher-level entities (KtDiagnostics)
    val lineEnd: Int get() = -1
    val columnEnd: Int get() = -1
    val lineContent: String? // related to the (start) line/column only, used to show start position in the console output
}

data class CompilerMessageLocation private constructor(
    override val path: String,
    override val line: Int,
    override val column: Int,
    override val lineContent: String?
) : CompilerMessageSourceLocation {
    override fun toString(): String =
        path + (if (line != -1 || column != -1) " ($line:$column)" else "")

    companion object {
        @JvmStatic
        fun create(path: String?): CompilerMessageLocation? =
            create(path, -1, -1, null)

        @JvmStatic
        fun create(path: String?, line: Int, column: Int, lineContent: String?): CompilerMessageLocation? =
            if (path == null) null else CompilerMessageLocation(path, line, column, lineContent)

        private val serialVersionUID: Long = 8228357578L
    }
}

data class CompilerMessageLocationWithRange private constructor(
    override val path: String,
    override val line: Int,
    override val column: Int,
    override val lineEnd: Int,
    override val columnEnd: Int,
    override val lineContent: String?
) : CompilerMessageSourceLocation {
    override fun toString(): String =
        path + (if (line != -1 || column != -1) " ($line:$column)" else "")

    companion object {
        @JvmStatic
        fun create(
            path: String?,
            lineStart: Int,
            columnStart: Int,
            lineEnd: Int?,
            columnEnd: Int?,
            lineContent: String?
        ): CompilerMessageLocationWithRange? =
            if (path == null) null else CompilerMessageLocationWithRange(path, lineStart, columnStart, lineEnd ?: -1, columnEnd ?: -1, lineContent)

        private val serialVersionUID: Long = 8228357578L
    }
}

