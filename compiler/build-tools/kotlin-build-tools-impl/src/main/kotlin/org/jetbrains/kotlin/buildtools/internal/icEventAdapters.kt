/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.build.report.events.IcEventImpl
import org.jetbrains.kotlin.buildtools.api.trackers.FileCompilationReason
import org.jetbrains.kotlin.buildtools.api.trackers.IcEvent

internal fun List<IcEventImpl>.toIcEvents(): List<IcEvent> = map { event ->
    when (event) {
        is IcEventImpl.CompilationStarted -> object : IcEvent.CompilationStarted {
            override val isIncremental = event.isIncremental
            override val reason = event.reason
            override val type = event.type
            override val timestamp = event.timestamp
            override val iteration = event.iteration
        }
        is IcEventImpl.CompileIteration -> object : IcEvent.CompileIteration {
            override val reasons = event.reasons.mapValues {
                it.value.map { object : FileCompilationReason.Basic { override val readableString = it } }
            }
            override val exitCode = event.exitCode
            override val type = event.type
            override val timestamp = event.timestamp
            override val iteration = event.iteration
        }
    }
}
