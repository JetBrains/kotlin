/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit
import java.io.File
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotter
import org.jetbrains.kotlin.build.report.metrics.*

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
open class ClasspathEntrySnapshotterBenchmark {

    private lateinit var jarFile: File

    var metricsReporter = BuildMetricsReporterImpl<GradleBuildTime, GradleBuildPerformanceMetric>()
    //private val buildTimes = BuildTimes<GradleBuildTime>()

    @Setup(Level.Trial)
    fun setUp() {
        jarFile = File("/home/Evgenii.Mazhukin/benchmarking-snapshotting/kotlin-stdlib-1.9.21.jar")
        require(jarFile.exists()) { "Test jar file not found at ${jarFile.absolutePath}" }

        metricsReporter = BuildMetricsReporterImpl<GradleBuildTime, GradleBuildPerformanceMetric>()
    }

    @Benchmark
    fun benchmarkSnapshot(bh: Blackhole) {
        val settings = ClasspathEntrySnapshotter.Settings(
            granularity = ClassSnapshotGranularity.CLASS_MEMBER_LEVEL,
            parseInlinedLocalClasses = false
        )

        val snapshot = ClasspathEntrySnapshotter.snapshot(jarFile, settings, metricsReporter)
        bh.consume(snapshot)
    }

    @Benchmark
    fun benchmarkNewSnapshot(bh: Blackhole) {
        val settings = ClasspathEntrySnapshotter.Settings(
            granularity = ClassSnapshotGranularity.CLASS_MEMBER_LEVEL,
            parseInlinedLocalClasses = true
        )

        val snapshot = ClasspathEntrySnapshotter.snapshot(jarFile, settings, metricsReporter)
        bh.consume(snapshot)
    }

    @TearDown
    fun tearDown() {
        println("Tearing down")
        println(metricsReporter.getMetrics().buildTimes.asMapMs().map { (a, b) ->
            "$a: $b"
        }.joinToString("\n"))
    }
}
