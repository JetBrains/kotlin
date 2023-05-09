/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.utils.newHashMapWithExpectedSize

class IrTypeSystemContextWithAdditionalAxioms(
    typeSystem: IrTypeSystemContext,
    firstParameters: List<IrTypeParameter>,
    secondParameters: List<IrTypeParameter>
) : IrTypeSystemContext by typeSystem {
    init {
        assert(firstParameters.size == secondParameters.size) {
            "different length of type parameter lists: $firstParameters vs $secondParameters"
        }
    }

    private val firstTypeParameterConstructors = firstParameters.map { it.symbol }
    private val secondTypeParameterConstructors = secondParameters.map { it.symbol }
    private val matchingTypeConstructors = firstTypeParameterConstructors
        .zip(secondTypeParameterConstructors)
        .toMap(newHashMapWithExpectedSize(firstTypeParameterConstructors.size))

    override fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean {
        if (super.areEqualTypeConstructors(c1, c2)) return true
        if (matchingTypeConstructors[c1] == c2 || matchingTypeConstructors[c2] == c1) return true
        return false
    }
}
