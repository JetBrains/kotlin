/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity

/**
 * Configures how classpath snapshots are done for module's dependencies
 */
data class SnapshotConfig(
    val granularity: ClassSnapshotGranularity,
    val useInlineLambdaSnapshotting: Boolean,
)
