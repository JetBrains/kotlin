/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DEPRECATION_ERROR")

package org.jetbrains.kotlin.buildtools.internal.jvm

import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.internal.*
import java.nio.file.Path

internal class JvmSnapshotBasedIncrementalCompilationOptionsImpl internal constructor(
    public override val options: Options = Options(JvmSnapshotBasedIncrementalCompilationOptions::class),
) : BaseIncrementalCompilationConfigurationImpl(), JvmSnapshotBasedIncrementalCompilationOptions,
    DeepCopyable<JvmSnapshotBasedIncrementalCompilationOptionsImpl>, HasSnapshotBasedIcOptionsAccessor {

    constructor() : this(Options(JvmSnapshotBasedIncrementalCompilationOptions::class)) {
        initializeOptions(this::class, options)
    }

    override fun deepCopy(): JvmSnapshotBasedIncrementalCompilationOptionsImpl =
        JvmSnapshotBasedIncrementalCompilationOptionsImpl(options.deepCopy())

    operator fun <V> get(key: Option<V>): V = options[key]

    @OptIn(UseFromImplModuleRestricted::class)
    operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>, value: V) {
        options[key] = value
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: BaseIncrementalCompilationConfiguration.Option<V>): V {
        return options[key]
    }

    override val workingDirectory: Path
        get() = error("should not be used")
    override val sourcesChanges: SourcesChanges
        get() = error("should not be used")
    override val dependenciesSnapshotFiles: List<Path>
        get() = error("should not be used")
    override val shrunkClasspathSnapshot: Path
        get() = error("should not be used")

    override fun <V> get(key: BaseOptionWithDefault<V>): V {
        return options[key]
    }

    open class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

    companion object {

        val PRECISE_JAVA_TRACKING: Option<Boolean> = Option("PRECISE_JAVA_TRACKING", false)

        val ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES: Option<Boolean> =
            Option("ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES", false)

        val USE_FIR_RUNNER: Option<Boolean> = Option("USE_FIR_RUNNER", false)
    }
}
