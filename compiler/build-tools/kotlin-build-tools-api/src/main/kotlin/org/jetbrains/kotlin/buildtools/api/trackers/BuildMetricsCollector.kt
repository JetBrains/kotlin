/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.trackers

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * A collector for various metrics from the compilation process.
 *
 * @since 2.3.0
 */
@ExperimentalBuildToolsApi
public interface BuildMetricsCollector {
    public enum class ValueType {
        BYTES,
        NUMBER,
        NANOSECONDS,
        MILLISECONDS,
        TIME,
        ATTRIBUTE,
    }

    /**
     * Callback for when the compiler reports a metric.
     *
     * @param name the name for the reported metric
     * @param type what the metric represents (the unit)
     * @param value the value reported in units denoted by [type]
     */
    public fun collectMetric(name: String, type: ValueType, value: Long)
}