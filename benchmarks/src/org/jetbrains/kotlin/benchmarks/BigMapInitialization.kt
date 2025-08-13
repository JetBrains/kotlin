/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

/**
 * Issue: KT-45458 (Implement collapsing a lot of similar type variables within one top-level call)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 10, timeUnit = TimeUnit.SECONDS)
open class BigMapInitialization : AbstractSimpleFileBenchmark(){
    @Param("1", "10", "100", "1000")
    private var size: Int = 0

    @Benchmark
    fun benchmark(bh: Blackhole) {
        analyzeGreenFile(bh)
    }

    override fun buildText(): String {
        return """
            |val map = mapOf(
            |${(1..size).joinToString("\n") { "\"$it\" to $it," }}
            |)
            """.trimMargin()
    }
}