/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization.constant

import org.jetbrains.kotlin.constant.ConstantValue
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.expressions.FirExpression

@RequiresOptIn(
    message = "Please note that this is internal API and it is supposed to be used with special conditions",
    level = RequiresOptIn.Level.WARNING
)
annotation class ConstValueProviderInternals

abstract class ConstValueProvider {
    abstract val session: FirSession
    abstract val evaluatedConstTracker: EvaluatedConstTracker

    var processingFirFile: FirFile? = null
        @ConstValueProviderInternals
        set

    abstract fun findConstantValueFor(firExpression: FirExpression?): ConstantValue<*>?
}
