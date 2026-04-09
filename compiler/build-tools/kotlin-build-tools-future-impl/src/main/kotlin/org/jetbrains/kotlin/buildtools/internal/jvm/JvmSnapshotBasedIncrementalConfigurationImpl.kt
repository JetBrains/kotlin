/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION", "DEPRECATION_ERROR")

package org.jetbrains.kotlin.buildtools.internal.jvm

import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.internal.BaseOptionWithDefault
import org.jetbrains.kotlin.buildtools.internal.Options
import org.jetbrains.kotlin.buildtools.internal.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.internal.initializeOptions
import java.nio.file.Path

class JvmSnapshotBasedIncrementalCompilationConfigurationImpl private constructor(
    workingDirectory: Path,
    sourcesChanges: SourcesChanges,
    dependenciesSnapshotFiles: List<Path>,
    shrunkClasspathSnapshot: Path,
    @Deprecated("Use `get` and `set` directly instead. This property will be removed in a future release.") // Remove in 2.7
    override val options: JvmSnapshotBasedIncrementalCompilationOptionsImpl = JvmSnapshotBasedIncrementalCompilationOptionsImpl(
        Options(
            JvmSnapshotBasedIncrementalCompilationConfiguration::class,
        )
    ),
) : JvmSnapshotBasedIncrementalCompilationConfiguration(
    workingDirectory,
    sourcesChanges,
    dependenciesSnapshotFiles,
    shrunkClasspathSnapshot,
    options
), JvmSnapshotBasedIncrementalCompilationConfiguration.Builder,
    org.jetbrains.kotlin.buildtools.internal.DeepCopyable<JvmSnapshotBasedIncrementalCompilationConfigurationImpl>, HasSnapshotBasedIcOptionsAccessor {

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
        JvmSnapshotBasedIncrementalCompilationOptionsImpl(
            Options(
                JvmSnapshotBasedIncrementalCompilationConfiguration::class,
            )
        ),
    ) {
        initializeOptions(JvmSnapshotBasedIncrementalCompilationOptionsImpl::class, options.options)
    }

    override fun build(): JvmSnapshotBasedIncrementalCompilationConfiguration = deepCopy()

    override fun toBuilder(): Builder = deepCopy()

    override fun deepCopy(): JvmSnapshotBasedIncrementalCompilationConfigurationImpl =
        JvmSnapshotBasedIncrementalCompilationConfigurationImpl(
            workingDirectory,
            sourcesChanges,
            dependenciesSnapshotFiles,
            shrunkClasspathSnapshot,
            options.deepCopy()
        )


    @UseFromImplModuleRestricted
    override fun <V> get(key: Option<V>): V {
        return options.options[key]
    }

    @UseFromImplModuleRestricted
    override fun <V> set(key: Option<V>, value: V) {
        options.options[key] = value
    }

    override operator fun <V> get(key: JvmSnapshotBasedIncrementalCompilationOptionsImpl.Option<V>): V {
        return options.options[key]
    }

    operator fun <V> set(key: JvmSnapshotBasedIncrementalCompilationOptionsImpl.Option<V>, value: V) {
        options.options[key] = value
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: BaseIncrementalCompilationConfiguration.Option<V>): V = options.options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: BaseIncrementalCompilationConfiguration.Option<V>, value: V) {
        options.options[key] = value
    }

    operator fun <V> get(key: BaseOptionWithDefault<V>): V {
        return options.options[key]
    }
}
//
//// Remove in 2.7
//@Deprecated("Use `JvmSnapshotBasedIncrementalCompilationConfiguration` and `JvmCompilationOperation.snapshotBasedIcConfigurationBuilder`. This interface will be removed in a future release.")
//internal class JvmSnapshotBasedIncrementalCompilationOptionsImpl internal constructor(
//    public override val options: Options = Options(JvmSnapshotBasedIncrementalCompilationOptions::class),
//) : BaseIncrementalCompilationConfigurationImpl(), JvmSnapshotBasedIncrementalCompilationOptions,
//    org.jetbrains.kotlin.buildtools.internal.DeepCopyable<JvmSnapshotBasedIncrementalCompilationOptionsImpl>, HasSnapshotBasedIcOptionsAccessor {
//
//    constructor() : this(Options(JvmSnapshotBasedIncrementalCompilationOptions::class)) {
//        initializeOptions(this::class, options)
//    }
//
//    override fun deepCopy(): JvmSnapshotBasedIncrementalCompilationOptionsImpl =
//        JvmSnapshotBasedIncrementalCompilationOptionsImpl(options.deepCopy())
//
//    override operator fun <V> get(key: Option<V>): V = options[key]
//
//    @OptIn(UseFromImplModuleRestricted::class)
//    operator fun <V> set(key: Option<V>, value: V) {
//        options[key] = value
//    }
//
//    @UseFromImplModuleRestricted
//    override fun <V> get(key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>): V = options[key]
//
//    @UseFromImplModuleRestricted
//    override fun <V> set(key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>, value: V) {
//        options[key] = value
//    }
//
//    @UseFromImplModuleRestricted
//    override fun <V> get(key: BaseIncrementalCompilationConfiguration.Option<V>): V {
//        return options[key]
//    }
//
//    open class Option<V> : BaseOptionWithDefault<V> {
//        constructor(id: String) : super(id)
//        constructor(id: String, default: V) : super(id, default = default)
//    }
//
//    companion object {
//
//        val PRECISE_JAVA_TRACKING: Option<Boolean> = Option("PRECISE_JAVA_TRACKING", false)
//
//        val ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES: Option<Boolean> =
//            Option("ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES", false)
//
//        val USE_FIR_RUNNER: Option<Boolean> = Option("USE_FIR_RUNNER", false)
//    }
//}
