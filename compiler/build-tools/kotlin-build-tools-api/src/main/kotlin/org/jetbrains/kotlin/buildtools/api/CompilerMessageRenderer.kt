/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

/**
 * A renderer for formatting compiler diagnostic messages.
 */
public interface CompilerMessageRenderer {

    /**
     * Renders a compiler message.
     *
     * @param severity the severity level of the message
     * @param message the message text
     * @param location the source location, or `null` if not applicable
     * @return the formatted message or `null`
     */
    public fun render(severity: Severity, message: String, location: SourceLocation?): String?

    /**
     * The severity level of a compiler message.
     */
    public enum class Severity {

        EXCEPTION,
        ERROR,

        /**
         * A warning that is not discarded when compilation errors occur.
         * Used for configuration-related problems rather than code diagnostics.
         */
        STRONG_WARNING,

        /**
         * A warning set by `-Xwarning-level=NAME:warning`.
         * Not suppressed by `-nowarn` and does not cause compilation failure with `-Werror`.
         */
        FIXED_WARNING,

        WARNING,
        INFO,
        LOGGING,

        /**
         * Source-to-output file mapping (e.g., `A.kt -> A.class`). Used for incremental compilation.
         */
        OUTPUT;
    }

    /**
     * A location in source code associated with a compiler message.
     *
     * @property path the file path
     * @property line the 1-based start line number
     * @property column the 1-based start column number
     * @property lineEnd the 1-based end line number, or `-1` if not applicable
     * @property columnEnd the 1-based end column number, or `-1` if not applicable
     * @property lineContent the content of the start line, or `null` if not available
     */
    public data class SourceLocation(
        public val path: String,
        public val line: Int,
        public val column: Int,
        public val lineEnd: Int,
        public val columnEnd: Int,
        public val lineContent: String?,
    )
}