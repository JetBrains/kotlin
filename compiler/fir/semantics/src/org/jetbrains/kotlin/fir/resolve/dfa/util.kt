/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirNamedReferenceWithCandidateBase
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.types.*

fun TypeStatement?.smartCastedType(context: ConeTypeContext, originalType: ConeKotlinType): ConeKotlinType =
    if (this != null && exactType.isNotEmpty()) {
        context.intersectTypes(exactType.toMutableList().also { it += originalType })
    } else {
        originalType
    }

@DfaInternals
fun FirOperation.isEq(): Boolean {
    return when (this) {
        FirOperation.EQ, FirOperation.IDENTITY -> true
        FirOperation.NOT_EQ, FirOperation.NOT_IDENTITY -> false
        else -> throw IllegalArgumentException("$this should not be there")
    }
}

@DfaInternals
val FirExpression.coneType: ConeKotlinType
    get() = typeRef.coneType

@DfaInternals
val FirElement.symbol: FirBasedSymbol<*>?
    get() = when (this) {
        is FirResolvable -> symbol
        is FirDeclaration -> symbol
        is FirWhenSubjectExpression -> whenRef.value.subject?.symbol
        is FirSafeCallExpression -> selector.symbol
        is FirSmartCastExpression -> originalExpression.symbol
        else -> null
    }?.takeIf {
        (this as? FirExpression)?.unwrapSmartcastExpression() is FirThisReceiverExpression ||
                (it !is FirFunctionSymbol<*> && it !is FirSyntheticPropertySymbol)
    }

@DfaInternals
internal val FirResolvable.symbol: FirBasedSymbol<*>?
    get() = when (val reference = calleeReference) {
        is FirThisReference -> reference.boundSymbol
        is FirResolvedNamedReference -> reference.resolvedSymbol
        is FirNamedReferenceWithCandidateBase -> reference.candidateSymbol
        else -> null
    }

@DfaInternals
fun FirElement.unwrapElement(): FirElement = when (this) {
    is FirWhenSubjectExpression -> whenRef.value.let { it.subjectVariable ?: it.subject }?.unwrapElement() ?: this
    is FirSmartCastExpression -> originalExpression.unwrapElement()
    is FirSafeCallExpression -> selector.unwrapElement()
    is FirCheckedSafeCallSubject -> originalReceiverRef.value.unwrapElement()
    is FirCheckNotNullCall -> argument.unwrapElement()
    else -> this
}

fun FirExpression.unwrapSmartcastExpression(): FirExpression =
    when (this) {
        is FirSmartCastExpression -> originalExpression
        else -> this
    }
