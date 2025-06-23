/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.v2.jvm


import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.BACKUP_CLASSES
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.FORCE_RECOMPILATION
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.KEEP_IC_CACHES_IN_MEMORY
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.OUTPUT_DIRS
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.PRECISE_JAVA_TRACKING
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.USE_FIR_RUNNER
import org.jetbrains.kotlin.buildtools.internal.v2.OptionsDelegate

class JvmSnapshotBasedIncrementalCompilationOptionsImpl() : JvmSnapshotBasedIncrementalCompilationOptions {
    private val optionsDelegate = OptionsDelegate()

    init {
        this[PRECISE_JAVA_TRACKING] = true
        this[BACKUP_CLASSES] = false
        this[KEEP_IC_CACHES_IN_MEMORY] = false
        this[FORCE_RECOMPILATION] = false
        this[OUTPUT_DIRS] = null
        this[USE_FIR_RUNNER] = false
        this[ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES] = false
    }

    override fun <V> get(key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>): V = optionsDelegate[key]
    override fun <V> set(key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>, value: V) {
        optionsDelegate[key] = value
    }
}
