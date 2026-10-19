/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(InternalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.trackers.BuildMetricsCollector
import org.jetbrains.kotlin.buildtools.internal.serializability.Message
import org.jetbrains.kotlin.buildtools.internal.serializability.MessageVisitor

internal class MetricsCollectorVisitor(private val metricsCollector: BuildMetricsCollector) : MessageVisitor {

    override fun accept(message: Message): Boolean = when (message) {
        is Message.CollectMetricMessage -> {
            metricsCollector.collectMetric(message.name, message.type, message.value)
            true
        }
        // else -> false // when other message types are added
    }
}
