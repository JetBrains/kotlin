/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.statistics

import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Schedules periodic statistics domain updates.
 */
class LLStatisticsScheduler(val statisticsService: LLStatisticsService) {
    var scheduledUpdates: ScheduledFuture<*>? = null

    /**
     * [schedule] must only be called *once* from a single thread.
     */
    fun schedule() {
        scheduledUpdates = scheduleWithInterval(updateInterval) {
            statisticsService.domains.forEach { it.update() }
        }
    }

    fun scheduleWithInterval(interval: Duration, action: Runnable): ScheduledFuture<*> {
        val milliseconds = interval.inWholeMilliseconds
        return AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
            action,
            milliseconds,
            milliseconds,
            TimeUnit.MILLISECONDS,
        )
    }

    /**
     * [cancel] must only be called *once* from a single thread.
     */
    fun cancel() {
        scheduledUpdates?.cancel(true)
        scheduledUpdates = null
    }

    companion object {
        val updateInterval = 20.milliseconds
    }
}
