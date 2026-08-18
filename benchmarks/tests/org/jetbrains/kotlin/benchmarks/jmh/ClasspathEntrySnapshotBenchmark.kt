/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks.jmh

import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshot
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotter
import org.openjdk.jmh.annotations.*
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
open class ClasspathEntrySnapshotBenchmark {
    @Param("100", "1000", "10000")
    private var classCount: Int = 0

    private lateinit var temporaryDirectory: File
    private lateinit var classesDirectory: File
    private lateinit var classesJar: File

    @Setup(Level.Trial)
    fun setUp() {
        temporaryDirectory = Files.createTempDirectory("classpath-entry-snapshot-benchmark").toFile()
        classesDirectory = temporaryDirectory.resolve("classes").apply { mkdir() }
        classesJar = temporaryDirectory.resolve("classes.jar")

        val classBytes = checkNotNull(
            ClasspathSnapshotBenchmarkInput::class.java.getResourceAsStream("ClasspathSnapshotBenchmarkInput.class")
        ).use { it.readBytes() }

        ZipOutputStream(classesJar.outputStream().buffered()).use { output ->
            repeat(classCount) { index ->
                val relativePath = "benchmark/Input${classCount - index}.class"
                classesDirectory.resolve(relativePath).apply {
                    parentFile.mkdirs()
                    writeBytes(classBytes)
                }
                output.putNextEntry(ZipEntry(relativePath))
                output.write(classBytes)
                output.closeEntry()
            }
        }
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        temporaryDirectory.deleteRecursively()
    }

    @Benchmark
    fun snapshotDirectory(): ClasspathEntrySnapshot = ClasspathEntrySnapshotter.snapshot(classesDirectory, SETTINGS)

    @Benchmark
    fun snapshotJar(): ClasspathEntrySnapshot = ClasspathEntrySnapshotter.snapshot(classesJar, SETTINGS)

    private companion object {
        val SETTINGS = ClasspathEntrySnapshotter.Settings(
            granularity = ClassSnapshotGranularity.CLASS_LEVEL,
            parseInlinedLocalClasses = false,
            expandTypeAliases = false,
        )
    }
}

private class ClasspathSnapshotBenchmarkInput