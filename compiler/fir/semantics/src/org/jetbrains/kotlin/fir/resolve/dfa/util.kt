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
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeContext
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.unwrapFakeOverrides

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
val FirElement.symbol: FirBasedSymbol<*>?
    get() = when (this) {
        is FirResolvable -> symbol.unwrapFakeOverridesIfNecessary()
        is FirVariableAssignment -> unwrapLValue()?.symbol
        is FirDeclaration -> symbol.unwrapFakeOverridesIfNecessary()
        is FirWhenSubjectExpression -> whenRef.value.subject?.symbol
        is FirSafeCallExpression -> selector.symbol
        is FirSmartCastExpression -> originalExpression.symbol
        is FirDesugaredAssignmentValueReferenceExpression -> expressionRef.value.symbol
        else -> null
    }?.takeIf {
        (this as? FirExpression)?.unwrapSmartcastExpression() is FirThisReceiverExpression ||
                (it !is FirFunctionSymbol<*> && it !is FirSyntheticPropertySymbol)
    }

private fun FirBasedSymbol<*>?.unwrapFakeOverridesIfNecessary(): FirBasedSymbol<*>? {
    if (this !is FirCallableSymbol) return this
    // This is necessary only for sake of optimizations necessary because this is a really hot place.
    // Not having `dispatchReceiverType` means that this is a local/top-level property that can't be a fake override.
    // And at the same time, checking a field is much faster than a couple of attributes (0.3 secs at MT Full Kotlin)
    if (this.dispatchReceiverType == null) return this

    return this.unwrapFakeOverrides()
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
    is FirDesugaredAssignmentValueReferenceExpression -> expressionRef.value.unwrapElement()
    else -> this
}
