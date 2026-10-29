// ISSUE: KT-90675
// TARGET_BACKEND: NATIVE
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: NATIVE:2.3,2.4
// DISABLE_NATIVE: gcType=NOOP
// DISABLE_NATIVE: optimizationMode=OPT
// ^^^ Optimizations might spoil the precise allocation measurement, so it's safer to perform testing only in non-opt mode
// FREE_COMPILER_ARGS: -Xbinary=gcSchedulerType=manual
// KIND: STANDALONE
// ^^^ The test must run in its own binary, so no parallel threads would allocate during the measurement.
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class, kotlin.ExperimentalStdlibApi::class)

import kotlin.native.runtime.GC

value class Id(val value: Int)

class Holder(val id: Id)

fun countEqual(holder: Holder, other: Id, n: Int): Int {
    var hits = 0
    repeat(n) { if (holder.id == other) hits++ }
    return hits
}

fun heapBytesAllocatedBy(comparisons: () -> Int): Long {
    GC.collect()
    val start = GC.lastGCInfo!!.memoryUsageAfter.values.sumOf { it.totalObjectsSizeBytes }
    comparisons()
    GC.collect()
    return GC.lastGCInfo!!.memoryUsageBefore.values.sumOf { it.totalObjectsSizeBytes } - start
}

fun box(): String {
    val holder = Holder(Id(100_000))
    heapBytesAllocatedBy { countEqual(holder, Id(200_000), 10_000) }
    val bytes = heapBytesAllocatedBy { countEqual(holder, Id(200_000), 100_000) }
    // Boxing both sides of `==` allocates ~4.6 MiB here (200_000 boxes of 24 bytes: object header, GC word, Int).
    // The custom allocator reports heap usage with page granularity (128/256 KiB), so the metric moves by zero or by
    // whole pages; the warm-up call above pre-populates the pages that `GC.lastGCInfo` itself allocates from, so they
    // cancel out. The legacy allocator counts object bytes instead, hence the 64 KiB slack.
    // In OPT mode escape analysis may stack-allocate the boxes, so the regression is reliably detected in DEBUG mode.
    return if (bytes < 64 * 1024) "OK" else "Id == Id allocated ${bytes / 1024} KiB in 100000 comparisons"
}
