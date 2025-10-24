/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

import org.jetbrains.kotlin.backend.common.DisposableContext
import org.jetbrains.kotlin.backend.common.ErrorReportingContext
import org.jetbrains.kotlin.backend.konan.NativeKlibCompilationConfig
import org.jetbrains.kotlin.config.LoggingContext
import org.jetbrains.kotlin.util.PerformanceManager

interface PerformanceManagerContext {
    val performanceManager: PerformanceManager?
}

/**
 * Context is a set of resources that is shared between different phases. PhaseContext is a "minimal context",
 * effectively just a wrapper around a [org.jetbrains.kotlin.config.CompilerConfiguration]. Still, it is more than enough in many cases.
 *
 * There is a fuzzy line between phase Input/Output and Context. We can consider them as a spectre:
 * * On the one end there is a [org.jetbrains.kotlin.backend.konan.Context] (circa 1.8.0). It has a lot of properties,
 * some even lateinit, which makes this object hard to construct and phases that depend on it are tightly coupled.
 * But we don't need to pass data between phases explicitly, which makes code easier to write.
 * * One the other end we can pass everything explicitly via I/O types. It will decouple code at the cost of boilerplate.
 *
 * So we have to find a point on this spectre for each phase.
 * We still don't have a rule of thumb for deciding whether object should be a part of context or not.
 * Some notes:
 * * Lifetime of context should be as small as possible: it reduces memory usage and forces a clean architecture.
 * * Frontend and backend are not really tied to IR and its friends, so we can pass more bytes via I/O.
 * * On the other hand, middle- and bitcode phases are hard to decouple due to the way the code was written many years ago.
 * It will take some time to rewrite it properly.
 */
interface PhaseContext : LoggingContext, ErrorReportingContext, DisposableContext, PerformanceManagerContext {
    val config: NativeKlibCompilationConfig
}