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
open class IntArrayPlusBenchmark : AbstractSimpleFileBenchmark() {

    @Param("1", "10", "100", "1000", "3000", "5000", "7000", "10000")
    private var size: Int = 0

    @Benchmark
    //@Fork(jvmArgsAppend = ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"])
    fun benchmark(bh: Blackhole) {
        if (!isIR) error("Doesn't make sense to run it on old frontend on buildserver")
        analyzeGreenFile(bh)
    }

    override fun buildText() =
            """
            |fun bar(x: IntArray, y: IntArray) {
            |${(1..size).joinToString("\n") { "    x + y" }}
            |}
            """.trimMargin()
}