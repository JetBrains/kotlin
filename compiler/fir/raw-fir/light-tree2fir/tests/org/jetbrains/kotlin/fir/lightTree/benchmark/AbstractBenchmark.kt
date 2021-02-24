/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.benchmark

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.CharsetToolkit
import org.jetbrains.kotlin.fir.lightTree.benchmark.generators.TreeGenerator
import org.jetbrains.kotlin.fir.lightTree.walkTopDown
import org.jetbrains.kotlin.fir.lightTree.walkTopDownWithTestData
import org.openjdk.jmh.annotations.*
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * This class is base for all benchmarks.
 *
 * To run benchmarks use gradle and consistently run next tasks:
 * 1. jmhBytecode
 * 2. jmhExec
 */
@BenchmarkMode(Mode.SingleShotTime)
@Warmup(iterations = 10, batchSize = 1)
@Measurement(iterations = 10, batchSize = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Benchmark)
abstract class AbstractBenchmark {
    private val files = mutableMapOf<File, String>()
    abstract val generator: TreeGenerator

    @Param("true", "false")
    var stubMode: Boolean = false

    protected fun readFiles(ignoreTestData: Boolean, path: String) {
        println("BASE PATH: $path")
        val saveText: (File) -> Unit = { file ->
            val text = FileUtil.loadFile(file, CharsetToolkit.UTF8, true).trim()
            files[file] = text
        }
        if (ignoreTestData) {
            path.walkTopDown(saveText)
        } else {
            path.walkTopDownWithTestData(saveText)
        }
    }

    protected fun forEachFile(f: (String, File) -> Unit) {
        for ((file, text) in files) {
            f(text, file)
        }
    }

    protected fun getFilesCount(): Int {
        return files.size
    }
}