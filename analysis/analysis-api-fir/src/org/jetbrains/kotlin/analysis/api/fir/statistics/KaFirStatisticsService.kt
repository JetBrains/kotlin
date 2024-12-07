/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.statistics

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.statistics.KaStatisticsService
import org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.LLStatisticsService

internal class KaFirStatisticsService(private val project: Project) : KaStatisticsService {
    private val statisticsService: LLStatisticsService?
        get() = LLStatisticsService.getInstance(project)

    override fun start() {
        statisticsService?.start()
    }
}
