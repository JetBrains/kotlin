/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
open class ControlFlowAnalysisBenchmark : AbstractSimpleFileBenchmark() {
    @Param("1000")
    private var size: Int = 0

    @Benchmark
    fun benchmark(bh: Blackhole) {
        analyzeGreenFile(bh)
    }

    override fun buildText() =
        buildString {
            appendLine("fun test() {")
            for (i in 0 until size) {
                appendLine("for (i$i in 0..10) { ")
            }
            for (i in 0 until size) {
                appendLine("}")
            }
            appendLine("}")
        }
}