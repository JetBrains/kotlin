/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
open class InferenceFromArgumentCallsBenchmark : AbstractInferenceBenchmark() {

    @Param("1", "10", "100", "1000", "5000", "10000")
    private var size: Int = 0

    @Benchmark
    fun benchmark(bh: Blackhole) {
        analyzeGreenFile(bh)
    }

    override fun buildText() =
            """
            |fun <T> foo(x: T): Int = 1
            |fun expectsInt(x: Int) {}
            |fun bar(v: Int) {
            |${(1..size).map { "    expectsInt(foo(v))" }.joinToString("\n")}
            |}
            """.trimMargin()
}