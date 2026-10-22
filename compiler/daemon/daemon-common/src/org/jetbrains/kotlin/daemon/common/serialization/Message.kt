/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalBuildToolsApi::class)
@file:UseSerializers(PathAsStringSerializer::class)

package org.jetbrains.kotlin.daemon.common.serialization

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.trackers.BuildMetricsCollector
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.api.trackers.IcEvent
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import java.nio.file.Path

@Serializable
sealed class Message {
    @Serializable
    data class CollectMetricMessage(
        val name: String,
        val type: BuildMetricsCollector.ValueType,
        val value: Long,
    ) : Message()

    object LookupTracker {
        @Serializable
        data class Record(
            val filePath: String,
            val scopeFqName: String,
            val scopeKind: CompilerLookupTracker.ScopeKind,
            val name: String,
        ) : Message()

        @Serializable
        object Clear : Message()
    }

    object CompilerMessage {
        @Serializable
        data class Report(
            val severity: CompilerMessageSeverity,
            val message: String,
            @Contextual
            val location: CompilerMessageSourceLocation? = null,
            val diagnosticId: String?,
        ) : Message()

        @Serializable
        object Clear : Message()
    }

    object IcEventCollector {
        @Serializable
        data class CollectEvents(
            val events: List<@Contextual IcEvent>,
        ) : Message()
    }

    object Logger {
        @Serializable
        data class LogLine(
            val level: String,
            val logLine: String,
            val throwable: ByteArray? = null
        ) : Message()
    }

    object FileMappingTracker {
        @Serializable
        data class RecordSourceFilesToOutputFileMapping(val sourceFilePaths: List<Path>, val outputFilePath: Path) : Message()

        @Serializable
        data class RecordSourceReferencedByCompilerPlugin(val sourceFilePath: Path) : Message()

        @Serializable
        data class RecordOutputFileGeneratedForPlugin(val outputFilePath: Path) : Message()

        @Serializable
        data class RecordSourceFileGeneratedForPlugin(val sourceFilePath: Path) : Message()
    }

    object EnumWhenTracker {
        @Serializable
        data class Report(val whenExpressionFilePath: Path, val enumClassFqName: String) : Message()
    }

    object ImportTracker {
        @Serializable
        data class Report(val filePath: Path, val importedFqName: String) : Message()
    }

    object InlineConstTracker {
        @Serializable
        data class Report(val filePath: Path, val owner: String, val name: String, val constType: String) : Message()
    }
}
