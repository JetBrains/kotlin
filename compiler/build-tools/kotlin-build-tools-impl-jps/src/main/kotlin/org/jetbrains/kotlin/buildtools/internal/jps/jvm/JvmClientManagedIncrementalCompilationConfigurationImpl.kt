/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jps.jvm

import org.jetbrains.kotlin.buildtools.api.jps.jvm.JvmClientManagedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.CompilerIncrementalCompilationComponents
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.CompilerEnumWhenTracker
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.CompilerExpectActualTracker
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.CompilerFileMappingTracker
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.CompilerImportTracker
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.CompilerInlineConstTracker
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.internal.jps.Option
import org.jetbrains.kotlin.buildtools.internal.jps.Options

internal class JvmClientManagedIncrementalCompilationConfigurationImpl private constructor(
    override val incrementalCompilationComponents: CompilerIncrementalCompilationComponents,
    private val options: Options,
) : JvmClientManagedIncrementalCompilationConfiguration,
    JvmClientManagedIncrementalCompilationConfiguration.Builder {

    constructor(incrementalCompilationComponents: CompilerIncrementalCompilationComponents) : this(
        incrementalCompilationComponents,
        Options(JvmClientManagedIncrementalCompilationConfiguration::class.java.name, defaults),
    )

    override fun build(): JvmClientManagedIncrementalCompilationConfiguration = copy()

    override fun toBuilder(): JvmClientManagedIncrementalCompilationConfiguration.Builder = copy()

    private fun copy(): JvmClientManagedIncrementalCompilationConfigurationImpl =
        JvmClientManagedIncrementalCompilationConfigurationImpl(incrementalCompilationComponents, options.copy())

    override fun <V> get(key: JvmClientManagedIncrementalCompilationConfiguration.Option<V>): V = options[key]

    override fun <V> set(key: JvmClientManagedIncrementalCompilationConfiguration.Option<V>, value: V) {
        options[key] = value
    }

    operator fun <V> get(key: Option<V>): V = options[key]

    companion object {
        val LOOKUP_TRACKER: Option<CompilerLookupTracker?> = Option("LOOKUP_TRACKER", null)

        val FILE_MAPPING_TRACKER: Option<CompilerFileMappingTracker?> = Option("FILE_MAPPING_TRACKER", null)

        val EXPECT_ACTUAL_TRACKER: Option<CompilerExpectActualTracker?> = Option("EXPECT_ACTUAL_TRACKER", null)

        val ENUM_WHEN_TRACKER: Option<CompilerEnumWhenTracker?> = Option("ENUM_WHEN_TRACKER", null)

        val IMPORT_TRACKER: Option<CompilerImportTracker?> = Option("IMPORT_TRACKER", null)

        val INLINE_CONST_TRACKER: Option<CompilerInlineConstTracker?> = Option("INLINE_CONST_TRACKER", null)

        private val defaults: List<Option<*>> = listOf(
            LOOKUP_TRACKER,
            FILE_MAPPING_TRACKER,
            EXPECT_ACTUAL_TRACKER,
            ENUM_WHEN_TRACKER,
            IMPORT_TRACKER,
            INLINE_CONST_TRACKER,
        )
    }
}
