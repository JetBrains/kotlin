/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.render

object FirTypeProjectionComparator : Comparator<FirTypeProjection> {
    private val FirTypeProjection.priority : Int
        get() = when (this) {
            is FirTypeProjectionWithVariance -> 2
            is FirStarProjection -> 1
            else -> 0
        }

    override fun compare(a: FirTypeProjection, b: FirTypeProjection): Int {
        val priorityDiff = a.priority - b.priority
        if (priorityDiff != 0) {
            return priorityDiff
        }

        when (a) {
            is FirTypeProjectionWithVariance -> {
                require(b is FirTypeProjectionWithVariance) {
                    "priority is inconsistent: ${a.render()} v.s. ${b.render()}"
                }
                val typeRefDiff = FirTypeRefComparator.compare(a.typeRef, b.typeRef)
                if (typeRefDiff != 0) {
                    return typeRefDiff
                }
                return a.variance.ordinal - b.variance.ordinal
            }
            is FirStarProjection -> {
                return 0
            }
            else ->
                error("Unsupported type projection comparison: ${a.render()} v.s. ${b.render()}")
        }
    }
}
