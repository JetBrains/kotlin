/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.metadata

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.CancellableBuildOperation
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.MetadataArguments
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import java.nio.file.Path

/**
 * Compiles Kotlin code using specified options into a Kotlin Metadata klib.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Obtain an instance of this interface from [KotlinMetadataPlatformToolchain.metadataKlibCompilationOperationBuilder].
 *
 * @since 2.4.20
 */
@ExperimentalBuildToolsApi
public interface KotlinMetadataKlibCompilationOperation : BaseCompilationOperation, CancellableBuildOperation<CompilationResult> {

    /**
     * All sources of the compilation unit.
     */
    public val sources: List<Path>

    /**
     * The directory where the resulting klib will be placed.
     */
    public val destination: Path

    @OptIn(ExperimentalCompilerArgument::class)
    public val compilerArguments: MetadataArguments

    /**
     * A builder for configuring and instantiating the [KotlinMetadataKlibCompilationOperation].
     */
    public interface Builder : BaseCompilationOperation.Builder {
        /**
         * All sources of the compilation unit.
         */
        public val sources: List<Path>

        /**
         * Output klib file.
         */
        public val destination: Path

        /**
         * Kotlin compiler configurable options for klib-based compilation.
         */
        @OptIn(ExperimentalCompilerArgument::class)
        public override val compilerArguments: MetadataArguments.Builder

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
         * Creates an immutable instance of [KotlinMetadataKlibCompilationOperation] based on the configuration of this builder.
         */
        public override fun build(): KotlinMetadataKlibCompilationOperation
    }

    /**
     * Returns a [Builder] initialized with the values of this [KotlinMetadataKlibCompilationOperation].
     */
    public fun toBuilder(): Builder

    /**
     * An option for configuring a [KotlinMetadataKlibCompilationOperation].
     */
    public class Option<V> internal constructor(id: String) : BaseOption<V>(id)

    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> get(key: Option<V>): V

    public companion object
}
