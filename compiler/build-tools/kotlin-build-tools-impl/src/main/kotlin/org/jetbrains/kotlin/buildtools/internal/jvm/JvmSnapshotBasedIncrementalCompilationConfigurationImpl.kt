/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:UseSerializers(PathAsStringSerializer::class)

package org.jetbrains.kotlin.buildtools.internal.jvm

import kotlinx.serialization.SerialName
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.getPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.internal.serializability.setPropertyWithSerialNameValue
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
        ).also { it.copyFrom(this) }

    private fun copyFrom(from: JvmSnapshotBasedIncrementalCompilationConfigurationImpl) {
        preciseJavaTracking = from.preciseJavaTracking
        assuredNoClasspathSnapshotChanges = from.assuredNoClasspathSnapshotChanges
        useFirRunner = from.useFirRunner
        rootProjectDir = from.rootProjectDir
        moduleBuildDir = from.moduleBuildDir
        backupClasses = from.backupClasses
        keepIcCachesInMemory = from.keepIcCachesInMemory
        forceRecompilation = from.forceRecompilation
        outputDirs = from.outputDirs
        unsafeIncrementalCompilationForMultiplatform = from.unsafeIncrementalCompilationForMultiplatform
        monotonousIncrementalCompileSetExpansion = from.monotonousIncrementalCompileSetExpansion
        trackConfigurationInputs = from.trackConfigurationInputs
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: JvmSnapshotBasedIncrementalCompilationConfiguration.Option<V>): V = get(key.id)

    private fun <V> get(id: String): V =
        JvmSnapshotBasedIncrementalCompilationConfigurationImpl::class.getPropertyWithSerialNameValue(this, id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: JvmSnapshotBasedIncrementalCompilationConfiguration.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        set(key.id, value)
    }

    operator fun <V> get(key: Option<V>): V = get(key.id)

    operator fun <V> set(key: Option<V>, value: V) {
        set(key.id, value)
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: BaseIncrementalCompilationConfiguration.Option<V>): V = get(key.id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: BaseIncrementalCompilationConfiguration.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        set(key.id, value)
    }

    operator fun <V> get(key: BaseIncrementalCompilationConfigurationImpl.Option<V>): V = get(key.id)
    override fun <V> get(key: BaseOptionWithDefault<V>): V = get(key.id)

    operator fun <V> set(key: BaseIncrementalCompilationConfigurationImpl.Option<V>, value: V) {
        set(key.id, value)
    }

    private fun <V> set(id: String, value: V) {
        JvmSnapshotBasedIncrementalCompilationConfigurationImpl::class.setPropertyWithSerialNameValue(this, id, value)
    }

    open class Option<V>(id: String) : BaseOption<V>(id)

    @SerialName("PRECISE_JAVA_TRACKING")
    var preciseJavaTracking: Boolean = false

    @SerialName("ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES")
    var assuredNoClasspathSnapshotChanges: Boolean = false

    @SerialName("USE_FIR_RUNNER")
    var useFirRunner: Boolean = false

    @SerialName("ROOT_PROJECT_DIR")
    var rootProjectDir: Path? = null

    @SerialName("MODULE_BUILD_DIR")
    var moduleBuildDir: Path? = null

    @SerialName("BACKUP_CLASSES")
    var backupClasses: Boolean = false

    @SerialName("KEEP_IC_CACHES_IN_MEMORY")
    var keepIcCachesInMemory: Boolean = false

    @SerialName("FORCE_RECOMPILATION")
    var forceRecompilation: Boolean = false

    @SerialName("OUTPUT_DIRS")
    var outputDirs: Set<Path>? = null

    @SerialName("UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM")
    var unsafeIncrementalCompilationForMultiplatform: Boolean = false

    @SerialName("MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION")
    var monotonousIncrementalCompileSetExpansion: Boolean = true

    @SerialName("TRACK_CONFIGURATION_INPUTS")
    var trackConfigurationInputs: Boolean = false

    companion object {
        val PRECISE_JAVA_TRACKING: Option<Boolean> = Option("PRECISE_JAVA_TRACKING")
        val ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES: Option<Boolean> =
            Option("ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES")
        val USE_FIR_RUNNER: Option<Boolean> = Option("USE_FIR_RUNNER")

        // copied from BaseCompilationConfigurationImpl so initializeOptions works
        val ROOT_PROJECT_DIR: Option<Path?> = Option("ROOT_PROJECT_DIR")
        val MODULE_BUILD_DIR: Option<Path?> = Option("MODULE_BUILD_DIR")
        val BACKUP_CLASSES: Option<Boolean> = Option("BACKUP_CLASSES")
        val KEEP_IC_CACHES_IN_MEMORY: Option<Boolean> = Option("KEEP_IC_CACHES_IN_MEMORY")
        val FORCE_RECOMPILATION: Option<Boolean> = Option("FORCE_RECOMPILATION")
        val OUTPUT_DIRS: Option<Set<Path>?> = Option("OUTPUT_DIRS")
        val UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM: Option<Boolean> =
            Option("UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM")
        val MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION: Option<Boolean> = Option("MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION")
        val TRACK_CONFIGURATION_INPUTS: Option<Boolean> = Option("TRACK_CONFIGURATION_INPUTS")
    }
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
