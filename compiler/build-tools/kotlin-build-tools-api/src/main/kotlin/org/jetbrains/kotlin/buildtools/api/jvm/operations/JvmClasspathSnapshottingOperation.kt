/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.api.jvm.operations

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathEntrySnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation.Companion.GRANULARITY

/**
 * Calculates a JVM classpath snapshot used for detecting changes in incremental compilation with specified [GRANULARITY].
 *
 * The [ClassSnapshotGranularity.CLASS_LEVEL] granularity should be preferred
 * for rarely changing dependencies as more lightweight in terms of the resulting snapshot size.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Obtain an instance of this interface from [JvmPlatformToolchain.createClasspathSnapshottingOperation].
 *
 * An example of the basic usage is:
 *  ```
 *   val toolchain = KotlinToolchains.loadImplementation(ClassLoader.getSystemClassLoader())
 *   val operation = toolchain.jvm.createClasspathSnapshottingOperation(classesDir)
 *   operation[GRANULARITY] = ClassSnapshotGranularity.CLASS_LEVEL
 *   toolchain.createBuildSession().use { it.executeOperation(operation) }
 *  ```
 *
 * @since 2.3.0
 */
@ExperimentalBuildToolsApi
public interface JvmClasspathSnapshottingOperation : BuildOperation<ClasspathEntrySnapshot> {

    public interface Builder : BuildOperation.Builder {
        public operator fun <V> get(key: Option<V>): V
        public operator fun <V> set(key: Option<V>, value: V)
        public fun build(): JvmClasspathSnapshottingOperation
    }

    public fun toBuilder(): Builder

    /**
     * Base class for [JvmClasspathSnapshottingOperation] options.
     *
     * @see get
     * @see set
     */
    public class Option<out V> internal constructor(id: String) : BaseOption<V>(id)

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
    @Deprecated("Use JvmClasspathSnapshottingOperation.Builder.set instead")
    public operator fun <V> set(key: Option<V>, value: V)

    public companion object {

        /**
         * Determines granularity of tracking.
         */
        @JvmField
        public val GRANULARITY: Option<ClassSnapshotGranularity> = Option("GRANULARITY")

        /**
         * Enables extended snapshotting mode for inline methods and accessors.
         */
        @JvmField
        public val PARSE_INLINED_LOCAL_CLASSES: Option<Boolean> = Option("PARSE_INLINED_LOCAL_CLASSES")
    }
}