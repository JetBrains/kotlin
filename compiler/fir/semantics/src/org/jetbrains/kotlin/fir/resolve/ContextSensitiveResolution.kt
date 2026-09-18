/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType

fun ConeKotlinType.getParentChainForContextSensitiveResolutionOfExpressions(session: FirSession): Sequence<FirRegularClassSymbol> =
    getClassRepresentativeForResolutionByExpectedType(session)
        ?.getParentChainForContextSensitiveResolution(session, onlySealed = false)
        .orEmpty()

fun ConeKotlinType.getParentChainForContextSensitiveResolutionOfTypes(session: FirSession): Sequence<FirRegularClassSymbol> =
    getClassRepresentativeForResolutionByExpectedType(session)
        ?.getParentChainForContextSensitiveResolution(session, onlySealed = true)
        .orEmpty()

/**
 * The receiver class itself followed by its enclosing super classes
 */
fun FirRegularClassSymbol.getParentChainForContextSensitiveResolution(
    session: FirSession, onlySealed: Boolean = false
): Sequence<FirRegularClassSymbol> = sequence {
    var current: FirRegularClassSymbol? = this@getParentChainForContextSensitiveResolution
    var onlySealed = onlySealed

    while (current != null) {
        if (!onlySealed || current.isSealed) {
            yield(current)
        }
        // after the first one, return only sealed enclosing parents
        current = (current.getContainingDeclaration(session) as? FirRegularClassSymbol)
            ?.takeIf { isSubclassOf(it.toLookupTag(), session, isStrict = true, lookupInterfaces = true) }
        onlySealed = true
    }
}
