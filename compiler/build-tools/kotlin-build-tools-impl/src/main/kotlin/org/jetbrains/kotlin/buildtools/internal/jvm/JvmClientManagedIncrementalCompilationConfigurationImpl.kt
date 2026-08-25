/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jvm

import org.jetbrains.kotlin.buildtools.api.jvm.*
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.internal.*

internal class JvmClientManagedIncrementalCompilationConfigurationImpl private constructor(
    override val incrementalCompilationComponents: CompilerIncrementalCompilationComponents,
    private val options: Options,
) : JvmClientManagedIncrementalCompilationConfiguration,
    JvmClientManagedIncrementalCompilationConfiguration.Builder,
    DeepCopyable<JvmClientManagedIncrementalCompilationConfigurationImpl> {

    constructor(incrementalCompilationComponents: CompilerIncrementalCompilationComponents) : this(
        incrementalCompilationComponents,
        Options(JvmClientManagedIncrementalCompilationConfiguration::class),
    ) {
        initializeOptions(this::class, options)
    }

    override fun build(): JvmClientManagedIncrementalCompilationConfiguration = deepCopy()

    override fun toBuilder(): JvmClientManagedIncrementalCompilationConfiguration.Builder = deepCopy()

    override fun deepCopy(): JvmClientManagedIncrementalCompilationConfigurationImpl =
        JvmClientManagedIncrementalCompilationConfigurationImpl(incrementalCompilationComponents, options.deepCopy())

    @UseFromImplModuleRestricted
    override fun <V> get(key: JvmClientManagedIncrementalCompilationConfiguration.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: JvmClientManagedIncrementalCompilationConfiguration.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        options[key] = value
    }

    operator fun <V> get(key: Option<V>): V = options[key]

    class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

    companion object {
        val LOOKUP_TRACKER: Option<CompilerLookupTracker?> = Option("LOOKUP_TRACKER", null)

        val FILE_MAPPING_TRACKER: Option<CompilerFileMappingTracker?> = Option("FILE_MAPPING_TRACKER", null)

        val EXPECT_ACTUAL_TRACKER: Option<CompilerExpectActualTracker?> = Option("EXPECT_ACTUAL_TRACKER", null)

        val ENUM_WHEN_TRACKER: Option<CompilerEnumWhenTracker?> = Option("ENUM_WHEN_TRACKER", null)

        val IMPORT_TRACKER: Option<CompilerImportTracker?> = Option("IMPORT_TRACKER", null)

        val INLINE_CONST_TRACKER: Option<CompilerInlineConstTracker?> = Option("INLINE_CONST_TRACKER", null)
    }
}
