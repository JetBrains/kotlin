/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.buildtools.internal.jvm

import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.internal.*
import java.nio.file.Path

@Suppress("DEPRECATION_ERROR")
internal class JvmSnapshotBasedIncrementalCompilationConfigurationImpl2 private constructor(
    override val workingDirectory: Path,
    override val sourcesChanges: SourcesChanges,
    override val dependenciesSnapshotFiles: List<Path>,
    override val options: Options = Options(JvmSnapshotBasedIncrementalCompilationConfiguration::class),
) : BaseIncrementalCompilationConfigurationImpl(), JvmSnapshotBasedIncrementalCompilationConfiguration,
    JvmSnapshotBasedIncrementalCompilationConfiguration.Builder,
    DeepCopyable<JvmSnapshotBasedIncrementalCompilationConfigurationImpl2>,
    HasSnapshotBasedIcOptionsAccessor {

    constructor(
        workingDirectory: Path,
        sourcesChanges: SourcesChanges,
        dependenciesSnapshotFiles: List<Path>,
    ) : this(
        workingDirectory,
        sourcesChanges,
        dependenciesSnapshotFiles,
        Options(
            JvmSnapshotBasedIncrementalCompilationConfiguration::class,
        )
    ) {
        initializeOptions(this::class, options)
    }

    override fun build(): JvmSnapshotBasedIncrementalCompilationConfiguration = deepCopy()

    override fun toBuilder(): JvmSnapshotBasedIncrementalCompilationConfiguration.Builder = deepCopy()

    override fun deepCopy(): JvmSnapshotBasedIncrementalCompilationConfigurationImpl2 =
        JvmSnapshotBasedIncrementalCompilationConfigurationImpl2(
            workingDirectory,
            sourcesChanges,
            dependenciesSnapshotFiles,
            options.deepCopy()
        )


    @UseFromImplModuleRestricted
    override fun <V> get(key: JvmSnapshotBasedIncrementalCompilationConfiguration.Option<V>): V {
        return options[key]
    }

    @UseFromImplModuleRestricted
    override fun <V> set(key: JvmSnapshotBasedIncrementalCompilationConfiguration.Option<V>, value: V) {
        options[key] = value
    }

    operator fun <V> get(key: Option<V>): V {
        return options[key]
    }

    operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    override fun <V> get(key: BaseOptionWithDefault<V>): V {
        return options[key]
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
    }
}

// Remove in 2.7
internal interface HasSnapshotBasedIcOptionsAccessor {
    val workingDirectory: Path
    val sourcesChanges: SourcesChanges
    val dependenciesSnapshotFiles: List<Path>
    operator fun <V> get(key: BaseOptionWithDefault<V>): V
}

// Remove in 2.7
@Suppress("DEPRECATION_ERROR")
internal fun JvmSnapshotBasedIncrementalCompilationConfiguration.toOptions(): HasSnapshotBasedIcOptionsAccessor {
    // In older BTA-APIs JvmSnapshotBasedIncrementalCompilationConfiguration is final,
    // so we have to avoid loading JvmSnapshotBasedIncrementalCompilationConfigurationImpl, or we'd get a verification error

//    return if (!JvmSnapshotBasedIncrementalCompilationConfiguration::class.java.isInterface || this !is JvmSnapshotBasedIncrementalCompilationConfigurationImpl2) {
//        // we're on an older BTA-API or user created JvmSnapshotBasedIncrementalCompilationConfiguration through the deprecated constructor directly
//        object : HasSnapshotBasedIcOptionsAccessor {
//            override val workingDirectory: Path
//                get() = this@toOptions.workingDirectory
//            override val sourcesChanges: SourcesChanges
//                get() = this@toOptions.sourcesChanges
//            override val dependenciesSnapshotFiles: List<Path>
//                get() = this@toOptions.dependenciesSnapshotFiles
//
//            override fun <V> get(key: BaseOptionWithDefault<V>): V {
//                return (this::class.java.getMethod("getOptions").invoke(this) as JvmSnapshotBasedIncrementalCompilationOptionsImpl)[key]
//            }
//
//        }
//
//    } else {
//        // we're on a newer BTA-API and user created JvmSnapshotBasedIncrementalCompilationConfiguration through the factory method
//        this
//    }
    return this as HasSnapshotBasedIcOptionsAccessor
}
