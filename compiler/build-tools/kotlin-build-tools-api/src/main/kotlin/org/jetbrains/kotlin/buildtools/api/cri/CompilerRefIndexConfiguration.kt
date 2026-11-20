/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.cri

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import java.nio.file.Path

/**
 * Configuration of the compiler reference index (CRI) that a compilation writes.
 *
 * The compilation appends the index to the files in [outputDirectory], with the names from [CriToolchain], so each compilation
 * needs a directory of its own. With [org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration],
 * a rebuild replaces the files; otherwise, clear the directory before a rebuild.
 * Only JVM compilations with an incremental compilation configuration write the index for now.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * @property outputDirectory the directory the index files are written to.
 *
 * @see org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation.COMPILER_REF_INDEX
 * @see org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation.Builder.compilerRefIndexConfigurationBuilder
 * @since 2.5.20
 */
@ExperimentalBuildToolsApi
public interface CompilerRefIndexConfiguration {

    public val outputDirectory: Path

    /**
     * A builder for [CompilerRefIndexConfiguration].
     *
     * @since 2.5.20
     */
    public interface Builder {
        /**
         * The directory the index files are written to.
         *
         * @since 2.5.20
         */
        public val outputDirectory: Path

        /**
         * Returns the value of the option [key]: the value it was [set] to, or else its default.
         *
         * @throws IllegalStateException if the option was not set and has no default
         * @since 2.5.20
         */
        public operator fun <V> get(key: Option<V>): V

        /**
         * Sets the option [key] to [value], replacing any earlier value.
         *
         * @since 2.5.20
         */
        public operator fun <V> set(key: Option<V>, value: V)

        /**
         * Creates an immutable [CompilerRefIndexConfiguration] from the settings of this builder.
         *
         * @since 2.5.20
         */
        public fun build(): CompilerRefIndexConfiguration
    }

    /**
     * Creates a builder that starts from a copy of this configuration.
     *
     * @since 2.5.20
     */
    public fun toBuilder(): Builder

    /**
     * An option of a [CompilerRefIndexConfiguration].
     *
     * @see get
     * @see Builder.set
     */
    public class Option<V> internal constructor(id: String, public val availableSinceVersion: KotlinReleaseVersion) : BaseOption<V>(id)

    /**
     * Returns the value of the option [key]: the value it was [set][Builder.set] to, or else its default.
     *
     * @throws IllegalStateException if the option was not set and has no default
     * @since 2.5.20
     */
    public operator fun <V> get(key: Option<V>): V
}
