/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm.operations

import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * The structured outcome of a [CompileReplSnippetOperation].
 *
 * A snippet compile either succeeds (producing a self-describing artifact) or fails. In both cases
 * the compiler may have emitted diagnostics — warnings on success, at least one error on failure.
 * This type carries the produced artifact and the diagnostics together so the operation never has
 * to signal a *compilation* failure by throwing: a [Failure] is a normal, expected outcome that the
 * caller pattern-matches on. Exceptions thrown by the operation are reserved for genuine
 * infrastructure/precondition errors (e.g. an unsupported execution policy or a malformed
 * prior-snippet artifact that cannot be decoded).
 *
 * @since 2.4.0
 */
@ExperimentalBuildToolsApi
public sealed interface ReplSnippetCompilationResult {
    /**
     * Diagnostics emitted while compiling the snippet, in compiler order.
     *
     * May be non-empty even for a [Success] (e.g. warnings). For a [Failure] it contains at least
     * one [CompilerMessageRenderer.Severity.ERROR] diagnostic.
     */
    public val diagnostics: List<ReplSnippetDiagnostic>

    /**
     * The snippet compiled successfully.
     *
     * @property artifact the encoded snippet artifact bytes (the same wire shape accepted as a prior
     *   snippet by a subsequent [CompileReplSnippetOperation] call), as produced by
     *   `SnippetArtifactCodec`.
     */
    public class Success(
        public val artifact: ByteArray,
        override val diagnostics: List<ReplSnippetDiagnostic>,
    ) : ReplSnippetCompilationResult

    /**
     * The snippet failed to compile; no artifact was produced.
     *
     * [diagnostics] contains at least one [CompilerMessageRenderer.Severity.ERROR] entry describing
     * why.
     */
    public class Failure(
        override val diagnostics: List<ReplSnippetDiagnostic>,
    ) : ReplSnippetCompilationResult
}

/**
 * A single structured diagnostic emitted while compiling a REPL snippet.
 *
 * @property severity the diagnostic severity.
 * @property message the human-readable diagnostic text.
 * @property location the source location the diagnostic points at, or `null` if not applicable.
 * @since 2.4.0
 */
@ExperimentalBuildToolsApi
public class ReplSnippetDiagnostic(
    public val severity: CompilerMessageRenderer.Severity,
    public val message: String,
    public val location: CompilerMessageRenderer.SourceLocation? = null,
)
