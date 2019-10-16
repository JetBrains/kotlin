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
open class ControlFlowOperators : AbstractSimpleFileBenchmark(){

    @Param("1", "100", "1000", "3000", "5000", "7000", "10000")
    private var size: Int = 0

    @Benchmark
    fun benchmark(bh: Blackhole) {
        analyzeGreenFile(bh)
    }

    override fun buildText() =
            """
            |var isTrue = true
            |var s = ""
            |fun bar() {
            |${(1..size).joinToString("\n") {
                """
                |var x$it: String
                |
                |when (s) {
                |   "A" -> { x$it = "1" }
                |   "B" -> { x$it = "2" }
                |   else -> { x$it = "3" }
                |}
                |
                |while (isTrue) {
                |   x$it.hashCode()
                |}
                """.trimMargin()
            }}
            |}
            """.trimMargin()
}
