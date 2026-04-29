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
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain
import java.nio.file.Path

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
    public val compilerArguments: JsArguments

    /**
     * A builder for configuring and instantiating the [JsLinkingOperation].
     */
    public interface Builder : BaseCompilationOperation.Builder {
        /**
         * Kotlin compiler configurable options for JS linking.
         */
        @OptIn(ExperimentalCompilerArgument::class)
        public override val compilerArguments: JsArguments.Builder

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
    }

    /**
     * Returns a [Builder] initialized with the values of this [JsLinkingOperation].
     */
    public fun toBuilder(): Builder

    /**
     * An option for configuring a [JsLinkingOperation].
     */
    public class Option<V> internal constructor(id: String, public val availableSinceVersion: KotlinReleaseVersion) : BaseOption<V>(id)
}
