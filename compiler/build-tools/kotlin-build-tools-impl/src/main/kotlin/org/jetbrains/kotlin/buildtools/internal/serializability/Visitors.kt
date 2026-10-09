/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(InternalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.CompilerEnumWhenTracker
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.CompilerExpectActualTracker
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.CompilerFileMappingTracker
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.CompilerImportTracker
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.CompilerInlineConstTracker
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

internal class FileMappingVisitor(private val tracker: CompilerFileMappingTracker) : MessageVisitor {

    override fun accept(message: Message): Boolean = when (message) {
        is Message.FileMappingTracker.RecordOutputFileGeneratedForPlugin -> {
            tracker.recordOutputFileGeneratedForPlugin(message.outputFilePath)
            true
        }
        is Message.FileMappingTracker.RecordSourceFileGeneratedForPlugin -> {
            tracker.recordSourceFileGeneratedForPlugin(message.sourceFilePath)
            true
        }
        is Message.FileMappingTracker.RecordSourceFilesToOutputFileMapping -> {
            tracker.recordSourceFilesToOutputFileMapping(message.sourceFilePaths, message.outputFilePath)
            true
        }
        is Message.FileMappingTracker.RecordSourceReferencedByCompilerPlugin -> {
            tracker.recordSourceReferencedByCompilerPlugin(message.sourceFilePath)
            true
        }
        else -> false
    }
}

internal class ExpectActualVisitor(private val tracker: CompilerExpectActualTracker) : MessageVisitor {

    override fun accept(message: Message): Boolean = when (message) {
        is Message.ExpectActualTracker.Report -> {
            tracker.report(message.expectFilePath, message.actualFilePath)
            true
        }
        is Message.ExpectActualTracker.ReportExpectOfLenientStub -> {
            tracker.reportExpectOfLenientStub(message.expectFilePath)
            true
        }
        else -> false
    }
}

internal class EnumWhenVisitor(private val tracker: CompilerEnumWhenTracker) : MessageVisitor {

    override fun accept(message: Message): Boolean = when (message) {
        is Message.EnumWhenTracker.Report -> {
            tracker.report(message.whenExpressionFilePath, message.enumClassFqName)
            true
        }
        else -> false
    }
}

internal class ImportVisitor(private val tracker: CompilerImportTracker) : MessageVisitor {

    override fun accept(message: Message): Boolean = when (message) {
        is Message.ImportTracker.Report -> {
            tracker.report(message.filePath, message.importedFqName)
            true
        }
        else -> false
    }
}

internal class InlineConstVisitor(private val tracker: CompilerInlineConstTracker) : MessageVisitor {

    override fun accept(message: Message): Boolean = when (message) {
        is Message.InlineConstTracker.Report -> {
            tracker.report(message.filePath, message.owner, message.name, message.constType)
            true
        }
        else -> false
    }
}
