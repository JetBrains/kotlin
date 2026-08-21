/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks.jmh

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

/**
 * Multi-feature FIR resolve microbenchmark shaped after kotlinconf-app frontend hotspots.
 *
 * Corpus intentionally stresses, in one synthetic file:
 * - **B1 call/candidate resolution**: overload sets, trailing-lambda "UI" fan-out
 * - **B2 inference/constraints**: generic `remember`/`map`-style calls and lambda arguments
 * - **B4 scopes/lookup**: nested `with` implicit receivers and extension calls
 *
 * Timed work is [FirTotalResolveProcessor.process] only (equivalent to `runResolution`).
 * PSI is built in trial setup; raw FIR is rebuilt each invocation in
 * [prepareFirForResolve] so it stays outside the timed method.
 *
 * Not covered: checkers (~⅓ of `resolveAndCheckFir`), FIR2IR/codegen, Compose/Metro
 * compiler plugins, or a real multi-module app classpath.
 *
 * Baseline command, env, scores, and coverage map:
 * [FirResolveHotspotsBenchmark.md](FirResolveHotspotsBenchmark.md).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
open class FirResolveHotspotsBenchmark : AbstractSimpleFileBenchmark() {

    /**
     * Number of repeated UI-like blocks in the synthetic file.
     * Keep moderate: each unit nests receivers, overloads, and generic lambdas.
     */
    @Param("10", "50", "100")
    private var size: Int = 0

    @Setup(Level.Invocation)
    fun prepareResolve() {
        prepareFirForResolve()
    }

    @Benchmark
    fun benchmark(bh: Blackhole) {
        processFirResolve(bh)
    }

    override fun buildText(): String = buildString {
        // Local stdlib-only stand-ins for Compose-like APIs (no Compose runtime on classpath).
        appendLine(
            """
            |inline fun Container(content: () -> Unit) { content() }
            |inline fun Container(modifier: Int, content: () -> Unit) { content() }
            |inline fun Row(content: () -> Unit) { content() }
            |inline fun Column(content: () -> Unit) { content() }
            |
            |fun Text(value: String) {}
            |fun Text(value: Int) {}
            |fun Text(value: String, style: Int) {}
            |fun Text(value: Int, style: Int) {}
            |
            |fun <T> remember(calculation: () -> T): T = calculation()
            |fun <T> mutableStateOf(value: T): T = value
            |fun <T, R> mapState(value: T, transform: (T) -> R): R = transform(value)
            |fun <T : Comparable<T>> maxOfTwo(a: T, b: T): T = if (a >= b) a else b
            |
            |fun item(value: String) {}
            |fun item(value: Int) {}
            |fun <T : Any> item(value: T, key: String) {}
            |fun item(value: Int, handler: (Int) -> Unit) {}
            |fun item(value: String, handler: (String) -> Unit) {}
            |
            |interface ThemeScope {
            |    fun color(name: String): Int
            |    fun typography(name: String): Int
            |}
            |interface DensityScope {
            |    fun dp(value: Int): Int
            |    fun sp(value: Int): Int
            |}
            |interface FocusScope {
            |    fun requestFocus()
            |    fun focused(): Boolean
            |}
            |
            |fun <R> ThemeScope.themed(block: ThemeScope.() -> R): R = block()
            |fun <R> DensityScope.withDensity(block: DensityScope.() -> R): R = block()
            |
            """.trimMargin()
        )

        appendLine("fun benchmarkBody(theme: ThemeScope, density: DensityScope, focus: FocusScope, labels: List<String>) {")
        for (i in 1..size) {
            appendLine(
                """
                |    Container {
                |        Column {
                |            val title$i = remember { mutableStateOf("title-$i") }
                |            val count$i = remember { mutableStateOf($i) }
                |            val mapped$i = mapState(count$i) { value -> value + 1 }
                |            val best$i = maxOfTwo(count$i, mapped$i)
                |            Text(title$i)
                |            Text(title$i, style = 1)
                |            Text(best$i)
                |            Text(best$i, style = 2)
                |            item(title$i)
                |            item(count$i)
                |            item(title$i, key = "k$i")
                |            item(count$i) { value -> Text(value) }
                |            item(title$i) { value -> Text(value, style = 0) }
                |            Row {
                |                Container(modifier = $i) {
                |                    with(theme) {
                |                        with(density) {
                |                            with(focus) {
                |                                val primary$i = color("primary")
                |                                val body$i = typography("body")
                |                                val pad$i = dp(count$i)
                |                                val font$i = sp(mapped$i)
                |                                Text(primary$i, style = body$i)
                |                                Text(pad$i, style = font$i)
                |                                if (focused()) {
                |                                    requestFocus()
                |                                }
                |                                themed {
                |                                    Text(color("accent"))
                |                                    item(color("surface"), key = "surface-$i")
                |                                }
                |                                withDensity {
                |                                    Text(dp(best$i))
                |                                    item(sp(best$i)) { value -> Text(value) }
                |                                }
                |                                labels.map { label ->
                |                                    item(label) { value ->
                |                                        Text(value)
                |                                        Text(maxOfTwo(value.length, count$i))
                |                                    }
                |                                }
                |                            }
                |                        }
                |                    }
                |                }
                |            }
                |        }
                |    }
                """.trimMargin()
            )
        }
        appendLine("}")
    }
}
