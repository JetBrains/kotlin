/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.js.operations

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.CancellableBuildOperation
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JsCompilerLinkingArguments
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.js.TsExportConfiguration
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Performs linking of a klib file targeting the JS platform into the final executable.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Obtain an instance of this interface from [JsPlatformToolchain.jsLinkingOperationBuilder].
 *
 * @since 2.4.20
 */
@ExperimentalBuildToolsApi
public interface JsLinkingOperation : BaseCompilationOperation, CancellableBuildOperation<CompilationResult> {

    /**
     * The input klib file.
     */
    public val klib: Path

    /**
     * Where to put the output of the linkage.
     */
    public val destination: Path

    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> get(key: Option<V>): V

    @OptIn(ExperimentalCompilerArgument::class)
    public val compilerArguments: JsCompilerLinkingArguments

    /**
     * A builder for configuring and instantiating the [JsLinkingOperation].
     */
    public interface Builder : BaseCompilationOperation.Builder {
        /**
         * Kotlin compiler configurable options for JS linking.
         */
        @OptIn(ExperimentalCompilerArgument::class)
        public override val compilerArguments: JsCompilerLinkingArguments.Builder

        /**
         * The input klib file.
         */
        public val klib: Path

        /**
         * Where to put the output of the compilation.
         */
        public val destination: Path

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
         * Creates an immutable instance of [JsLinkingOperation] based on the configuration of this builder.
         */
        public override fun build(): JsLinkingOperation

        /**
         * **Design option D — fold TypeScript declaration (`.d.ts`) generation into linking via a
         * configuration object.** Creates a builder for a [TsExportConfiguration]; assigning the built
         * configuration to the [TS_EXPORT] option makes this linking operation also emit TypeScript
         * declarations into [dtsOutputDirectory], reusing the linking [compilerArguments] for
         * consistency. Unlike the flat [GENERATE_DTS] / [DTS_OUTPUT_DIRECTORY] options (option B), the
         * returned [TsExportConfiguration] is the extension point for future export settings.
         *
         * Mirrors the sub-builder pattern of `JvmCompilationOperation.Builder.snapshotBasedIcConfigurationBuilder`.
         *
         * @param dtsOutputDirectory the directory to write the generated `.d.ts` files into
         * @since 2.5.0
         */
        public fun tsExportBuilder(dtsOutputDirectory: Path): TsExportConfiguration.Builder
    }

    /**
     * Returns a [Builder] initialized with the values of this [JsLinkingOperation].
     */
    public fun toBuilder(): Builder

    /**
     * **Design option C.** Creates a builder for a standalone TypeScript declaration
     * (`.d.ts`) generation operation that inherits this linking operation's KLIBs and export-relevant
     * configuration (module kind, `Long`-as-`bigint`, per-module/per-file granularity, target, and
     * language-feature flags), so the generated declarations stay consistent with the produced JS
     * without any extra configuration.
     *
     * This mirrors the sub-builder pattern used elsewhere in the API (for example
     * `JvmCompilationOperation.Builder.snapshotBasedIcConfigurationBuilder`). It produces a
     * separate operation — which can run in parallel with this one — rather than a sub-configuration of
     * this operation.
     *
     * @param outputDirectory the directory to write the generated `.d.ts` files into
     * @since 2.5.0
     */
    public fun jsConsistentDtsGenerationOperationBuilder(outputDirectory: Path): JsConsistentDtsGenerationOperation.Builder

    /**
     * An option for configuring a [JsLinkingOperation].
     */
    public class Option<V> internal constructor(id: String, public val availableSinceVersion: KotlinReleaseVersion) : BaseOption<V>(id)

    public companion object {
        /**
         * **Design option B — fold TypeScript declaration (`.d.ts`) generation into JS linking.**
         *
         * When set to `true`, the linking operation also generates TypeScript declarations, reusing
         * this operation's [compilerArguments] (module kind, `-Xes-long-as-bigint`, per-module/per-file
         * granularity, …) as the single source of truth, so the declarations are guaranteed consistent
         * with the produced JS. This couples DTS generation to the linking lifecycle: it cannot run in
         * parallel with linking, and DTS cannot be regenerated without re-running linking.
         *
         * Defaults to `false`.
         */
        @JvmField
        public val GENERATE_DTS: Option<Boolean> = Option("GENERATE_DTS", KotlinReleaseVersion(2, 5, 0))

        /**
         * The directory the generated `.d.ts` files are written into when [GENERATE_DTS] is `true`.
         * When `null`, the linking [destination] is used.
         */
        @JvmField
        public val DTS_OUTPUT_DIRECTORY: Option<Path?> = Option("DTS_OUTPUT_DIRECTORY", KotlinReleaseVersion(2, 5, 0))

        /**
         * **Design option D.** When set to a non-`null` [TsExportConfiguration] (obtained via
         * [Builder.tsExportBuilder]), the linking operation also generates TypeScript declarations into
         * the configuration's [output directory][TsExportConfiguration.outputDirectory], reusing this
         * operation's [compilerArguments] for consistency. This is the structured, extensible
         * alternative to [GENERATE_DTS] / [DTS_OUTPUT_DIRECTORY] (option B).
         *
         * Defaults to `null` (no declaration generation).
         */
        @JvmField
        public val TS_EXPORT: Option<TsExportConfiguration?> = Option("TS_EXPORT", KotlinReleaseVersion(2, 5, 0))
    }
}

/**
 * Convenience function for creating a [JsConsistentDtsGenerationOperation] from this linking operation,
 * with optional overrides configured by [builderAction].
 *
 * @return an immutable `JsConsistentDtsGenerationOperation`.
 * @see JsLinkingOperation.jsConsistentDtsGenerationOperationBuilder
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalBuildToolsApi
public inline fun JsLinkingOperation.jsConsistentDtsGenerationOperation(
    outputDirectory: Path,
    builderAction: JsConsistentDtsGenerationOperation.Builder.() -> Unit = {},
): JsConsistentDtsGenerationOperation {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return jsConsistentDtsGenerationOperationBuilder(outputDirectory).apply(builderAction).build()
}

/**
 * Convenience function for creating a [TsExportConfiguration] (design option D) with options configured
 * by [builderAction]. Assign the result to the [JsLinkingOperation.TS_EXPORT] option to fold TypeScript
 * declaration generation into linking.
 *
 * @return an immutable `TsExportConfiguration`.
 * @see JsLinkingOperation.Builder.tsExportBuilder
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalBuildToolsApi
public inline fun JsLinkingOperation.Builder.tsExportConfiguration(
    dtsOutputDirectory: Path,
    builderAction: TsExportConfiguration.Builder.() -> Unit = {},
): TsExportConfiguration {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return tsExportBuilder(dtsOutputDirectory).apply(builderAction).build()
}
