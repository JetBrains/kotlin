/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.v2.jvm.operations

import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.v2.BuildOperation
import org.jetbrains.kotlin.buildtools.api.v2.internal.Option.WithDefault
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmClasspathEntrySnapshot

public interface JvmClasspathSnapshottingOperation : BuildOperation<JvmClasspathEntrySnapshot> {
    public interface Option<V>

    public operator fun <V> get(key: Option<V>): V

    public operator fun <V> set(key: Option<V>, value: V)

    public companion object {

        private fun <V> optional(id: String, defaultValue: V): Option<V> =
            object : WithDefault<V>(id, defaultValue), Option<V> {}

        @JvmField
        public val GRANULARITY: Option<ClassSnapshotGranularity> = optional("GRANULARITY", ClassSnapshotGranularity.CLASS_MEMBER_LEVEL)

        @JvmField
        public val PARSE_INLINED_LOCAL_CLASSES: Option<Boolean> = optional("PARSE_INLINED_LOCAL_CLASSES", true)
    }
}