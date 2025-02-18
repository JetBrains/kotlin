/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.domains

/**
 * An [LLStatisticsDomain] encapsulates the meters, update logic, and any additional state required to support statistics
 * collection and reporting in a specific area of concern.
 */
interface LLStatisticsDomain {
    /**
     * Performs an update on a fixed schedule which cannot be covered by eager or asynchronous meters.
     */
    fun update() {}
}
