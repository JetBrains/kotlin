/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.buildtools.internal.jvm

import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.internal.BaseOptionWithDefault
import org.jetbrains.kotlin.buildtools.internal.DeepCopyable
import org.jetbrains.kotlin.buildtools.internal.Options

public interface HasSnapshotBasedIcOptionsAccessor {
    public operator fun <V> get(key: JvmSnapshotBasedIncrementalCompilationOptionsImpl.Option<V>): V
}

public class JvmSnapshotBasedIncrementalCompilationOptionsImpl(
    public val options: Options = Options(JvmSnapshotBasedIncrementalCompilationOptions::class),
) : JvmSnapshotBasedIncrementalCompilationOptions, DeepCopyable<JvmSnapshotBasedIncrementalCompilationOptionsImpl> {
    override fun <V> get(key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>): V {
        TODO("Not yet implemented")
    }

    override fun <V> set(key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>, value: V) {
        TODO("Not yet implemented")
    }

    override fun <V> get(key: BaseIncrementalCompilationConfiguration.Option<V>): V {
        TODO("Not yet implemented")
    }

    override fun deepCopy(): JvmSnapshotBasedIncrementalCompilationOptionsImpl {
        TODO("Not yet implemented")
    }

    public class Option<V>(id:String): BaseOptionWithDefault<V>(id) {}
}
