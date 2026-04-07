/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.buildtools.internal.jvm

import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.internal.*
import java.nio.file.Path

@Suppress("DEPRECATION_ERROR")
internal class JvmSnapshotBasedIncrementalCompilationConfigurationImpl private constructor(
    workingDirectory: Path,
    sourcesChanges: SourcesChanges,
    dependenciesSnapshotFiles: List<Path>,
    shrunkClasspathSnapshot: Path,
    private val options2: Options = Options(JvmSnapshotBasedIncrementalCompilationConfiguration::class),
) : JvmSnapshotBasedIncrementalCompilationConfiguration(
    workingDirectory,
    sourcesChanges,
    dependenciesSnapshotFiles,
    shrunkClasspathSnapshot,
), JvmSnapshotBasedIncrementalCompilationConfiguration.Builder,
    DeepCopyable<JvmSnapshotBasedIncrementalCompilationConfigurationImpl>,
    HasSnapshotBasedIcOptionsAccessor {

    constructor(
        workingDirectory: Path,
        sourcesChanges: SourcesChanges,
        dependenciesSnapshotFiles: List<Path>,
        shrunkClasspathSnapshot: Path,
    ) : this(
        workingDirectory,
        sourcesChanges,
        dependenciesSnapshotFiles,
        shrunkClasspathSnapshot,
        Options(
            JvmSnapshotBasedIncrementalCompilationConfiguration::class,
        )
    ) {
        initializeOptions(this::class, options2)
    }

    override fun build(): JvmSnapshotBasedIncrementalCompilationConfiguration = deepCopy()

    override fun toBuilder(): Builder = deepCopy()

    override fun deepCopy(): JvmSnapshotBasedIncrementalCompilationConfigurationImpl =
        JvmSnapshotBasedIncrementalCompilationConfigurationImpl(
            workingDirectory,
            sourcesChanges,
            dependenciesSnapshotFiles,
            shrunkClasspathSnapshot,
            options2.deepCopy()
        )


    @UseFromImplModuleRestricted
    override fun <V> get(key: JvmSnapshotBasedIncrementalCompilationConfiguration.Option<V>): V {
        return options2[key]
    }

    @UseFromImplModuleRestricted
    override fun <V> set(key: JvmSnapshotBasedIncrementalCompilationConfiguration.Option<V>, value: V) {
        options2[key] = value
    }

    operator fun <V> get(key: Option<V>): V {
        return options2[key]
    }

    operator fun <V> set(key: Option<V>, value: V) {
        options2[key] = value
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: BaseIncrementalCompilationConfiguration.Option<V>): V = options2[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: BaseIncrementalCompilationConfiguration.Option<V>, value: V) {
        options2[key] = value
    }

    operator fun <V> get(key: BaseIncrementalCompilationConfigurationImpl.Option<V>): V = options2[key]
    override fun <V> get(key: BaseOptionWithDefault<V>): V {
        return options2[key]
    }

    @OptIn(UseFromImplModuleRestricted::class)
    operator fun <V> set(key: BaseIncrementalCompilationConfigurationImpl.Option<V>, value: V) {
        options2[key] = value
    }

    open class Option<V> : BaseOptionWithDefault<V> {
        constructor(id: String) : super(id)
        constructor(id: String, default: V) : super(id, default = default)
    }

    companion object {

        val PRECISE_JAVA_TRACKING: Option<Boolean> = Option("PRECISE_JAVA_TRACKING", false)

        val ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES: Option<Boolean> =
            Option("ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES", false)

        val USE_FIR_RUNNER: Option<Boolean> = Option("USE_FIR_RUNNER", false)

        // copied from BaseCompilationConfigurationImpl so initializeOptions works

        val ROOT_PROJECT_DIR: Option<Path?> = Option("ROOT_PROJECT_DIR", null)

        val MODULE_BUILD_DIR: Option<Path?> = Option("MODULE_BUILD_DIR", null)

        val BACKUP_CLASSES: Option<Boolean> = Option("BACKUP_CLASSES", false)

        val KEEP_IC_CACHES_IN_MEMORY: Option<Boolean> = Option("KEEP_IC_CACHES_IN_MEMORY", false)

        val FORCE_RECOMPILATION: Option<Boolean> = Option("FORCE_RECOMPILATION", false)

        val OUTPUT_DIRS: Option<Set<Path>?> = Option("OUTPUT_DIRS", null)

        val UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM: Option<Boolean> =
            Option("UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM", false)

        val MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION: Option<Boolean> = Option("MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION", true)

        val TRACK_CONFIGURATION_INPUTS: Option<Boolean> = Option("TRACK_CONFIGURATION_INPUTS", false)
    }
}

// Remove in 2.7
internal interface HasSnapshotBasedIcOptionsAccessor {
    operator fun <V> get(key: BaseOptionWithDefault<V>): V
}

// Remove in 2.7
@Suppress("DEPRECATION_ERROR")
internal fun JvmSnapshotBasedIncrementalCompilationConfiguration.toOptions(): HasSnapshotBasedIcOptionsAccessor {
    // In older BTA-APIs JvmSnapshotBasedIncrementalCompilationConfiguration is final,
    // so we have to avoid loading JvmSnapshotBasedIncrementalCompilationConfigurationImpl, or we'd get a verification error

    return if (JvmSnapshotBasedIncrementalCompilationConfiguration::class.isFinal || this !is JvmSnapshotBasedIncrementalCompilationConfigurationImpl) {
        // we're on an older BTA-API or user created JvmSnapshotBasedIncrementalCompilationConfiguration through the deprecated constructor directly
        this::class.java.getMethod("getOptions").invoke(this) as JvmSnapshotBasedIncrementalCompilationOptionsImpl
    } else {
        // we're on a newer BTA-API and user created JvmSnapshotBasedIncrementalCompilationConfiguration through the factory method
        this
    }
}
