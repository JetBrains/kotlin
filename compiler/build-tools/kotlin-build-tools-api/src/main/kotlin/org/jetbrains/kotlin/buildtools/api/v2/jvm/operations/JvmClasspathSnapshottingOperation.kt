/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.api.v2.jvm.operations

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathEntrySnapshot
import org.jetbrains.kotlin.buildtools.api.v2.BuildOperation
import org.jetbrains.kotlin.buildtools.api.v2.internal.BaseOption

public interface JvmClasspathSnapshottingOperation : BuildOperation<ClasspathEntrySnapshot> {
    public class Option<V>(id: String) : BaseOption<V>(id)

    public operator fun <V> get(key: Option<V>): V

    public operator fun <V> set(key: Option<V>, value: V)

    public companion object {

        @JvmField
        public val GRANULARITY: Option<ClassSnapshotGranularity> = Option("GRANULARITY")

        @JvmField
        public val PARSE_INLINED_LOCAL_CLASSES: Option<Boolean> = Option("PARSE_INLINED_LOCAL_CLASSES")
    }
}