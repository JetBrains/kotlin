/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.build.report.events.IcEventImpl
import org.jetbrains.kotlin.buildtools.api.trackers.FileCompilationReason
import org.jetbrains.kotlin.buildtools.api.trackers.IcEvent

internal data class CompilationStartedEvent(
    override val isIncremental: Boolean,
    override val reason: String?,
    override val type: String = "CompilationStarted",
    override val timestamp: Long = System.currentTimeMillis(),
    override val iteration: Int,
) : IcEvent.CompilationStarted

internal data class CompileIterationEvent(
    override val reasons: Map<String, List<FileCompilationReason>>,
    override val exitCode: String,
    override val type: String = "CompileIteration",
    override val timestamp: Long = System.currentTimeMillis(),
    override val iteration: Int,
) : IcEvent.CompileIteration

internal data class FileCompilationReasonBasic(
    override val readableString: String,
) : FileCompilationReason.Basic

internal fun List<IcEventImpl>.toIcEvents(): List<IcEvent> = map { event ->
    when (event) {
        is IcEventImpl.CompilationStarted -> CompilationStartedEvent(
            isIncremental = event.isIncremental,
            reason = event.reason,
            type = event.type,
            timestamp = event.timestamp,
            iteration = event.iteration,
        )
        is IcEventImpl.CompileIteration -> CompileIterationEvent(
            reasons = event.reasons.mapValues {
                it.value.map { FileCompilationReasonBasic(it) }
            },
            exitCode = event.exitCode,
            type = event.type,
            timestamp = event.timestamp,
            iteration = event.iteration,
        )
    }
}
