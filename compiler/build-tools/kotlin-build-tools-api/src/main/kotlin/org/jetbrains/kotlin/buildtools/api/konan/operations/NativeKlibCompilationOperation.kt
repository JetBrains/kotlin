/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.konan.operations

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.CancellableBuildOperation
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.konan.NativePlatformToolchain
import java.nio.file.Path

/**
 * Compiles Kotlin code targeting the Kotlin/Native platform and using specified options into a klib.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Obtain an instance of this interface from [NativePlatformToolchain.nativeKlibCompilationOperationBuilder].
 *
 * @since 2.5.20
 */
@ExperimentalBuildToolsApi
public interface NativeKlibCompilationOperation : BaseCompilationOperation, CancellableBuildOperation<CompilationResult> {

    /**
     * All sources of the compilation unit.
     */
    public val sources: List<Path>

    /**
     * The klib file or directory where the compilation result will be placed.
     */
    public val destination: Path

    /**
     * Kotlin compiler configurable options.
     */
    public val compilerArguments: CommonCompilerArguments

    /**
     * A builder for configuring and instantiating the [NativeKlibCompilationOperation].
     */
    public interface Builder : BaseCompilationOperation.Builder {
        /**
         * All sources of the compilation unit.
         */
        public val sources: List<Path>

        /**
         * Output klib file or directory.
         */
        public val destination: Path

        /**
         * Kotlin compiler configurable options.
         */
        public override val compilerArguments: CommonCompilerArguments.Builder

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
         * Creates an immutable instance of [NativeKlibCompilationOperation] based on the configuration of this builder.
         */
        public override fun build(): NativeKlibCompilationOperation
    }

    /**
     * Returns a [Builder] initialized with the values of this [NativeKlibCompilationOperation].
     */
    public fun toBuilder(): Builder

    /**
     * An option for configuring a [NativeKlibCompilationOperation].
     */
    public class Option<V> internal constructor(id: String, public val availableSinceVersion: KotlinReleaseVersion) : BaseOption<V>(id)

    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> get(key: Option<V>): V

    public companion object
}
