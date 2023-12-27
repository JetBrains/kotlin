/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.jvm.IncrementalJvmCompilationConfiguration
import org.jetbrains.kotlin.incremental.IncrementalCompilationFeatures

private typealias ICConfiguration = IncrementalJvmCompilationConfiguration<*>

/**
 * IncrementalJvmCompilationConfiguration provides single-property API for forward-compatibility.
 *
 * configurationAdapters are there to regroup the properties and work with higher-level interfaces.
 */

internal fun ICConfiguration.extractIncrementalCompilationFeatures(): IncrementalCompilationFeatures {
    return IncrementalCompilationFeatures(
        withAbiSnapshot = false,
        preciseCompilationResultsBackup = preciseCompilationResultsBackupEnabled,
        keepIncrementalCompilationCachesInMemory = incrementalCompilationCachesKeptInMemory,
    )
}
