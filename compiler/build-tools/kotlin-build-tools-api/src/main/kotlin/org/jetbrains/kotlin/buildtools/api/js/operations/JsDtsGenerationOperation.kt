/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.js.operations

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsModuleKind
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.js.JsDtsCompilationStrategy
import org.jetbrains.kotlin.buildtools.api.js.JsDtsGranularity
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain
import java.nio.file.Path

/**
 * **Design option A — standalone operation with explicit options.**
 *
 * Generates TypeScript declarations (`.d.ts`) from a set of compiled KLIBs into [outputDirectory],
 * independently of (and potentially in parallel with) JS linking. All export settings are supplied
 * explicitly via the typed [Option]s below; it is the caller's responsibility to keep them consistent
 * with the JS linking configuration (see [JsConsistentDtsGenerationOperation] for an alternative that
 * inherits the configuration from linking automatically).
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Obtain an instance of this interface from [JsPlatformToolchain.jsDtsGenerationOperationBuilder].
 *
 * @since 2.5.0
 */
@ExperimentalBuildToolsApi
public interface JsDtsGenerationOperation : BuildOperation<CompilationResult> {

    /**
     * The KLIBs to generate declarations from: the main KLIB and all of its dependencies
     * (regular, friend, and runtime). Per-module output names are derived from the KLIB manifests.
     */
    public val klibs: List<Path>

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
     * A builder for configuring and instantiating the [JsDtsGenerationOperation].
     */
    public interface Builder : BuildOperation.Builder {
        /**
         * The KLIBs to generate declarations from.
         */
        public val klibs: List<Path>

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
         * Creates an immutable instance of [JsDtsGenerationOperation] based on the configuration of this builder.
         */
        public fun build(): JsDtsGenerationOperation
    }

    /**
     * Returns a [Builder] initialized with the values of this [JsDtsGenerationOperation].
     */
    public fun toBuilder(): Builder

    /**
     * An option for configuring a [JsDtsGenerationOperation].
     */
    public class Option<V> internal constructor(id: String, public val availableSinceVersion: KotlinReleaseVersion) : BaseOption<V>(id)

    public companion object {
        /**
         * The name of the produced module.
         */
        @JvmField
        public val MODULE_NAME: Option<String?> = Option("MODULE_NAME", KotlinReleaseVersion(2, 5, 0))

        /**
         * The kind of JS module the declarations target. Must match the JS linking `MODULE_KIND`.
         */
        @JvmField
        public val MODULE_KIND: Option<JsModuleKind?> = Option("MODULE_KIND", KotlinReleaseVersion(2, 5, 0))

        /**
         * The granularity of the generated declarations. Must match the JS linking granularity.
         */
        @JvmField
        public val GRANULARITY: Option<JsDtsGranularity?> = Option("GRANULARITY", KotlinReleaseVersion(2, 5, 0))

        /**
         * How the generated declarations are laid out across files (merged vs. per file). Must be kept
         * consistent with how the JS output is emitted. Defaults to [JsDtsCompilationStrategy.MERGED].
         */
        @JvmField
        public val TS_COMPILATION_STRATEGY: Option<JsDtsCompilationStrategy?> = Option("TS_COMPILATION_STRATEGY", KotlinReleaseVersion(2, 5, 0))

        /**
         * Whether `Long` is compiled as ES2020 `bigint`. Must match the JS linking `-Xes-long-as-bigint`.
         */
        @JvmField
        public val COMPILE_LONG_AS_BIG_INT: Option<Boolean> = Option("COMPILE_LONG_AS_BIG_INT", KotlinReleaseVersion(2, 5, 0))

        /**
         * Whether exported interfaces are emitted in an implementable way
         * (corresponds to the `JsExportInterfacesInImplementableWay` language feature).
         */
        @JvmField
        public val IMPLEMENT_INTERFACES: Option<Boolean> = Option("IMPLEMENT_INTERFACES", KotlinReleaseVersion(2, 5, 0))

        /**
         * Whether suspend lambdas are exportable
         * (corresponds to the `JsExportingSuspendLambdas` language feature).
         */
        @JvmField
        public val EXPORT_SUSPEND_LAMBDAS: Option<Boolean> = Option("EXPORT_SUSPEND_LAMBDAS", KotlinReleaseVersion(2, 5, 0))
    }
}
