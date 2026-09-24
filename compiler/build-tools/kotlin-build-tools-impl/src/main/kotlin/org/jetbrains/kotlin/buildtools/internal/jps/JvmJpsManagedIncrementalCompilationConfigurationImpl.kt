/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jps

import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jps.jvm.JvmJpsManagedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.CompilerIncrementalCompilationComponents
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.*
import org.jetbrains.kotlin.buildtools.api.jvm.JvmIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.BaseCompilationOperationImpl.Companion.LOOKUP_TRACKER
import org.jetbrains.kotlin.buildtools.internal.jps.JvmJpsManagedIncrementalCompilationConfigurationImpl.Companion.ENUM_WHEN_TRACKER
import org.jetbrains.kotlin.buildtools.internal.jps.JvmJpsManagedIncrementalCompilationConfigurationImpl.Companion.EXPECT_ACTUAL_TRACKER
import org.jetbrains.kotlin.buildtools.internal.jps.JvmJpsManagedIncrementalCompilationConfigurationImpl.Companion.FILE_MAPPING_TRACKER
import org.jetbrains.kotlin.buildtools.internal.jps.JvmJpsManagedIncrementalCompilationConfigurationImpl.Companion.IMPORT_TRACKER
import org.jetbrains.kotlin.buildtools.internal.jps.JvmJpsManagedIncrementalCompilationConfigurationImpl.Companion.INLINE_CONST_TRACKER
import org.jetbrains.kotlin.buildtools.internal.jps.incremental.IncrementalCompilationComponentsAdapter
import org.jetbrains.kotlin.buildtools.internal.jps.incremental.trackers.*
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.JvmCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.trackers.LookupTrackerAdapter
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.buildtools.internal.jps.JvmJpsManagedIncrementalCompilationConfigurationImpl.Companion.LOOKUP_TRACKER as JPS_LOOKUP_TRACKER

/**
 * Marks a client-managed incremental compilation configuration, so that it can be recognized without mentioning any
 * `kotlin-build-tools-api-jps` type.
 *
 * That module is an optional dependency of this one: it is on the classpath only for consumers that use the JPS part of the API. Loading any
 * of its types when it is absent fails with a [NoClassDefFoundError], and so does loading
 * [JvmJpsManagedIncrementalCompilationConfigurationImpl], which implements its interfaces. This marker is declared here instead, where it is
 * always available, and only that class implements it, so an instance can exist only if api-jps is on the classpath.
 *
 * Therefore this declaration must not reference anything from `kotlin-build-tools-api-jps`.
 */
internal interface JvmClientManagedIncrementalCompilationConfiguration : JvmIncrementalCompilationConfiguration

@OptIn(DelicateBuildToolsApi::class, InternalBuildToolsApi::class)
context(serviceBuilder: Services.Builder, operation: JvmCompilationOperationImpl)
internal fun JvmClientManagedIncrementalCompilationConfiguration.registerPlatformServices(logger: KotlinLogger) {
    check(this is JvmJpsManagedIncrementalCompilationConfigurationImpl) {
        "Unexpected JPS incremental compilation configuration: ${this::class}. It must be an instance of JvmJpsManagedIncrementalCompilationConfigurationImpl."
    }

    serviceBuilder.register(
        IncrementalCompilationComponents::class.java,
        IncrementalCompilationComponentsAdapter(this.incrementalCompilationComponents),
    )

    this[JPS_LOOKUP_TRACKER]?.let { tracker ->
        if (operation[LOOKUP_TRACKER] != null) {
            logger.warn(
                "A lookup tracker is set both as BaseCompilationOperation.LOOKUP_TRACKER and as " +
                        "JvmJpsManagedIncrementalCompilationConfiguration.LOOKUP_TRACKER. The latter takes precedence."
            )
        }

        serviceBuilder.register(LookupTracker::class.java, LookupTrackerAdapter(tracker))
    }

    this[IMPORT_TRACKER]?.let { tracker ->
        serviceBuilder.register(ImportTracker::class.java, ImportTrackerAdapter(tracker))
    }
    this[FILE_MAPPING_TRACKER]?.let { tracker ->
        serviceBuilder.register(ICFileMappingTracker::class.java, FileMappingTrackerAdapter(tracker))
    }
    this[EXPECT_ACTUAL_TRACKER]?.let { tracker ->
        serviceBuilder.register(ExpectActualTracker::class.java, ExpectActualTrackerAdapter(tracker))
    }
    this[ENUM_WHEN_TRACKER]?.let { tracker ->
        serviceBuilder.register(EnumWhenTracker::class.java, EnumWhenTrackerAdapter(tracker))
    }
    this[INLINE_CONST_TRACKER]?.let { tracker ->
        serviceBuilder.register(InlineConstTracker::class.java, InlineConstTrackerAdapter(tracker))
    }
}

@DelicateBuildToolsApi
@OptIn(InternalBuildToolsApi::class)
internal class JvmJpsManagedIncrementalCompilationConfigurationImpl private constructor(
    override val incrementalCompilationComponents: CompilerIncrementalCompilationComponents,
    private val options: Options,
) : JvmJpsManagedIncrementalCompilationConfiguration,
    JvmJpsManagedIncrementalCompilationConfiguration.Builder,
    JvmClientManagedIncrementalCompilationConfiguration,
    DeepCopyable<JvmJpsManagedIncrementalCompilationConfigurationImpl> {

    constructor(incrementalCompilationComponents: CompilerIncrementalCompilationComponents) : this(
        incrementalCompilationComponents,
        Options(JvmJpsManagedIncrementalCompilationConfiguration::class),
    ) {
        initializeOptions(this::class, options)
    }

    override fun build(): JvmJpsManagedIncrementalCompilationConfiguration = deepCopy()

    override fun toBuilder(): JvmJpsManagedIncrementalCompilationConfiguration.Builder = deepCopy()

    override fun deepCopy(): JvmJpsManagedIncrementalCompilationConfigurationImpl =
        JvmJpsManagedIncrementalCompilationConfigurationImpl(incrementalCompilationComponents, options.deepCopy())

    @UseFromImplModuleRestricted
    override fun <V> get(key: JvmJpsManagedIncrementalCompilationConfiguration.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: JvmJpsManagedIncrementalCompilationConfiguration.Option<V>, value: V) {
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
