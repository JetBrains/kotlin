/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.types.AbstractTypePreparator
import org.jetbrains.kotlin.types.model.KotlinTypeMarker

class ConeTypePreparator(val session: FirSession) : AbstractTypePreparator() {
    override fun prepareType(type: KotlinTypeMarker): KotlinTypeMarker {
        return when (type) {
            is ConeClassLikeType -> type.fullyExpandedType(session)
            is ConeFlexibleType -> {
                val lowerBound = prepareType(type.lowerBound)
                if (lowerBound === type.lowerBound) return type

                ConeFlexibleType(
                    lowerBound as ConeKotlinType,
                    prepareType(type.upperBound) as ConeKotlinType
                )
            }
            else -> type
        }
    }
}
