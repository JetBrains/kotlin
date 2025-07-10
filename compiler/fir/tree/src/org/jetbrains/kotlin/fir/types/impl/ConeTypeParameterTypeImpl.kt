/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.types.CETypeParameterType
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.ConeErrorUnionType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection

class ConeTypeParameterTypeImpl private constructor(
    override val lookupTag: ConeTypeParameterLookupTag,
    override val isMarkedNullable: Boolean,
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeTypeParameterType() {
    override val typeArguments: Array<out ConeTypeProjection>
        get() = EMPTY_ARRAY

    companion object {
        fun createPure(
            lookupTag: ConeTypeParameterLookupTag,
            isMarkedNullable: Boolean,
            attributes: ConeAttributes = ConeAttributes.Empty,
        ): ConeTypeParameterTypeImpl {
            return ConeTypeParameterTypeImpl(lookupTag, isMarkedNullable, attributes)
        }

        fun create(
            lookupTag: ConeTypeParameterLookupTag,
            isMarkedNullable: Boolean,
            attributes: ConeAttributes = ConeAttributes.Empty,
        ): ConeErrorUnionType {
            return ConeErrorUnionType.create(
                ConeTypeParameterTypeImpl(lookupTag, isMarkedNullable, attributes),
                CETypeParameterType(lookupTag)
            )
        }

        fun createUnknown(
            lookupTag: ConeTypeParameterLookupTag,
            isMarkedNullable: Boolean,
            attributes: ConeAttributes = ConeAttributes.Empty,
        ): ConeTypeParameterTypeImpl =
            error("unknown")
    }
}
