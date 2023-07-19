/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import org.jetbrains.kotlin.build.report.RemoteReporter

interface RemoteBuildMetricsReporter<B : BuildTime, P : BuildPerformanceMetric> : BuildMetricsReporter<B, P>, RemoteReporter