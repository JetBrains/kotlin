/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks.jmh

import com.intellij.util.io.ZipUtil
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshot
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotter
import org.openjdk.jmh.annotations.*
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

@Suppress("unused")
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
open class StdlibClasspathEntrySnapshotBenchmark {
    @Param("CLASS_LEVEL", "CLASS_MEMBER_LEVEL")
    private lateinit var granularity: ClassSnapshotGranularity

    private lateinit var stdlibJar: File
    private lateinit var settings: ClasspathEntrySnapshotter.Settings
    private lateinit var stdlibClassesDir: File

    @Setup
    fun setUp() {
        stdlibJar = File(checkNotNull(System.getProperty("classpathSnapshot.stdlib")))
        check(stdlibJar.isFile)

        stdlibClassesDir = Files.createTempDirectory("classpathSnapshot.benchmark").toFile()
        ZipUtil.extract(stdlibJar.toPath(), stdlibClassesDir.toPath(), null, true)

        settings = ClasspathEntrySnapshotter.Settings(granularity, parseInlinedLocalClasses = true, expandTypeAliases = false)
    }

    @Benchmark
    fun snapshotJar(): ClasspathEntrySnapshot = ClasspathEntrySnapshotter.snapshot(stdlibJar, settings)

    @Benchmark
    fun snapshotDirectory(): ClasspathEntrySnapshot = ClasspathEntrySnapshotter.snapshot(stdlibClassesDir, settings)
}
