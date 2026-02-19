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
     * Callback for when the build operation reports a metric.
     *
     * Note the build operation may report the same metric multiple times if the same measured action repeats multiple times.
     * In this case, the reported value should be the sum of all reported values for that metric.
     *
     * The metric name represents a hierarchical structure, e.g. "Run compilation -> Shrink and save current classpath snapshot after compilation -> Save shrunk current classpath snapshot"
     *
     * @param name the name for the reported metric
     * @param type what the metric represents (the unit)
     * @param value the value reported in units denoted by [type]
     */
    public fun collectMetric(name: String, type: ValueType, value: Long)
}