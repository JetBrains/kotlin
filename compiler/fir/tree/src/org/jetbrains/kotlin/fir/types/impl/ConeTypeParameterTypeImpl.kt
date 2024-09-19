/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType

class ConeTypeParameterTypeImpl(
    override val lookupTag: ConeTypeParameterLookupTag,
    override val isMarkedNullable: Boolean,
    override val attributes: ConeAttributes = ConeAttributes.Empty
) : ConeTypeParameterType() {
    override val typeArguments: Array<out ConeTypeProjection>
        get() = EMPTY_ARRAY

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConeTypeParameterTypeImpl

        if (lookupTag != other.lookupTag) return false
        if (isMarkedNullable != other.isMarkedNullable) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lookupTag.hashCode()
        result = 31 * result + isMarkedNullable.hashCode()
        return result
    }

}
