/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.daemon.common.threadCpuTime
import org.jetbrains.kotlin.daemon.common.threadUserTime
import java.lang.management.ManagementFactory
import javax.management.ObjectName

data class GCInfo(val name: String, val gcTime: Long, val collections: Long) {
    operator fun minus(other: GCInfo): GCInfo {
        return this.copy(
            gcTime = gcTime - other.gcTime,
            collections = collections - other.collections
        )
    }

    operator fun plus(other: GCInfo): GCInfo {
        return this.copy(
            gcTime = gcTime + other.gcTime,
            collections = collections + other.collections
        )
    }
}

data class VMCounters(
    val userTime: Long = 0,
    val cpuTime: Long = 0,
    val gcInfo: Map<String, GCInfo> = emptyMap(),

    val safePointTotalTime: Long = 0,
    val safePointSyncTime: Long = 0,
    val safePointCount: Long = 0,
) {


    operator fun minus(other: VMCounters): VMCounters {
        return VMCounters(
            userTime - other.userTime,
            cpuTime - other.cpuTime,
            merge(gcInfo, other.gcInfo) { a, b -> a - b },
            safePointTotalTime - other.safePointTotalTime,
            safePointSyncTime - other.safePointSyncTime,
            safePointCount - other.safePointCount
        )
    }


    operator fun plus(other: VMCounters): VMCounters {
        return VMCounters(
            userTime + other.userTime,
            cpuTime + other.cpuTime,
            merge(gcInfo, other.gcInfo) { a, b -> a + b },
            safePointTotalTime + other.safePointTotalTime,
            safePointSyncTime + other.safePointSyncTime,
            safePointCount + other.safePointCount
        )
    }
}


private fun <K, V : Any> merge(first: Map<K, V>, second: Map<K, V>, valueOp: (V, V) -> V): Map<K, V> {
    val result = first.toMutableMap()
    for ([k, v] in second) {
        result.merge(k, v, valueOp)
    }
    return result
}

object Init {
    init {
        ManagementFactory.getThreadMXBean().isThreadCpuTimeEnabled = true
    }
}

/**
 * Safepoint statistics are only published by HotSpot's own `HotspotRuntime` MBean, which is registered on
 * demand and whose interface is not exported from `java.management`. It is therefore reached through JMX by
 * name, and reported as zeroes whenever that fails — for example on a JDK 9+ runtime that does not pass
 * `--add-exports java.management/sun.management=ALL-UNNAMED`.
 */
private val hotspotRuntimeMBeanName: ObjectName? by lazy {
    runCatching {
        val server = ManagementFactory.getPlatformMBeanServer()
        val name = ObjectName("sun.management:type=HotspotRuntime")
        if (!server.isRegistered(name)) {
            server.createMBean("sun.management.HotspotInternal", null)
        }
        name.takeIf { server.isRegistered(it) }
    }.getOrNull()
}

private fun safepointCounter(attribute: String): Long {
    val name = hotspotRuntimeMBeanName ?: return 0
    return runCatching {
        ManagementFactory.getPlatformMBeanServer().getAttribute(name, attribute) as Long
    }.getOrDefault(0)
}

fun vmStateSnapshot(): VMCounters {
    @Suppress("UNUSED_EXPRESSION") Init
    val threadMXBean = ManagementFactory.getThreadMXBean()

    return VMCounters(
        threadMXBean.threadUserTime(), threadMXBean.threadCpuTime(),
        ManagementFactory.getGarbageCollectorMXBeans().associate { it.name to GCInfo(it.name, it.collectionTime, it.collectionCount) },
        safepointCounter("TotalSafepointTime"),
        safepointCounter("SafepointSyncTime"),
        safepointCounter("SafepointCount")
    )
}
