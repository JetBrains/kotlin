/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks.jmh

import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotter
import java.io.File

/** Coarse retained-heap check, not a timing benchmark. Run separately under an OS peak-RSS monitor. */
object ClasspathSnapshotMemory {
    @JvmStatic
    fun main(args: Array<String>) {
        val entry = File(args[0])
        val settings = ClasspathEntrySnapshotter.Settings(ClassSnapshotGranularity.valueOf(args[1]), true, false)
        repeat(5) { ClasspathEntrySnapshotter.snapshot(entry, settings) }
        val runtime = Runtime.getRuntime()
        System.gc()
        val before = runtime.totalMemory() - runtime.freeMemory()
        val snapshots = List(10) { ClasspathEntrySnapshotter.snapshot(entry, settings) }
        System.gc()
        val after = runtime.totalMemory() - runtime.freeMemory()
        println("retainedBytesPerSnapshot=${(after - before) / snapshots.size}")
        println("classCount=${snapshots.sumOf { it.classSnapshots.size }}")
    }
}
