/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*

fun ConeKotlinType.getClassRepresentativeForContextSensitiveResolution(session: FirSession): FirRegularClassSymbol? {
    return when (this) {
        is ConeFlexibleType ->
            lowerBound.getClassRepresentativeForContextSensitiveResolution(session)?.takeIf {
                it == upperBound.getClassRepresentativeForContextSensitiveResolution(session)
            }

        is ConeDefinitelyNotNullType -> original.getClassRepresentativeForContextSensitiveResolution(session)

        is ConeIntegerLiteralType -> possibleTypes.singleOrNull()?.getClassRepresentativeForContextSensitiveResolution(session)

        is ConeIntersectionType -> {
            val representativesForComponents =
                intersectedTypes.map { it.getClassRepresentativeForContextSensitiveResolution(session) }

            if (representativesForComponents.any { it == null }) return null
            @Suppress("UNCHECKED_CAST") // See the check above
            representativesForComponents as List<FirClassSymbol<*>>

            representativesForComponents.firstOrNull { candidate ->
                representativesForComponents.all { other ->
                    candidate.fir.isSubclassOf(other.toLookupTag(), session, isStrict = false)
                }
            }
        }

        is ConeLookupTagBasedType ->
            when (val symbol = lookupTag.toSymbol(session)) {
                is FirRegularClassSymbol -> symbol

                is FirTypeParameterSymbol ->
                    symbol.resolvedBounds.singleOrNull()?.coneType?.getClassRepresentativeForContextSensitiveResolution(session)

                is FirAnonymousObjectSymbol -> null
                is FirTypeAliasSymbol ->
                    fullyExpandedType(session)
                        .takeIf { it !== this }
                        ?.getClassRepresentativeForContextSensitiveResolution(session)
                null -> null
            }

        is ConeCapturedType, is ConeStubTypeForTypeVariableInSubtyping, is ConeTypeVariableType -> null
    }
}
