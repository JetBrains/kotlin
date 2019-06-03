/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.benchmark

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.CharsetToolkit
import org.jetbrains.kotlin.fir.lightTree.benchmark.generators.TreeGenerator
import org.openjdk.jmh.annotations.*
import java.io.File
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.SingleShotTime)
@Warmup(iterations = 10, batchSize = 1)
@Measurement(iterations = 10, batchSize = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Benchmark)
abstract class AbstractBenchmark {
    private val files = mutableMapOf<String, Pair<File, String>>()
    abstract val generator: TreeGenerator

    protected fun readFiles(ignoreTestData: Boolean, path: String) {
        val root = File(path)

        println("BASE PATH: $path")
        for (file in root.walkTopDown()) {
            if (file.isDirectory) continue
            if (ignoreTestData && (file.path.contains("testData") || file.path.contains("resources"))) continue
            if (file.extension != "kt") continue

            val text = FileUtil.loadFile(file, CharsetToolkit.UTF8, true).trim()
            files[file.name] = Pair(file, text)
        }
    }

    //TODO null check
    protected fun getFileByName(fileName: String): Pair<File, String> {
        return files[fileName]!!
    }

    protected fun forEachFile(f: (String, File) -> Unit) {
        for ((file, text) in files.values) {
            f(text, file)
        }
    }

    protected fun getFilesCount(): Int {
        return files.size
    }
}