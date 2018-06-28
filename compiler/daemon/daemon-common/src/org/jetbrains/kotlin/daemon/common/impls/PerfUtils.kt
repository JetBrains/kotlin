/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.impls

import java.lang.management.ManagementFactory
import java.lang.management.ThreadMXBean
import java.util.concurrent.atomic.AtomicLong

interface PerfCounters {
    val count: Long
    val time: Long
    val threadTime: Long
    val threadUserTime: Long
    val memory: Long

    fun addMeasurement(time: Long = 0, thread: Long = 0, threadUser: Long = 0, memory: Long = 0)
}

interface Profiler {
    fun getCounters(): Map<Any?, PerfCounters>
    fun getTotalCounters(): PerfCounters

    fun<R> withMeasure(obj: Any?, body: () -> R): R
}


open class SimplePerfCounters : PerfCounters {
    private val _count: AtomicLong = AtomicLong(0L)
    private val _time: AtomicLong = AtomicLong(0L)
    private val _threadTime: AtomicLong = AtomicLong(0L)
    private val _threadUserTime: AtomicLong = AtomicLong(0L)
    private val _memory: AtomicLong = AtomicLong(0L)

    override val count: Long get() = _count.get()
    override val time: Long get() = _time.get()
    override val threadTime: Long get() = _threadTime.get()
    override val threadUserTime: Long get() = _threadUserTime.get()
    override val memory: Long get() = _memory.get()

    override fun addMeasurement(time: Long, thread: Long, threadUser: Long, memory: Long) {
        _count.incrementAndGet()
        _time.addAndGet(time)
        _threadTime.addAndGet(thread)
        _threadUserTime.addAndGet(threadUser)
        _memory.addAndGet(memory)
    }
}


class SimplePerfCountersWithTotal(val totalRef: PerfCounters) : SimplePerfCounters() {
    override fun addMeasurement(time: Long, thread: Long, threadUser: Long, memory: Long) {
        super.addMeasurement(time, thread, threadUser, memory)
        totalRef.addMeasurement(time, thread, threadUser, memory)
    }
}


@Suppress("NOTHING_TO_INLINE")
inline fun ThreadMXBean.threadCpuTime() = if (isCurrentThreadCpuTimeSupported) currentThreadCpuTime else 0L

@Suppress("NOTHING_TO_INLINE")
inline fun ThreadMXBean.threadUserTime() = if (isCurrentThreadCpuTimeSupported) currentThreadUserTime else 0L

@Suppress("NOTHING_TO_INLINE")
inline fun usedMemory(withGC: Boolean): Long {
    if (withGC) {
        System.gc()
    }
    val rt = Runtime.getRuntime()
    return (rt.totalMemory() - rt.freeMemory())
}


inline fun<R> withMeasureWallTime(perfCounters: PerfCounters, body: () -> R): R {
    val startTime = System.nanoTime()
    val res = body()
    perfCounters.addMeasurement(time = System.nanoTime() - startTime) // TODO: add support for time wrapping
    return res
}


inline fun<R> withMeasureWallAndThreadTimes(perfCounters: PerfCounters, threadMXBean: ThreadMXBean, body: () -> R): R {
    val startTime = System.nanoTime()
    val startThreadTime = threadMXBean.threadCpuTime()
    val startThreadUserTime = threadMXBean.threadUserTime()

    val res = body()

    // TODO: add support for time wrapping
    perfCounters.addMeasurement(time = System.nanoTime() - startTime,
                                thread = threadMXBean.threadCpuTime() - startThreadTime,
                                threadUser = threadMXBean.threadUserTime() - startThreadUserTime)
    return res
}

inline fun<R> withMeasureWallAndThreadTimes(perfCounters: PerfCounters, body: () -> R): R =
    withMeasureWallAndThreadTimes(perfCounters, ManagementFactory.getThreadMXBean(), body)


inline fun<R> withMeasureWallAndThreadTimesAndMemory(perfCounters: PerfCounters, withGC: Boolean = false, threadMXBean: ThreadMXBean, body: () -> R): R {
    val startMem = usedMemory(withGC)
    val startTime = System.nanoTime()
    val startThreadTime = threadMXBean.threadCpuTime()
    val startThreadUserTime = threadMXBean.threadUserTime()

    val res = body()

    // TODO: add support for time wrapping
    perfCounters.addMeasurement(time = System.nanoTime() - startTime,
                                thread = threadMXBean.threadCpuTime() - startThreadTime,
                                threadUser = threadMXBean.threadUserTime() - startThreadUserTime,
                                memory = usedMemory(withGC) - startMem)
    return res
}

inline fun<R> withMeasureWallAndThreadTimesAndMemory(perfCounters: PerfCounters, withGC: Boolean, body: () -> R): R =
    withMeasureWallAndThreadTimesAndMemory(
        perfCounters,
        withGC,
        ManagementFactory.getThreadMXBean(),
        body
    )


class DummyProfiler : Profiler {
    override fun getCounters(): Map<Any?, PerfCounters> = mapOf(null to SimplePerfCounters())
    override fun getTotalCounters(): PerfCounters =
        SimplePerfCounters()

    override final inline fun <R> withMeasure(obj: Any?, body: () -> R): R = body()
}


abstract class TotalProfiler : Profiler {

    val total = SimplePerfCounters()
    val threadMXBean = ManagementFactory.getThreadMXBean()

    override fun getCounters(): Map<Any?, PerfCounters> = mapOf()
    override fun getTotalCounters(): PerfCounters = total
}


class WallTotalProfiler : TotalProfiler() {
    override final inline fun <R> withMeasure(obj: Any?, body: () -> R): R =
        withMeasureWallTime(total, body)
}


class WallAndThreadTotalProfiler : TotalProfiler() {
    override final inline fun <R> withMeasure(obj: Any?, body: () -> R): R =
        withMeasureWallAndThreadTimes(total, threadMXBean, body)
}


class WallAndThreadAndMemoryTotalProfiler(val withGC: Boolean) : TotalProfiler() {
    override final inline fun <R> withMeasure(obj: Any?, body: () -> R): R =
        withMeasureWallAndThreadTimesAndMemory(total, withGC, threadMXBean, body)
}


class WallAndThreadByClassProfiler() : TotalProfiler() {

    val counters = hashMapOf<Any?, SimplePerfCountersWithTotal>()

    override fun getCounters(): Map<Any?, PerfCounters> = counters

    override final inline fun <R> withMeasure(obj: Any?, body: () -> R): R =
        withMeasureWallAndThreadTimes(
            counters.getOrPut(
                obj?.javaClass?.name,
                {
                    SimplePerfCountersWithTotal(
                        total
                    )
                }), threadMXBean, body
        )
}
