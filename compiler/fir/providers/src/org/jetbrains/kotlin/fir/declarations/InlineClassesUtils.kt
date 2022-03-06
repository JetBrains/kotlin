/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.createTypeSubstitutorByTypeConstructor
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.fir.types.ConeTypeContext
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.types.model.typeConstructor

internal fun ConeKotlinType.substitutedUnderlyingTypeForInlineClass(session: FirSession, context: ConeTypeContext): ConeKotlinType? {
    val unsubstitutedType = unsubstitutedUnderlyingTypeForInlineClass(session) ?: return null
    val substitutor = createTypeSubstitutorByTypeConstructor(
        mapOf(this.typeConstructor(context) to this), context, approximateIntegerLiterals = true
    )
    return substitutor.substituteOrNull(unsubstitutedType)
}

internal fun ConeKotlinType.unsubstitutedUnderlyingTypeForInlineClass(session: FirSession): ConeKotlinType? {
    val symbol = (this.fullyExpandedType(session) as? ConeLookupTagBasedType)
        ?.lookupTag
        ?.toSymbol(session) as? FirRegularClassSymbol
        ?: return null
    symbol.ensureResolved(FirResolvePhase.STATUS)
    return symbol.fir.getInlineClassUnderlyingParameter(session)?.returnTypeRef?.coneType
}

// TODO: implement inlineClassRepresentation in FirRegularClass instead.
fun FirRegularClass.getInlineClassUnderlyingParameter(session: FirSession): FirValueParameter? =
    if (isInline) primaryConstructorIfAny(session)?.fir?.valueParameters?.singleOrNull() else null

fun FirRegularClass.getMultiFieldValueClassUnderlyingParameters(session: FirSession): List<FirValueParameter>? =
    if (isInline) primaryConstructorIfAny(session)?.fir?.valueParameters?.takeIf { it.size > 1 } else null
