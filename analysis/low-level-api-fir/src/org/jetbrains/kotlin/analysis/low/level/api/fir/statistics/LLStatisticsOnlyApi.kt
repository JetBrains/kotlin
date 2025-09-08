/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.statistics

/**
 * [LLStatisticsOnlyApi] is applied to endpoints of regular (non-statistics) LL FIR declarations. The annotation ensures that the specific
 * endpoint is only used for statistics and not for resolution purposes.
 */
@RequiresOptIn("This API is intended only for statistics collection. It must not be used for other purposes.")
annotation class LLStatisticsOnlyApi
