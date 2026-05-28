/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm.operations

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption

/**
 * Compiles a single K2 REPL snippet against an ordered list of prior-snippet artifacts, producing
 * a new self-describing artifact for that snippet.
 *
 * This op is the **transport seam** for the stateless K2 REPL compilation prototype
 * (see plugins/scripting/.ai/target/40-jsr223-target.md §"Stateless snippet compilation" and
 * plugins/scripting/.ai/target/90-open-questions.md Q5d).
 *
 * ### Wire shape
 *
 * Both prior artifacts and the produced artifact are transported as self-delimited byte arrays
 * encoded by `SnippetArtifactCodec` (scripting-compiler-internal). Each blob is a single JSON
 * document of shape:
 *
 * ```json
 * {
 *   "artifactVersion": 1,
 *   "sidecar":   "<base64 of sidecar JSON>",
 *   "classFiles": { "InternalName": "<base64 of class bytes>", ... }
 * }
 * ```
 *
 * The version is the envelope-layout version (separate from the sidecar's field-set version);
 * incompatible inputs are rejected with a clear diagnostic.
 *
 * ### Why a `List<ByteArray>` + `ByteArray` and not `List<Path>` / a typed result?
 *
 *  * The natural caller is an IDE consumer (IntelliJ today) that keeps prior artifacts in memory.
 *  * The byte-array shape forces the framing/envelope decision to live **inside** the artifact
 *    payload itself, which is exactly the design surface Q5d asks the prototype to expose before
 *    the protobuf-in-`.kotlin_metadata` cut. A typed surface here would hide that decision.
 *  * Path-based input remains a future option for build-system scenarios; it can be added as a
 *    separate op (or an Option on this one) without re-shaping the central artifact contract.
 *
 * ### Lifecycle
 *
 * Only `ExecutionPolicy.InProcess` is supported in this iteration. The impl creates and disposes
 * its own per-call IntelliJ-platform environment — callers are not required to manage one.
 * Daemon execution will be added in a later iteration (mirrors the `DiscoverScriptExtensionsOperation`
 * KT-84096 TODO).
 *
 * @since 2.4.0
 */
@ExperimentalBuildToolsApi
public interface CompileReplSnippetOperation : BuildOperation<ByteArray> {
    /**
     * Ordered list of prior-snippet artifacts (1..N-1), each encoded with `SnippetArtifactCodec`.
     * The order matters — the consumer of artifact k may reference declarations from any
     * artifact 1..k-1.
     */
    public val priorSnippets: List<ByteArray>

    /**
     * Source text of snippet N.
     */
    public val snippetSource: String

    /**
     * Human-readable name for snippet N (used in diagnostics + as the basis for the wrapper class
     * name). Typically `"snippet_N.repl.kts"` or similar.
     */
    public val snippetName: String

    /**
     * A builder for configuring and instantiating the [CompileReplSnippetOperation].
     *
     * @since 2.4.0
     */
    public interface Builder : BuildOperation.Builder {
        public val priorSnippets: List<ByteArray>
        public val snippetSource: String
        public val snippetName: String

        /**
         * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
         */
        public operator fun <V> get(key: Option<V>): V

        /**
         * Set the [value] for option specified by [key], overriding any previous value for that option.
         */
        public operator fun <V> set(key: Option<V>, value: V)

        /**
         * Creates an immutable instance of [CompileReplSnippetOperation] based on the configuration of this builder.
         */
        public fun build(): CompileReplSnippetOperation
    }

    /**
     * Creates a builder for [CompileReplSnippetOperation] that contains a copy of this configuration.
     */
    public fun toBuilder(): Builder

    /**
     * An option for configuring a [CompileReplSnippetOperation].
     *
     * @see get
     * @see set
     */
    public class Option<V> internal constructor(
        id: String,
        public val availableSinceVersion: KotlinReleaseVersion,
    ) : BaseOption<V>(id)

    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     */
    public operator fun <V> get(key: Option<V>): V

    public companion object {

        /**
         * Extra classpath entries to be added to snippet compilation (e.g. user libraries or
         * resolved dependencies). Mirrors `ScriptCompilationConfiguration.updateClasspath`.
         *
         * Defaults to an empty list — the snippet compiles against the stdlib only.
         *
         * @since 2.4.0
         */
        @JvmField
        public val ADDITIONAL_CLASSPATH: Option<List<java.nio.file.Path>> =
            Option("ADDITIONAL_CLASSPATH", KotlinReleaseVersion(2, 4, 0))

        /**
         * Transform compiler diagnostics into formatted strings for output.
         *
         * If no specific renderer is provided, the system defaults to a standard format:
         * file://<path>:<line>:<column> <message>
         *
         * @see CompilerMessageRenderer
         * @since 2.4.0
         */
        @JvmField
        public val COMPILER_MESSAGE_RENDERER: Option<CompilerMessageRenderer> =
            Option("COMPILER_MESSAGE_RENDERER", KotlinReleaseVersion(2, 4, 0))
    }
}
