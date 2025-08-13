/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 1, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 4, time = 4, timeUnit = TimeUnit.SECONDS)
open class ForEachBenchmark {
    @Param("0", "1", "5", "10", "100", "1000")
    private var size = 0

    lateinit var list: List<Int>

    @Setup
    fun setUp() {
        list = buildList(size) {
            (0..<size).forEach {
                add(it)
            }
        }
    }

    @Benchmark
    fun iteratorFree(bh: Blackhole) {
        for (i in 0 until list.size) {
            bh.consume(list[i])
        }
    }

    @Benchmark
    fun stdLib(bh: Blackhole) {
        for (e in list) {
            bh.consume(e)
        }
    }
}