/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

/**
 * Issue: KT-16285 (Compilation time rises exponentially on nested "arrayOf(...)")
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 10, timeUnit = TimeUnit.SECONDS)
open class ArrayOfChain : AbstractSimpleFileBenchmark(){
    @Param("10", "100", "1000")
    private var size: Int = 0

    @Benchmark
    fun benchmark(bh: Blackhole) {
        analyzeGreenFile(bh)
    }

    override fun buildText(): String {
        return buildString {
            for (i in 0 until size) {
                append("val a$i = ")
                if (i == 0) {
                    appendLine("0")
                } else {
                    appendLine("arrayOf(a${i - 1})")
                }
            }
        }
    }
}