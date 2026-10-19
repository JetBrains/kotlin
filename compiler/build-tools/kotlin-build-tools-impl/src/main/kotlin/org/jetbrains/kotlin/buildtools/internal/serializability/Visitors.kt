/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.serializability

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.trackers.BuildMetricsCollector
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.internal.KotlinLoggerMessageCollectorAdapter
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream

internal class LogLineVisitor(private val logger: KotlinLogger) : MessageVisitor {

    override fun accept(message: Message): Boolean = when (message) {
        is Message.Logger.LogLine -> {
            val throwable = if (message.throwable != null) {
                try {
                    val ois = ObjectInputStream(ByteArrayInputStream(message.throwable))
                    ois.readObject() as Throwable
                } catch (e: Exception) {
                    logger.error("Failed to deserialize throwable", e)
                    null
                }
            } else null

            when (message.level) {
                ERROR -> logger.error(message.logLine, throwable)
                WARN -> logger.warn(message.logLine, throwable)
                LIFECYCLE -> logger.lifecycle(message.logLine)
                INFO -> logger.info(message.logLine)
                DEBUG -> logger.debug(message.logLine)
            }
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

internal class CompilerMessageVisitor(private val logger: KotlinLoggerMessageCollectorAdapter) : MessageVisitor {

    override fun accept(message: Message): Boolean = when (message) {
        is Message.CompilerMessage.Report -> {
            logger.report(message.severity, message.message, message.location, message.diagnosticId)
            true
        }
        is Message.CompilerMessage.Clear -> {
            logger.clear()
            true
        }
        else -> false
    }
}

internal class LookupMessageVisitor(private val lookupTracker: CompilerLookupTracker) : MessageVisitor {

    override fun accept(message: Message): Boolean = when (message) {
        is Message.LookupTracker.Record -> {
            lookupTracker.recordLookup(message.filePath, message.scopeFqName, message.scopeKind, message.name)
            true
        }
        is Message.LookupTracker.Clear -> {
            lookupTracker.clear()
            true
        }
        else -> false
    }
}
