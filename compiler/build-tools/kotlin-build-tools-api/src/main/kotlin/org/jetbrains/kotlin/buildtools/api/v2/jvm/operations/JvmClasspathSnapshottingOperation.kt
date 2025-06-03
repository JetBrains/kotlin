/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.v2.jvm.operations

import org.jetbrains.kotlin.buildtools.api.v2.BuildOperation
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmClasspathEntrySnapshot

public interface JvmClasspathSnapshottingOperation : BuildOperation<JvmClasspathEntrySnapshot> {
    public class Option<V> internal constructor(public val id: String)

    public operator fun <V> get(key: Option<V>): V?

    public operator fun <V> set(key: Option<V>, value: V)

    public companion object {
        @JvmField
        public val GRANULARITY: Option<JvmClassSnapshotGranularity> = Option("GRANULARITY")
    }
}