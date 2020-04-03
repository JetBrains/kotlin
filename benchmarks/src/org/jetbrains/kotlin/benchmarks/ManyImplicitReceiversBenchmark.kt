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
open class ManyImplicitReceiversBenchmark : AbstractSimpleFileBenchmark() {
    @Param("1", "10", "50")
    private var size: Int = 0

    @Benchmark
    fun benchmark(bh: Blackhole) {
        analyzeGreenFile(bh)
    }

    override fun buildText(): String {
        return buildString {
            appendln("inline fun <T, R> with(receiver: T, block: T.() -> R): R = block()")

            for (i in 1..size) {
                appendln("interface A$i {")
                appendln("    fun foo$i()")
                appendln("}")
                appendln()
            }
            appendln()
            append("fun test(")
            append((1..size).joinToString(", ") { "a$it: A$it" })
            appendln(" {")
            for (i in 1..size) {
                appendln("with(a$i) {")
            }
            for (i in 1..size) {
                appendln("foo$i()")
            }
            for (i in 1..size) {
                appendln("}")
            }
            appendln("}")
        }
    }
}