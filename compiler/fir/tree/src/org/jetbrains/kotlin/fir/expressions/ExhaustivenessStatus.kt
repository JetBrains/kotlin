/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.diagnostics.WhenMissingCase

sealed class ExhaustivenessStatus {
    object Exhaustive : ExhaustivenessStatus()
    class NotExhaustive(val reasons: List<WhenMissingCase>) : ExhaustivenessStatus() {
        companion object {
            val NO_ELSE_BRANCH = NotExhaustive(listOf(WhenMissingCase.Unknown))
        }
    }
}


val FirWhenExpression.isExhaustive: Boolean
    get() = exhaustivenessStatus == ExhaustivenessStatus.Exhaustive
