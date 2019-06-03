/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.benchmark

import org.jetbrains.kotlin.fir.lightTree.benchmark.generators.TreeGenerator
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@Warmup(iterations = 10, batchSize = 1)
@Measurement(iterations = 10, batchSize = 1)
@State(Scope.Benchmark)
abstract class AbstractBenchmarkForGivenPath(
    private val path: String,
    private val ignoreTestData: Boolean = true
) : AbstractBenchmark() {
    abstract val generator: TreeGenerator

    @Setup
    fun setUp() {
        generator.setUp()
        readFiles(ignoreTestData, path)
        println("FILES COUNT: ${getFilesCount()}")
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