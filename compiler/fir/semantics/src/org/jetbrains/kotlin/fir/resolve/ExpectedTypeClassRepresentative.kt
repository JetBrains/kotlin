/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*

/**
 * The class whose static scope serves as an implicit receiver when a collection literal or a context-sensitive simple name
 * is resolved against the expected type [this], or `null` when the type has no single meaningful class behind it
 * (type variables, stub and dynamic types, anonymous objects, type parameters with several bounds, etc.).
 */
fun ConeKotlinType.getClassRepresentativeForResolutionByExpectedType(session: FirSession): FirRegularClassSymbol? {
    return when (this) {
        // NB: must be checked before ConeFlexibleType, otherwise `dynamic` would be represented by its lower bound `Nothing`
        is ConeDynamicType -> null
        is ConeFlexibleType -> lowerBound.getClassRepresentativeForResolutionByExpectedType(session)
        is ConeCapturedType -> constructor.lowerType?.getClassRepresentativeForResolutionByExpectedType(session)
        // very rarely, but still needed, because there might be an expected type of form `Captured(in SomeCollection?) & Any`
        is ConeDefinitelyNotNullType -> original.getClassRepresentativeForResolutionByExpectedType(session)
        is ConeIntegerLiteralType -> possibleTypes.singleOrNull()?.getClassRepresentativeForResolutionByExpectedType(session)
        is ConeIntersectionType -> {
            val representativesForComponents = intersectedTypes.map {
                it.getClassRepresentativeForResolutionByExpectedType(session)
                    ?: return@getClassRepresentativeForResolutionByExpectedType null
            }
            representativesForComponents.chooseMostSpecificClass(session)
        }
        is ConeStubType, is ConeTypeVariableType, is ConeUnionType -> null
        is ConeLookupTagBasedType ->
            when (val symbol = lookupTag.toSymbol(session)) {
                is FirRegularClassSymbol -> symbol
                is FirTypeParameterSymbol ->
                    symbol.resolvedBounds.singleOrNull()?.coneType?.getClassRepresentativeForResolutionByExpectedType(session)
                is FirTypeAliasSymbol ->
                    fullyExpandedType(session)
                        .takeIf { it !== this }
                        ?.getClassRepresentativeForResolutionByExpectedType(session)
                is FirAnonymousObjectSymbol, null -> null
            }
    }
}

/**
 * @return the class that is a subclass of all the others in the collection, if there is one
 */
fun Collection<FirRegularClassSymbol>.chooseMostSpecificClass(session: FirSession): FirRegularClassSymbol? {
    return firstOrNull { candidate ->
        all { other ->
            candidate.fir.isSubclassOf(other.toLookupTag(), session, isStrict = false)
        }
    }
}
