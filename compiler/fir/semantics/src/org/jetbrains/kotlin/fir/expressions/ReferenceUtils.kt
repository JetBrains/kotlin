/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

fun FirElement.toReference(): FirReference? {
    return when (this) {
        is FirExpression -> toReference()
        is FirVariableAssignment -> calleeReference
        is FirResolvable -> calleeReference
        else -> null
    }
}

fun FirExpression.toReference(): FirReference? {
    return when (this) {
        is FirWrappedArgumentExpression -> expression.toResolvedCallableReference()
        is FirSmartCastExpression -> originalExpression.toReference()
        is FirDesugaredAssignmentValueReferenceExpression -> expressionRef.value.toReference()
        is FirResolvable -> calleeReference
        else -> null
    }
}

val FirVariableAssignment.calleeReference: FirReference? get() = lValue.toReference()

fun FirExpression.toResolvedCallableReference(): FirResolvedNamedReference? {
    return toReference()?.resolved
}

fun FirExpression.toResolvedCallableSymbol(): FirCallableSymbol<*>? {
    return toResolvedCallableReference()?.resolvedSymbol as? FirCallableSymbol<*>?
}
