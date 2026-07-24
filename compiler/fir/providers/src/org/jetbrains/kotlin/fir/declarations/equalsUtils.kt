/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.equalityBoundTypeOfParameter
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.util.OperatorNameConventions

fun FirNamedFunctionSymbol.isEquals(session: FirSession): Boolean {
    if (name != OperatorNameConventions.EQUALS) return false
    if (valueParameterSymbols.size != 1) return false
    if (contextParameterSymbols.isNotEmpty()) return false
    if (receiverParameterSymbol != null) return false
    val parameter = valueParameterSymbols.first()
    return parameter.resolvedReturnTypeRef.coneType.fullyExpandedType(session).isNullableAny
}

fun FirNamedFunction.isEquals(session: FirSession): Boolean = symbol.isEquals(session)

@JvmName("setEqualityBoundTypeFromOverriddenSymbols")
fun FirNamedFunction.setEqualityBoundTypeFromOverridden(
    overridden: Collection<FirCallableSymbol<*>>,
    session: FirSession,
) {
    setEqualityBoundTypeFromOverridden(overridden.map { it.fir }, session)
}

fun FirNamedFunction.setEqualityBoundTypeFromOverridden(
    overridden: Collection<FirCallableDeclaration>,
    session: FirSession,
) {
    if (isEquals(session)) {
        val bounds = overridden.filterIsInstance<FirNamedFunction>().mapNotNull { it.equalityBoundTypeOfParameter }
        if (bounds.isNotEmpty()) {
            equalityBoundTypeOfParameter = session.typeContext.intersectTypes(bounds)
        }
    }
}
