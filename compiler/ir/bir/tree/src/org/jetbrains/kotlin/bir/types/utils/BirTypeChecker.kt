/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.types.utils

import org.jetbrains.kotlin.bir.declarations.BirTypeParameter
import org.jetbrains.kotlin.bir.types.BirTypeSystemContext
import org.jetbrains.kotlin.types.AbstractTypePreparator
import org.jetbrains.kotlin.types.AbstractTypeRefiner
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.utils.newHashMapWithExpectedSize

fun createBirTypeCheckerState(typeSystemContext: BirTypeSystemContext): TypeCheckerState {
    return TypeCheckerState(
        isErrorTypeEqualsToAnything = false,
        isStubTypeEqualsToAnything = false,
        allowedTypeVariable = false,
        typeSystemContext = typeSystemContext,
        kotlinTypePreparator = AbstractTypePreparator.Default,
        kotlinTypeRefiner = AbstractTypeRefiner.Default
    )
}

class BirTypeSystemContextWithAdditionalAxioms(
    typeSystem: BirTypeSystemContext,
    firstParameters: List<BirTypeParameter>,
    secondParameters: List<BirTypeParameter>
) : BirTypeSystemContext by typeSystem {
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
