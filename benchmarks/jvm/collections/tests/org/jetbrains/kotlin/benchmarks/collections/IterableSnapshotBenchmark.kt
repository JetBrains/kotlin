/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks.collections

import kotlinx.benchmark.*

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.NANOSECONDS)
open class IterableSnapshotBenchmark : BaseIterableSnapshotBenchmark() {
    @Param(
        "ArrayList",
        // Name rationale: it implements to `List` but doesn't inherit `AbstractCollection` nor implements `RandomAccess`
        "ListWorstCase",
        "HashSet",
    )
    override var sourceKind: String = ""
}

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.NANOSECONDS)
open class SingletonListSnapshotBenchmark : BaseSingletonListSnapshotBenchmark()
