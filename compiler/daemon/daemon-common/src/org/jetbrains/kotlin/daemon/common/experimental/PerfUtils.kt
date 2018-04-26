/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.daemon.common.*
import java.lang.management.ManagementFactory
import java.lang.management.ThreadMXBean

interface Profiler {
    fun getCounters(): Map<Any?, PerfCounters>
    fun getTotalCounters(): PerfCounters

    suspend fun <R> withMeasure(obj: Any?, body: suspend () -> R): R
}

class DummyProfiler : Profiler {
    override fun getCounters(): Map<Any?, PerfCounters> = mapOf(null to SimplePerfCounters())
    override fun getTotalCounters(): PerfCounters = SimplePerfCounters()

    override suspend fun <R> withMeasure(obj: Any?, body: suspend () -> R): R = body()
}

abstract class TotalProfiler : Profiler {

    val total = SimplePerfCounters()
    val threadMXBean = ManagementFactory.getThreadMXBean()

    override fun getCounters(): Map<Any?, PerfCounters> = mapOf()
    override fun getTotalCounters(): PerfCounters = total
}

suspend fun <R> withMeasureWallAndThreadTimesAndMemory(
    perfCounters: PerfCounters,
    withGC: Boolean = false,
    threadMXBean: ThreadMXBean,
    body: suspend () -> R
): R {
    val startMem = usedMemory(withGC)
    val startTime = System.nanoTime()
    val startThreadTime = threadMXBean.threadCpuTime()
    val startThreadUserTime = threadMXBean.threadUserTime()

    val res = body()

    // TODO: add support for time wrapping
    perfCounters.addMeasurement(
        time = System.nanoTime() - startTime,
        thread = threadMXBean.threadCpuTime() - startThreadTime,
        threadUser = threadMXBean.threadUserTime() - startThreadUserTime,
        memory = usedMemory(withGC) - startMem
    )
    return res
}

suspend fun <R> withMeasureWallAndThreadTimes(
    perfCounters: PerfCounters,
    threadMXBean: ThreadMXBean,
    body: suspend () -> R
): R {
    val startTime = System.nanoTime()
    val startThreadTime = threadMXBean.threadCpuTime()
    val startThreadUserTime = threadMXBean.threadUserTime()

    val res = body()

    // TODO: add support for time wrapping
    perfCounters.addMeasurement(
        time = System.nanoTime() - startTime,
        thread = threadMXBean.threadCpuTime() - startThreadTime,
        threadUser = threadMXBean.threadUserTime() - startThreadUserTime
    )
    return res
}

class WallAndThreadTotalProfiler : TotalProfiler() {
    override suspend fun <R> withMeasure(obj: Any?, body: suspend () -> R): R = withMeasureWallAndThreadTimes(
        total,
        threadMXBean,
        body
    )
}


class WallAndThreadAndMemoryTotalProfiler(val withGC: Boolean) : TotalProfiler() {
    override suspend fun <R> withMeasure(obj: Any?, body: suspend () -> R): R =
        withMeasureWallAndThreadTimesAndMemory(total, withGC, threadMXBean, body)
}


fun <R> Profiler.withMeasureBlocking(obj: Any?, body: suspend () -> R): R = runBlocking {
    this@withMeasureBlocking.withMeasure<R>(obj, body)
}