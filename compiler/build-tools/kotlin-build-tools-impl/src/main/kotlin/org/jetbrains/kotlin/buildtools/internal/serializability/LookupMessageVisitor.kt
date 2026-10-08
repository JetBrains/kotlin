/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.trackers.BuildMetricsCollector
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.internal.serializability.Message
import org.jetbrains.kotlin.buildtools.internal.serializability.afterSerialization

internal class LookupMessageVisitor(private val lookupTracker: CompilerLookupTracker) : MessageVisitor {

    override fun accept(message: Message): Boolean = when (message) {
        is Message.LookupMessage -> {
            lookupTracker.recordLookup(message.filePath, message.scopeFqName, message.scopeKind, message.name)
            true
        }
        is Message.LookupClear -> {
            lookupTracker.clear()
            true
        }
        else -> false
    }
}

internal class LogLineVisitor(private val logger: KotlinLogger) : MessageVisitor {

    override fun accept(message: Message): Boolean = when (message) {
        is Message.LogLine -> {
            when (message.level) {
                ERROR -> logger.error(message.logLine)
                WARN -> logger.warn(message.logLine)
                LIFECYCLE -> logger.lifecycle(message.logLine)
                INFO -> logger.info(message.logLine)
                DEBUG -> logger.debug(message.logLine)
            }
            true
        }
        else -> false
    }
}

internal class CompilerMessageVisitor(private val logger: KotlinLoggerMessageCollectorAdapter) : MessageVisitor {

    override fun accept(message: Message): Boolean = when (message) {
        is Message.CompilerMessageWithDiagnosticId -> {
            logger.report(message.severity, message.message, message.location?.afterSerialization(), message.diagnosticId)
            true
        }
        is Message.CompilerMessageClear -> {
            logger.clear()
            true
        }
        else -> false
    }
}

internal class MetricsCollectorVisitor(private val metricsCollector: BuildMetricsCollector) : MessageVisitor {

    override fun accept(message: Message): Boolean = when (message) {
        is Message.CollectMetricMessage -> {
            metricsCollector.collectMetric(message.name, message.type, message.value)
            true
        }
        else -> false
    }
}
