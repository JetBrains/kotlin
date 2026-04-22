/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jvm

import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.internal.*
import java.nio.file.Path

internal class JvmSnapshotBasedIncrementalCompilationConfigurationImpl @Suppress("DEPRECATION") private constructor(
    workingDirectory: Path,
    sourcesChanges: SourcesChanges,
    dependenciesSnapshotFiles: List<Path>,
    shrunkClasspathSnapshot: Path,
    // this can be renamed to options after we get rid of the superclass `options: JvmSnapshotBasedIncrementalCompilationOptions`
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

    @Suppress("DEPRECATION")
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
        checkOptionIsAvailableForVersion(key)
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
        checkOptionIsAvailableForVersion(key)
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

            @Suppress("DEPRECATION")
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
