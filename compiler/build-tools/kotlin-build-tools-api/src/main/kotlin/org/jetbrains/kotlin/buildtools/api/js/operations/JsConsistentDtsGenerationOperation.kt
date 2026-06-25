/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.js.operations

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import java.nio.file.Path

/**
 * **Design option C — standalone operation that inherits its configuration from linking.**
 *
 * Generates TypeScript declarations (`.d.ts`) into [outputDirectory], independently of (and potentially
 * in parallel with) JS linking — like [JsDtsGenerationOperation] — but obtained from a
 * [JsLinkingOperation] (see [JsLinkingOperation.jsConsistentDtsGenerationOperationBuilder]). It inherits
 * the KLIBs and the export-relevant settings (module kind, `Long`-as-`bigint`, per-module/per-file
 * granularity, target, and language-feature flags) from that operation.
 *
 * By design this operation exposes **no** export-configuration options of its own: consistency with the
 * produced JS is guaranteed precisely because every relevant setting comes from [linking] and there is
 * nothing here that could diverge from it.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Obtain an instance of this interface from [JsLinkingOperation.jsConsistentDtsGenerationOperationBuilder].
 *
 * @since 2.5.0
 */
@ExperimentalBuildToolsApi
public interface JsConsistentDtsGenerationOperation : BuildOperation<CompilationResult> {

    /**
     * The JS linking operation this operation inherits its KLIBs and configuration from.
     */
    public val linking: JsLinkingOperation

    /**
     * The directory the generated `.d.ts` files are written into.
     */
    public val outputDirectory: Path

    /**
     * A builder for instantiating the [JsConsistentDtsGenerationOperation].
     */
    public interface Builder : BuildOperation.Builder {
        /**
         * The JS linking operation this operation inherits its KLIBs and configuration from.
         */
        public val linking: JsLinkingOperation

        /**
         * The directory the generated `.d.ts` files are written into.
         */
        public val outputDirectory: Path

        /**
         * Creates an immutable instance of [JsConsistentDtsGenerationOperation] based on the configuration of this builder.
         */
        public fun build(): JsConsistentDtsGenerationOperation
    }

    /**
     * Returns a [Builder] initialized with the values of this [JsConsistentDtsGenerationOperation].
     */
    public fun toBuilder(): Builder
}
