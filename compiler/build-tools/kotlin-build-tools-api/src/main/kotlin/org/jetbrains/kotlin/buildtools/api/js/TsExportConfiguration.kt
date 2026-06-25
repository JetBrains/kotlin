/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.js

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.js.operations.JsLinkingOperation
import java.nio.file.Path

/**
 * **Design option D — configuration for TypeScript declaration (`.d.ts`) generation folded into JS linking.**
 *
 * Attaching this configuration to a [JsLinkingOperation] (via the [JsLinkingOperation.TS_EXPORT] option,
 * obtained through [JsLinkingOperation.Builder.tsExportBuilder]) makes the linking operation also emit
 * TypeScript declarations, reusing the linking [compiler arguments][JsLinkingOperation.compilerArguments]
 * for configuration consistency. It is the structured, extensible alternative to the flat
 * [JsLinkingOperation.GENERATE_DTS] / [JsLinkingOperation.DTS_OUTPUT_DIRECTORY] options (option B):
 * it currently carries only the mandatory [outputDirectory], but its [Option]s are the extension point
 * for future TypeScript export settings (for example declaration filters or per-package selection).
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Obtain a builder from [JsLinkingOperation.Builder.tsExportBuilder].
 *
 * @since 2.5.0
 */
@ExperimentalBuildToolsApi
public interface TsExportConfiguration {

    /**
     * The directory the generated `.d.ts` files are written into.
     */
    public val outputDirectory: Path

    /**
     * Get the value for option specified by [key] if it was previously [Builder.set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> get(key: Option<V>): V

    /**
     * A builder for configuring and instantiating the [TsExportConfiguration].
     */
    public interface Builder {
        /**
         * The directory the generated `.d.ts` files are written into.
         */
        public val outputDirectory: Path

        /**
         * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
         *
         * @return the previously set value for an option
         * @throws IllegalStateException if the option was not set and has no default value
         */
        public operator fun <V> get(key: Option<V>): V

        /**
         * Set the [value] for option specified by [key], overriding any previous value for that option.
         */
        public operator fun <V> set(key: Option<V>, value: V)

        /**
         * Creates an immutable instance of [TsExportConfiguration] based on the configuration of this builder.
         */
        public fun build(): TsExportConfiguration
    }

    /**
     * An option for configuring a [TsExportConfiguration].
     */
    public class Option<V> internal constructor(id: String, public val availableSinceVersion: KotlinReleaseVersion) : BaseOption<V>(id)

    public companion object {
        // No options yet. This companion is the extension point for future TypeScript export
        // configuration (for example declaration filters or per-package selection).
    }
}
