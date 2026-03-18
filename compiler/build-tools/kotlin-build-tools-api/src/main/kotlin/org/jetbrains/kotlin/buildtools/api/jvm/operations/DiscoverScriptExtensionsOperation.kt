/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm.operations

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import java.nio.file.Path

/**
 * Discovers custom script extensions that exist in a given classpath.
 *
 * @since 2.4.0
 */
@ExperimentalBuildToolsApi
public interface DiscoverScriptExtensionsOperation : BuildOperation<Collection<String>> {
    /**
     * The classpath to search for custom script extensions.
     */
    public val classpath: List<Path>

    /**
     * A builder for configuring and instantiating the [DiscoverScriptExtensionsOperation].
     *
     * @since 2.4.0
     */
    public interface Builder : BuildOperation.Builder {
        /**
         * The classpath to search for custom script extensions.
         */
        public val classpath: List<Path>

        /**
         * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
         *
         * @return the previously set value for an option
         * @throws IllegalStateException if the option was not set and has no default value
         *
         */
        public operator fun <V> get(key: Option<V>): V

        /**
         * Set the [value] for option specified by [key], overriding any previous value for that option.
         *
         */
        public operator fun <V> set(key: Option<V>, value: V)

        /**
         * Creates an immutable instance of [DiscoverScriptExtensionsOperation] based on the configuration of this builder.
         *
         */
        public fun build(): DiscoverScriptExtensionsOperation
    }

    /**
     * Creates a builder for [DiscoverScriptExtensionsOperation] that contains a copy of this configuration.
     *
     */
    public fun toBuilder(): DiscoverScriptExtensionsOperation.Builder

    /**
     * An option for configuring a [DiscoverScriptExtensionsOperation].
     *
     * @see get
     * @see set
     */
    public class Option<V> internal constructor(id: String) : BaseOption<V>(id)

    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> get(key: Option<V>): V

    public companion object {

        /**
         * Transform compiler diagnostics into formatted strings for output.
         *
         * If no specific renderer is provided, the system defaults to a standard format:
         * file://<path>:<line>:<column> <message>
         *
         * Example Output:
         * file:///path/to/File.kt:10:5 Unresolved reference: foo
         *
         * @see CompilerMessageRenderer
         */
        @JvmField
        public val COMPILER_MESSAGE_RENDERER: Option<CompilerMessageRenderer> = Option("COMPILER_MESSAGE_RENDERER")
    }
}