/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.mpp

import org.jetbrains.kotlin.mpp.TypeParameterSymbolMarker
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker

context(ExpectActualMatchingContext<*>)
internal fun areCompatibleTypeLists(
    expectedTypes: List<KotlinTypeMarker?>,
    actualTypes: List<KotlinTypeMarker?>,
): Boolean {
    for (i in expectedTypes.indices) {
        if (!areCompatibleExpectActualTypes(expectedTypes[i], actualTypes[i])) {
            return false
        }
    }
    return true
}

context(ExpectActualMatchingContext<*>)
internal fun areCompatibleTypeParameterUpperBounds(
    expectTypeParameterSymbols: List<TypeParameterSymbolMarker>,
    actualTypeParameterSymbols: List<TypeParameterSymbolMarker>,
    substitutor: TypeSubstitutorMarker,
): Boolean {
    for (i in expectTypeParameterSymbols.indices) {
        val expectBounds = expectTypeParameterSymbols[i].bounds
        val actualBounds = actualTypeParameterSymbols[i].bounds
        if (
            expectBounds.size != actualBounds.size ||
            !areCompatibleTypeLists(expectBounds.map { substitutor.safeSubstitute(it) }, actualBounds)
        ) {
            return false
        }
    }

    return true
}
