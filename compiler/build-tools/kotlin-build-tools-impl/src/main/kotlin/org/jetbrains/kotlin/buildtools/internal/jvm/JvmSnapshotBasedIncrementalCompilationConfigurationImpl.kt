/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:UseSerializers(PathAsStringSerializer::class)

package org.jetbrains.kotlin.buildtools.internal.jvm

import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.cast
import org.jetbrains.kotlin.buildtools.internal.serializability.throwPropertyNotFound
import java.nio.file.Path

@Suppress("DEPRECATION_ERROR")
internal class JvmSnapshotBasedIncrementalCompilationConfigurationImpl(
    workingDirectory: Path,
    sourcesChanges: SourcesChanges,
    dependenciesSnapshotFiles: List<Path>,
    shrunkClasspathSnapshot: Path,
) : JvmSnapshotBasedIncrementalCompilationConfiguration(
    workingDirectory,
    sourcesChanges,
    dependenciesSnapshotFiles,
    shrunkClasspathSnapshot,
    /**
     * Required for forward compatibility 2.3 -> 2.4
     */
    DummyOptions,
), JvmSnapshotBasedIncrementalCompilationConfiguration.Builder,
    DeepCopyable<JvmSnapshotBasedIncrementalCompilationConfigurationImpl>,
    HasSnapshotBasedIcOptionsAccessor {

    override fun build(): JvmSnapshotBasedIncrementalCompilationConfiguration = deepCopy()

    override fun toBuilder(): Builder = deepCopy()

    override fun deepCopy(): JvmSnapshotBasedIncrementalCompilationConfigurationImpl =
        JvmSnapshotBasedIncrementalCompilationConfigurationImpl(
            workingDirectory,
            sourcesChanges,
            dependenciesSnapshotFiles.toList(),
            shrunkClasspathSnapshot,
        ).also {
            it.preciseJavaTracking = preciseJavaTracking
            it.assuredNoClasspathSnapshotChanges = assuredNoClasspathSnapshotChanges
            it.useFirRunner = useFirRunner
            it.rootProjectDir = rootProjectDir
            it.moduleBuildDir = moduleBuildDir
            it.backupClasses = backupClasses
            it.keepIcCachesInMemory = keepIcCachesInMemory
            it.forceRecompilation = forceRecompilation
            it.outputDirs = outputDirs
            it.unsafeIncrementalCompilationForMultiplatform = unsafeIncrementalCompilationForMultiplatform
            it.monotonousIncrementalCompileSetExpansion = monotonousIncrementalCompileSetExpansion
            it.trackConfigurationInputs = trackConfigurationInputs
        }

    var preciseJavaTracking: Boolean = false

    var assuredNoClasspathSnapshotChanges: Boolean = false

    var useFirRunner: Boolean = false

    var rootProjectDir: Path? = null

    var moduleBuildDir: Path? = null

    var backupClasses: Boolean = false

    var keepIcCachesInMemory: Boolean = false

    var forceRecompilation: Boolean = false

    var outputDirs: Set<Path>? = null

    var unsafeIncrementalCompilationForMultiplatform: Boolean = false

    var monotonousIncrementalCompileSetExpansion: Boolean = true

    var trackConfigurationInputs: Boolean = false

    @Suppress("UNCHECKED_CAST")
    private fun <V> get(key: String): V = when (key) {
        "PRECISE_JAVA_TRACKING" -> preciseJavaTracking
        "ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES" -> assuredNoClasspathSnapshotChanges
        "USE_FIR_RUNNER" -> useFirRunner
        "ROOT_PROJECT_DIR" -> rootProjectDir
        "MODULE_BUILD_DIR" -> moduleBuildDir
        "BACKUP_CLASSES" -> backupClasses
        "KEEP_IC_CACHES_IN_MEMORY" -> keepIcCachesInMemory
        "FORCE_RECOMPILATION" -> forceRecompilation
        "OUTPUT_DIRS" -> outputDirs
        "UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM" -> unsafeIncrementalCompilationForMultiplatform
        "MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION" -> monotonousIncrementalCompileSetExpansion
        "TRACK_CONFIGURATION_INPUTS" -> trackConfigurationInputs
        else -> throwPropertyNotFound(key)
    } as V

    private fun <V> set(key: String, value: V) {
        when (key) {
            "PRECISE_JAVA_TRACKING" -> preciseJavaTracking = value.cast()
            "ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES" -> assuredNoClasspathSnapshotChanges = value.cast()
            "USE_FIR_RUNNER" -> useFirRunner = value.cast()
            "ROOT_PROJECT_DIR" -> rootProjectDir = value.cast()
            "MODULE_BUILD_DIR" -> moduleBuildDir = value.cast()
            "BACKUP_CLASSES" -> backupClasses = value.cast()
            "KEEP_IC_CACHES_IN_MEMORY" -> keepIcCachesInMemory = value.cast()
            "FORCE_RECOMPILATION" -> forceRecompilation = value.cast()
            "OUTPUT_DIRS" -> outputDirs = value.cast()
            "UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM" -> unsafeIncrementalCompilationForMultiplatform = value.cast()
            "MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION" -> monotonousIncrementalCompileSetExpansion = value.cast()
            "TRACK_CONFIGURATION_INPUTS" -> trackConfigurationInputs = value.cast()
            else -> throwPropertyNotFound(key)
        }
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: Option<V>): V = get(key.id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        set(key.id, value)
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: BaseIncrementalCompilationConfiguration.Option<V>): V = get(key.id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: BaseIncrementalCompilationConfiguration.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        set(key.id, value)
    }

    override fun <V> get(key: BaseOptionWithDefault<V>): V = get(key.id)
}

/**
 * Required for forward compatibility 2.3 -> 2.4
 */
@Suppress("DEPRECATION_ERROR")
private object DummyOptions : JvmSnapshotBasedIncrementalCompilationOptions {
    override fun <V> get(key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>): V {
        error("Not implemented. Do not use `JvmSnapshotBasedIncrementalCompilationConfiguration.options` - it's deprecated. Use JvmSnapshotBasedIncrementalCompilationConfiguration.get to read option values")
    }

    override fun <V> set(
        key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>,
        value: V,
    ) {
        error("Not implemented. Do not use `JvmSnapshotBasedIncrementalCompilationConfiguration.options` - it's deprecated. Use JvmSnapshotBasedIncrementalCompilationConfiguration.Builder.set to set option values")
    }

    override fun <V> get(key: BaseIncrementalCompilationConfiguration.Option<V>): V {
        error("Not implemented. Do not use `JvmSnapshotBasedIncrementalCompilationConfiguration.options` - it's deprecated. Use JvmSnapshotBasedIncrementalCompilationConfiguration.get to read option values")
    }
}

internal interface HasSnapshotBasedIcOptionsAccessor {
    val workingDirectory: Path
    val sourcesChanges: SourcesChanges
    val dependenciesSnapshotFiles: List<Path>
    val shrunkClasspathSnapshot: Path
    operator fun <V> get(key: BaseOptionWithDefault<V>): V
}

internal fun JvmSnapshotBasedIncrementalCompilationConfiguration.toOptions(): HasSnapshotBasedIcOptionsAccessor {

    // In a future version of BTA, we will change the JvmSnapshotBasedIncrementalCompilationConfiguration class
    // into an interface, and provide an instance of it through a compatibility wrapper. Need to access its options
    // through reflection.
    if (JvmSnapshotBasedIncrementalCompilationConfiguration::class.java.isInterface) {
        // The compatibility wrapper will be defined in API, so it will not have access to BaseOptionWithDefault
        class Option<V>(key: BaseOptionWithDefault<V>) : BaseOption<V>(key.id)

        val getMethod = this@toOptions::class.java.getMethod("get", BaseOption::class.java)

        return object : HasSnapshotBasedIcOptionsAccessor {
            override val workingDirectory: Path by lazy(LazyThreadSafetyMode.PUBLICATION) {
                this@toOptions::class.java.getMethod("getWorkingDirectory").invoke(this@toOptions) as Path
            }

            override val sourcesChanges: SourcesChanges by lazy(LazyThreadSafetyMode.PUBLICATION) {
                this@toOptions::class.java.getMethod("getSourcesChanges").invoke(this@toOptions) as SourcesChanges
            }

            @Suppress("UNCHECKED_CAST")
            override val dependenciesSnapshotFiles: List<Path> by lazy(LazyThreadSafetyMode.PUBLICATION) {
                this@toOptions::class.java.getMethod("getDependenciesSnapshotFiles").invoke(this@toOptions) as List<Path>
            }

            override val shrunkClasspathSnapshot: Path by lazy(LazyThreadSafetyMode.PUBLICATION) {
                this@toOptions::class.java.getMethod("getShrunkClasspathSnapshot").invoke(this@toOptions) as Path
            }

            override fun <V> get(key: BaseOptionWithDefault<V>): V {
                val baseOption = Option(key)
                @Suppress("UNCHECKED_CAST")
                return getMethod.invoke(this@toOptions, baseOption) as V
            }
        }
    }
    // In older BTA-APIs JvmSnapshotBasedIncrementalCompilationConfiguration is final,
    // so we have to avoid loading JvmSnapshotBasedIncrementalCompilationConfigurationImpl, or we'd get a verification error

    return if (JvmSnapshotBasedIncrementalCompilationConfiguration::class.isFinal || this !is JvmSnapshotBasedIncrementalCompilationConfigurationImpl) {
        // we're on an older BTA-API or user created JvmSnapshotBasedIncrementalCompilationConfiguration through the deprecated constructor directly
        val options = this::class.java.getMethod("getOptions").invoke(this) as JvmSnapshotBasedIncrementalCompilationOptionsImpl
        object : HasSnapshotBasedIcOptionsAccessor {
            override val workingDirectory: Path
                get() = this@toOptions.workingDirectory
            override val sourcesChanges: SourcesChanges
                get() = this@toOptions.sourcesChanges
            override val dependenciesSnapshotFiles: List<Path>
                get() = this@toOptions.dependenciesSnapshotFiles

            @Suppress("DEPRECATION_ERROR")
            override val shrunkClasspathSnapshot: Path
                get() = this@toOptions.shrunkClasspathSnapshot

            override fun <V> get(key: BaseOptionWithDefault<V>): V {
                return options[key]
            }
        }
    } else {
        // we're on a newer BTA-API and user created JvmSnapshotBasedIncrementalCompilationConfiguration through the factory method
        this
    }
}
