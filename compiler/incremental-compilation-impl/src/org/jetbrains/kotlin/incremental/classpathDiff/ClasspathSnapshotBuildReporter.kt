/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.build.report.debug
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime

class ClasspathSnapshotBuildReporter(private val buildReporter: BuildReporter<GradleBuildTime, GradleBuildPerformanceMetric>) :
    ICReporter by buildReporter, BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric> by buildReporter {

    override fun report(message: () -> String, severity: ICReporter.ReportSeverity) {
        buildReporter.report({ "[ClasspathSnapshot] ${message()}" }, severity)
    }

    fun reportVerboseWithLimit(maxLength: Int = 1000, message: () -> String) {
        debug {
            message().let {
                if (it.length > maxLength) {
                    it.substring(0, maxLength) + "... (string too long, showing $maxLength / ${it.length} chars)"
                } else it
            }
        }
    }
}
