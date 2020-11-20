/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.daemon.common.threadCpuTime
import org.jetbrains.kotlin.daemon.common.threadUserTime
import sun.management.ManagementFactoryHelper

data class GCInfo(val name: String, val gcTime: Long, val collections: Long) {
    operator fun minus(other: GCInfo): GCInfo {
        return this.copy(
            gcTime = gcTime - other.gcTime,
            collections = collections - other.collections
        )
    }
}

data class VMCounters(val userTime: Long, val cpuTime: Long, val gcInfo: Map<String, GCInfo>) {


    operator fun minus(other: VMCounters): VMCounters {
        return VMCounters(
            userTime - other.userTime,
            cpuTime - other.cpuTime,
            merge(gcInfo, other.gcInfo) { a, b -> a - b }
        )
    }

}


private fun <K, V> merge(first: Map<K, V>, second: Map<K, V>, valueOp: (V, V) -> V): Map<K, V> {
    val result = first.toMutableMap()
    for ((k, v) in second) {
        @Suppress("NULLABLE_TYPE_PARAMETER_AGAINST_NOT_NULL_TYPE_PARAMETER") // KT-43225
        result.merge(k, v, valueOp)
    }
    return result
}

object Init {
    init {
        ManagementFactoryHelper.getThreadMXBean().isThreadCpuTimeEnabled = true
    }
}

fun vmStateSnapshot(): VMCounters {
    Init
    val threadMXBean = ManagementFactoryHelper.getThreadMXBean()

    return VMCounters(
        threadMXBean.threadUserTime(), threadMXBean.threadCpuTime(),
        ManagementFactoryHelper.getGarbageCollectorMXBeans().associate { it.name to GCInfo(it.name, it.collectionTime, it.collectionCount) }
    )
}
