/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.comparators

import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirConstructedClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.types.FirTypeRefComparator

object FirTypeParameterRefComparator : Comparator<FirTypeParameterRef> {
    private val FirTypeParameterRef.priority : Int
        get() = when (this) {
            is FirConstructedClassTypeParameterRef -> 3
            is FirOuterClassTypeParameterRef -> 2
            is FirTypeParameter -> 1
            else -> 0
        }

    override fun compare(a: FirTypeParameterRef, b: FirTypeParameterRef): Int {
        val priorityDiff = a.priority - b.priority
        if (priorityDiff != 0) {
            return priorityDiff
        }
        when (a) {
            is FirConstructedClassTypeParameterRef -> {
                require(b is FirConstructedClassTypeParameterRef) {
                    "priority is inconsistent: ${a.render()} v.s. ${b.render()}"
                }
                return a.symbol.name.compareTo(b.symbol.name)
            }
            is FirOuterClassTypeParameterRef -> {
                require(b is FirOuterClassTypeParameterRef) {
                    "priority is inconsistent: ${a.render()} v.s. ${b.render()}"
                }
                return a.symbol.name.compareTo(b.symbol.name)
            }
            is FirTypeParameter -> {
                require(b is FirTypeParameter) {
                    "priority is inconsistent: ${a.render()} v.s. ${b.render()}"
                }
                val nameDiff = a.symbol.name.compareTo(b.symbol.name)
                if (nameDiff != 0) {
                    return nameDiff
                }
                val varianceDiff = a.variance.ordinal - b.variance.ordinal
                if (varianceDiff != 0) {
                    return varianceDiff
                }
                val boundsSizeDiff = a.bounds.size - b.bounds.size
                if (boundsSizeDiff != 0) {
                    return boundsSizeDiff
                }
                for ((aBound, bBound) in a.bounds.zip(b.bounds)) {
                    val boundDiff = FirTypeRefComparator.compare(aBound, bBound)
                    if (boundDiff != 0) {
                        return boundDiff
                    }
                }
                return 0
            }
            else ->
                error("Unsupported type parameter reference comparison: ${a.render()} v.s. ${b.render()}")
        }
    }
}
