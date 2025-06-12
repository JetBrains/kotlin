/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.v2.jvm


import org.jetbrains.kotlin.buildtools.api.v2.internal.OptionsDelegate
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions

class JvmSnapshotBasedIncrementalCompilationOptionsImpl() : JvmSnapshotBasedIncrementalCompilationOptions {
    private val optionsDelegate = OptionsDelegate()

    override fun <V> get(key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>): V = optionsDelegate[key] as V
    override fun <V> set(key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>, value: V) {
        optionsDelegate[key] = value
    }
}
