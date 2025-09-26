/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import org.jetbrains.kotlin.incremental.IncrementalCompilationFeatures
import org.jetbrains.kotlin.server.IncrementalCompilationFeaturesProto

fun IncrementalCompilationFeatures.toProto(): IncrementalCompilationFeaturesProto {
    return IncrementalCompilationFeaturesProto.newBuilder()
        .setUsePreciseJavaTracking(usePreciseJavaTracking)
        .setWithAbiSnapshot(withAbiSnapshot)
        .setPreciseCompilationResultsBackup(preciseCompilationResultsBackup)
        .setKeepIncrementalCompilationCachesInMemory(keepIncrementalCompilationCachesInMemory)
        .setEnableUnsafeIncrementalCompilationForMultiplatform(enableUnsafeIncrementalCompilationForMultiplatform)
        .setEnableMonotonousIncrementalCompileSetExpansion(enableMonotonousIncrementalCompileSetExpansion)
        .build()
}

fun IncrementalCompilationFeaturesProto.toDomain(): IncrementalCompilationFeatures {
    return IncrementalCompilationFeatures(
        usePreciseJavaTracking,
        withAbiSnapshot,
        preciseCompilationResultsBackup,
        keepIncrementalCompilationCachesInMemory,
        enableUnsafeIncrementalCompilationForMultiplatform,
        enableMonotonousIncrementalCompileSetExpansion
    )
}