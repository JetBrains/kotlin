/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.declarations.FirValueParameterKind
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.lookupTagIfAny
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.util.anonymousContextParameterName

fun FirValueParameterSymbol.anonymousContextParameterName(session: FirSession, invalidChars: Set<Char>): String? {
    if (fir.valueParameterKind != FirValueParameterKind.ContextParameter || name != SpecialNames.UNDERSCORE_FOR_UNUSED_VAR) return null
    return anonymousContextParameterName(
        (containingDeclarationSymbol as FirCallableSymbol).contextParameterSymbols,
        invalidChars,
        erasedUpperBoundName = {
            it.resolvedReturnType.erasedUpperBoundName(session)?.identifierOrNullIfSpecial ?: "???"
        }
    )
}

private fun ConeKotlinType.erasedUpperBoundName(session: FirSession): Name? {
    return when (this) {
        is ConeTypeParameterType -> {
            val bounds = lookupTag.symbol.resolvedBounds
            for (bound in bounds) {
                val type = bound.coneType.fullyExpandedType(session)
                val classSymbol = type.toClassSymbol(session) ?: continue
                if (classSymbol.classKind != ClassKind.ANNOTATION_CLASS && classSymbol.classKind != ClassKind.INTERFACE) {
                    return classSymbol.name
                }
            }
            bounds.first().coneType.erasedUpperBoundName(session)
        }
        is ConeLookupTagBasedType -> fullyExpandedType(session).lookupTagIfAny?.name
        else -> null
    }
}