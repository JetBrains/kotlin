/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report

import org.jetbrains.kotlin.build.report.metrics.*

open class BuildReporter<B : BuildTime, P : BuildPerformanceMetric>(
    protected open val icReporter: ICReporter,
    protected open val buildMetricsReporter: BuildMetricsReporter<B, P>,
) : ICReporter by icReporter, BuildMetricsReporter<B, P> by buildMetricsReporter

class RemoteBuildReporter<B : BuildTime, P : BuildPerformanceMetric>(
    override val icReporter: RemoteICReporter,
    override val buildMetricsReporter: RemoteBuildMetricsReporter<B, P>,
) : BuildReporter<B, P>(icReporter, buildMetricsReporter), RemoteReporter {
    override fun flush() {
        icReporter.flush()
        buildMetricsReporter.flush()
    }
}

object DoNothingBuildReporter :
    BuildReporter<GradleBuildTime, GradleBuildPerformanceMetric>(DoNothingICReporter, DoNothingBuildMetricsReporter)
