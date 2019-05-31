/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.benchmark.totalKotlin

import org.jetbrains.kotlin.fir.lightTree.benchmark.*
import org.openjdk.jmh.annotations.*

@State(Scope.Benchmark)
abstract class AbstractTotalKotlinBenchmark : AbstractBenchmark() {
    abstract val generator: TreeGenerator

    @Setup
    fun setUp() {
        generator.setUp()
        readFiles(true, System.getProperty("user.dir"))
    }

    @TearDown
    fun tearDown() {
        generator.tearDown()
    }

    @Benchmark
    fun testTotalKotlinOnlyBaseTree() {
        forEachFile { text, file -> generator.generateBaseTree(text, file) }
    }

    @Benchmark
    fun testTotalKotlinFir() {
        forEachFile { text, file -> generator.generateFir(text, file) }
    }
}

open class LightTree2FirTotalKotlinBenchmark(override val generator: TreeGenerator = LightTree2FirGenerator()) :
    AbstractTotalKotlinBenchmark()

open class Psi2FirTotalKotlinBenchmark(override val generator: TreeGenerator = Psi2FirGenerator()) :
    AbstractTotalKotlinBenchmark()