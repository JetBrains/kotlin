/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.dirtyFiles

import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.debug
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.NoChanges
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableDueToMissingClasspathSnapshot
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotBuildReporter
import org.jetbrains.kotlin.incremental.libraryDiff.klib.KlibChangesComputer.computeKlibChanges
import org.jetbrains.kotlin.incremental.libraryDiff.klib.LazyKlibSnapshot

internal class KlibSnapshotBasedImpactDeterminer(
    private val caches: IncrementalJsCachesManager,
    private val classpathChanges: ClasspathChanges,
    private val lazyKlibSnapshot: LazyKlibSnapshot,
    private val reporter: BuildReporter<BuildTimeMetric, BuildPerformanceMetric>,
) : ImpactedFilesDeterminer {

    override fun determineChangedAndImpactedSymbols(): ChangesEither {
        reporter.debug { "Klib changes info passed from the build tool: ${classpathChanges::class.simpleName}" }

        return when (classpathChanges) {
            is NoChanges -> ChangesEither.Known(emptySet(), emptySet())
            is ToBeComputedByIncrementalCompiler -> reporter.measure(COMPUTE_CLASSPATH_CHANGES) {
                reporter.addMetric(COMPUTE_CLASSPATH_CHANGES_EXECUTION_COUNT, 1)
                val klibChanges = computeKlibChanges(caches.lookupCache, lazyKlibSnapshot, ClasspathSnapshotBuildReporter(reporter))
                // `klibChanges` contains changed and impacted symbols in the libraries.
                // We also need to compute symbols in the current module that are impacted by `klibChanges`.
                klibChanges.toChangeInfoList().getChangedAndImpactedSymbols(listOf(caches.platformCache), reporter).toChangesEither()
            }
            is NotAvailableDueToMissingClasspathSnapshot -> ChangesEither.Unknown(BuildAttribute.CLASSPATH_SNAPSHOT_NOT_FOUND)
            is NotAvailableForNonIncrementalRun -> ChangesEither.Unknown(BuildAttribute.UNKNOWN_CHANGES_IN_GRADLE_INPUTS)
            is ClasspathSnapshotDisabled -> error("Unexpected ClasspathSnapshotDisabled for this code path: ${classpathChanges.javaClass.name}.")
            is NotAvailableForJSCompiler -> error("Unexpected NotAvailableForJSCompiler for this code path: ${classpathChanges.javaClass.name}.")
        }
    }
}
