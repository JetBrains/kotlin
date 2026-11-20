/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.dirtyFiles

import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTimeMetric
import org.jetbrains.kotlin.incremental.ChangesEither
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.IncrementalJsCachesManager
import org.jetbrains.kotlin.incremental.libraryDiff.klib.LazyKlibSnapshot

internal class KlibSnapshotBasedImpactDeterminer(
    private val caches: IncrementalJsCachesManager,
    private val classpathChanges: ClasspathChanges,
    private val lazyKlibSnapshot: LazyKlibSnapshot,
    private val reporter: BuildReporter<BuildTimeMetric, BuildPerformanceMetric>,
) : ImpactedFilesDeterminer {

    override fun determineChangedAndImpactedSymbols(): ChangesEither {
        TODO("KT-90559 step 2")
    }
}
