/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report

import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.RemoteBuildMetricsReporter

open class BuildReporter(
    protected open val icReporter: ICReporter,
    protected open val buildMetricsReporter: BuildMetricsReporter
) : ICReporter by icReporter, BuildMetricsReporter by buildMetricsReporter

class RemoteBuildReporter(
    override val icReporter: RemoteICReporter,
    override val buildMetricsReporter: RemoteBuildMetricsReporter
) : BuildReporter(icReporter, buildMetricsReporter), RemoteReporter {
    override fun flush() {
        icReporter.flush()
        buildMetricsReporter.flush()
    }
}
