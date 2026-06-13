/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.typeUtil

import org.jetbrains.kotlin.types.Variance

fun getEffectiveVariance(parameterVariance: Variance, projectionKind: Variance): Variance {
    if (parameterVariance === Variance.INVARIANT) {
        return projectionKind
    }
    if (projectionKind === Variance.INVARIANT) {
        return parameterVariance
    }
    if (parameterVariance === projectionKind) {
        return parameterVariance
    }

    // In<out X> = In<*>
    // Out<in X> = Out<*>
    return Variance.OUT_VARIANCE
}
