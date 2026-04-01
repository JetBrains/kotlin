/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.abi.operations

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.abi.AbiFilters
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.abi.AbiValidationToolchain
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Prints an ABI dump for JVM from [Builder.inputFiles] into the provided via [AbiValidationToolchain.dumpJvmAbiToStringOperationBuilder] appendable.
 * It is possible to pass class-files or jar files in [Builder.inputFiles].
 *
 * To control which declarations are passed to the dump, the option [DumpJvmAbiToStringOperation.PATTERN_FILTERS] could be used. By default, no filters will be applied.
 *
 * @since 2.4.0
 */
@ExperimentalBuildToolsApi
public interface DumpJvmAbiToStringOperation : BuildOperation<Unit> {
    public val inputFiles: Iterable<Path>

    /**
     * A builder for [DumpJvmAbiToStringOperation].
     * Generates immutable instances of [DumpJvmAbiToStringOperation] based on the configuration of this builder.
     *
     * @since 2.4.0
     */
    public interface Builder : BuildOperation.Builder {
        public val inputFiles: Iterable<Path>

        /**
         * Creates a builder for configuring ABI filters.
         *
         * @since 2.4.0
         */
        public fun filtersBuilder(): AbiFilters.Builder

        /**
         * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
         *
         * @return the previously set value for an option
         * @throws IllegalStateException if the option was not set and has no default value
         *
         * @since 2.4.0
         */
        public operator fun <V> get(key: Option<V>): V

        /**
         * Set the [value] for option specified by [key], overriding any previous value for that option.
         *
         * @since 2.4.0
         */
        public operator fun <V> set(key: Option<V>, value: V)

        /**
         * Creates an immutable instance of [DumpJvmAbiToStringOperation] based on the configuration of this builder.
         *
         * @since 2.4.0
         */
        public fun build(): DumpJvmAbiToStringOperation
    }

    public companion object {
        /**
         * Set of filtering rules that restrict ABI declarations included in a dump.
         * See [AbiFilters] for details.
         *
         * @since 2.4.0
         */
        @JvmField
        public val PATTERN_FILTERS: Option<AbiFilters?> = Option("PATTERN_FILTERS")
    }

    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     *
     * @since 2.4.0
     */
    public operator fun <V> get(key: Option<V>): V

    /**
     * An option for configuring a [DumpJvmAbiToStringOperation].
     *
     * @see get
     * @see DumpJvmAbiToStringOperation.Companion
     *
     * @since 2.4.0
     */
    public class Option<V> internal constructor(id: String) : BaseOption<V>(id)
}

/**
 * Creates a builder for configuring ABI filters.
 *
 * @since 2.4.0
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalBuildToolsApi
public fun DumpJvmAbiToStringOperation.Builder.filters(
    builderAction: AbiFilters.Builder.() -> Unit = {},
): AbiFilters {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return filtersBuilder().apply(builderAction).build()
}
