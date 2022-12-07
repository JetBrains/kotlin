/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.utils

import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter

class TestBuildReporter(
    val testICReporter: TestICReporter,
    buildMetricsReporter: BuildMetricsReporter
) : BuildReporter(testICReporter, buildMetricsReporter) {
    fun reportCachesDump(cachesDump: String) {
        testICReporter.cachesDump = cachesDump
    }
}