/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.abi.operations

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.abi.AbiFilters
import org.jetbrains.kotlin.buildtools.api.abi.AbiValidationToolchain
import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetId
import org.jetbrains.kotlin.buildtools.api.abi.operations.DumpKlibAbiToStringOperation.Companion.REFERENCE_DUMP_FILE
import org.jetbrains.kotlin.buildtools.api.abi.operations.DumpKlibAbiToStringOperation.Companion.TARGETS_TO_INFER
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * Prints an ABI dump for klib targets from [Builder.klibs] into the provided via [AbiValidationToolchain.dumpKlibAbiToStringOperationBuilder] appendable.
 * Compressed and unpacked klibs are supported.
 *
 * If option [TARGETS_TO_INFER] is specified and not empty, for the specified targets the ABI will be inferred from the value of the option [REFERENCE_DUMP_FILE].
 * The inference works as follows:
 * - for each target from [TARGETS_TO_INFER], the ABI is inferred from the [REFERENCE_DUMP_FILE], if it exists, not empty, and this target is present in it.
 * - all the non-inferred targets that belong to the group that this target belongs to are found. Then all declarations are added that are present in all of them.
 * - if some target specified in [TARGETS_TO_INFER] and present in the [klibs], then the error is thrown.
 *
 * The inference is used in cases where the host compiler cannot compile some targets, but there is a need to build an ABI dump,
 * even if with some inaccuracies.
 *
 * To control which declarations are passed to the dump, the option [DumpKlibAbiToStringOperation.PATTERN_FILTERS] could be used. By default, no filters will be applied.
 *
 * @since 2.4.0
 */
@ExperimentalBuildToolsApi
public interface DumpKlibAbiToStringOperation : BuildOperation<Unit> {
    public val klibs: Map<KlibTargetId, Path>

    /**
     * A builder for [DumpKlibAbiToStringOperation].
     * Generates immutable instances of [DumpKlibAbiToStringOperation] based on the configuration of this builder.
     *
     * @since 2.4.0
     */
    public interface Builder : BuildOperation.Builder {
        public val klibs: Map<KlibTargetId, Path>

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
         * Creates an immutable instance of [DumpKlibAbiToStringOperation] based on the configuration of this builder.
         *
         * @since 2.4.0
         */
        public fun build(): DumpKlibAbiToStringOperation
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

        /**
         * Set of targets for which the ABI will be inferred from the [REFERENCE_DUMP_FILE].
         *
         * Refer to the documentation of [DumpKlibAbiToStringOperation] for more details about the inference process.
         *
         * @since 2.4.0
         */
        @JvmField
        public val TARGETS_TO_INFER: Option<Set<KlibTargetId>> = Option("TARGETS_TO_INFER")

        /**
         * Path to the file containing the reference ABI dump for the previous ABI.
         * Should be used only if [TARGETS_TO_INFER] is specified and not empty.
         *
         * Refer to the documentation of [DumpKlibAbiToStringOperation] for more details about the inference process.
         *
         * @since 2.4.0
         */
        @JvmField
        public val REFERENCE_DUMP_FILE: Option<Path?> = Option("REFERENCE_DUMP_FILE")
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
     * An option for configuring a [DumpKlibAbiToStringOperation].
     *
     * @see get
     * @see DumpKlibAbiToStringOperation.Companion
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
public fun DumpKlibAbiToStringOperation.Builder.filters(
    builderAction: AbiFilters.Builder.() -> Unit = {},
): AbiFilters {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return filtersBuilder().apply(builderAction).build()
}
