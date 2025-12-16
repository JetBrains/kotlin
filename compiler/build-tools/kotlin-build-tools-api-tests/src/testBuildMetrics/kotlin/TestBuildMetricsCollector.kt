/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests

import org.jetbrains.kotlin.buildtools.api.trackers.BuildMetricsCollector
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

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

    private val _entries = CopyOnWriteArrayList<Entry>()

    override fun collectMetric(
        name: String,
        type: BuildMetricsCollector.ValueType,
        value: Long,
    ) {
        _entries += Entry(
            name = name,
            type = type,
            value = value,
        )
    }

    fun all(): List<Entry> = _entries.toList()
}