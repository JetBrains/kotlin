/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.serializability

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.buildtools.api.trackers.BuildMetricsCollector
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation

@Serializable
public sealed class Message {
    @Serializable
    public data class CollectMetricMessage(
        public val name: String,
        public val type: BuildMetricsCollector.ValueType,
        public val value: Long,
    ) : Message()

    public object LookupTracker {
        @Serializable
        public data class Record(
            public val filePath: String,
            public val scopeFqName: String,
            public val scopeKind: CompilerLookupTracker.ScopeKind,
            public val name: String,
        ) : Message()

        @Serializable
        public object Clear : Message()
    }

    public object CompilerMessage {
        @Serializable
        public data class Report(
            public val severity: CompilerMessageSeverity,
            public val message: String,
            @Serializable(with = CompilerMessageSourceLocationSerializer::class)
            public val location: CompilerMessageSourceLocation? = null,
            public val diagnosticId: String?,
        ) : Message()

        @Serializable
        public object Clear : Message()
    }
}
