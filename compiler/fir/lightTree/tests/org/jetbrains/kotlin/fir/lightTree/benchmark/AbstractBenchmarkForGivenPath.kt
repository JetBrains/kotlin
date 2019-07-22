/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.benchmark

import org.openjdk.jmh.annotations.*

abstract class AbstractBenchmarkForGivenPath(
    private val path: String,
    private val ignoreTestData: Boolean = true
) : AbstractBenchmark() {
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
    fun testBuildOnlyBaseTreeForGivenPath() {
        forEachFile { text, file -> generator.generateBaseTree(text, file) }
    }

    @Benchmark
    fun testBuildFirForGivenPath() {
        forEachFile { text, file -> generator.generateFir(text, file, stubMode) }
    }
}