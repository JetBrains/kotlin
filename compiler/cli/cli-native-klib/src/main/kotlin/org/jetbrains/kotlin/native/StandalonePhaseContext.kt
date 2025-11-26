/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native

import org.jetbrains.kotlin.backend.konan.NativeKlibCompilationConfig
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.util.PerformanceManager

/**
 * A standalone implementation of [PhaseContext] that works with [StandaloneNativeKlibCompilationConfig].
 *
 * This class provides the context needed to run the klib compilation phases
 * without depending on the full Native backend infrastructure.
 */
class StandalonePhaseContext(
    override val config: NativeKlibCompilationConfig,
) : PhaseContext {
    override var inVerbosePhase: Boolean = false

    override val messageCollector: MessageCollector
        get() = config.configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)

    override val performanceManager: PerformanceManager?
        get() = config.configuration.perfManager

    override fun dispose() {
        // Nothing to dispose
    }
}
