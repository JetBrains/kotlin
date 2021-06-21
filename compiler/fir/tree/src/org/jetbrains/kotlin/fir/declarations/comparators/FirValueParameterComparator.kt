/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.comparators

import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.types.FirTypeRefComparator

object FirValueParameterComparator : Comparator<FirValueParameter> {
    override fun compare(a: FirValueParameter, b: FirValueParameter): Int {
        val valueParameterNameDiff = a.name.compareTo(b.name)
        if (valueParameterNameDiff != 0) {
            return valueParameterNameDiff
        }

        val valueParameterTypeDiff = FirTypeRefComparator.compare(a.returnTypeRef, b.returnTypeRef)
        if (valueParameterTypeDiff != 0) {
            return valueParameterTypeDiff
        }

        val aHasDefaultValue = if (a.defaultValue != null) 1 else 0
        val bHasDefaultValue = if (b.defaultValue != null) 1 else 0
        val defaultValueDiff = aHasDefaultValue - bHasDefaultValue
        if (defaultValueDiff != 0) {
            return defaultValueDiff
        }

        val aIsVararg = if (a.isVararg) 1 else 0
        val bIsVararg = if (b.isVararg) 1 else 0
        return aIsVararg - bIsVararg
    }
}
