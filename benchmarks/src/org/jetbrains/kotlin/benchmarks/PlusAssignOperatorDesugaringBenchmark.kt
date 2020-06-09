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
open class PlusAssignOperatorDesugaringBenchmark : AbstractInferenceBenchmark() {
    @Param("9", "10", "11", "12", "13", "14")
    private var size: Int = 0

    @Benchmark
    fun benchmark(bh: Blackhole) {
        analyzeGreenFile(bh)
    }

    override fun buildText(): String = buildString {
        appendLine(
            """
            class A {
                operator fun <T : Number> plus(other: (Int) -> T): A = this
                operator fun <T : CharSequence> plusAssign(other: (String) -> T) {}
            }
        """.trimIndent()
        )
        appendLine("fun test() {")
        appendLine("var a = A()")
        for (i in 1..size) {
            appendLine("a += {")
        }
        for (i in 1..size) {
            appendLine(
                """
                it.inc()
                1
                }
            """.trimIndent()
            )
        }
        appendLine()
    }
}