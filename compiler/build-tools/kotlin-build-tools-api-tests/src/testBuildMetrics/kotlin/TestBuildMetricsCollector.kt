/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests

import org.jetbrains.kotlin.buildtools.api.trackers.BuildMetricsCollector
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder

/**
 * Simple thread-safe implementation of [BuildMetricsCollector] intended for tests.
 * It stores all reported metrics in memory so that test code can assert on them later.
 */
class TestBuildMetricsCollector : BuildMetricsCollector {
    data class Entry(
        val name: String,
        val type: BuildMetricsCollector.ValueType,
        val value: Long,
    )

    private data class Key(val name: String, val type: BuildMetricsCollector.ValueType)

    private val counters = ConcurrentHashMap<Key, LongAdder>()

    override fun collectMetric(
        name: String,
        type: BuildMetricsCollector.ValueType,
        value: Long,
    ) {
        counters.computeIfAbsent(Key(name, type)) { LongAdder() }.add(value)
    }

    fun all(): List<Entry> = counters.entries
        .map { (key, adder) -> Entry(key.name, key.type, adder.sum()) }
        // Provide deterministic ordering for stable test assertions.
        .sortedWith(compareBy<Entry> { it.name }.thenBy { it.type.toString() })
}