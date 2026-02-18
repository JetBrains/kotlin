/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.isClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.toReference
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeVisibilityError
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.DataClassResolver
import org.jetbrains.kotlin.types.EnrichedProjectionKind
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.runIf

fun isPrivateToThisInvisibleAccess(
    expression: FirQualifiedAccessExpression,
    session: FirSession,
    symbolFromSmartCast: FirCallableSymbol<*>? = null,
): Boolean = getSymbolAndQualifierIfInvisibleAccess(expression, session, symbolFromSmartCast) != null

fun getSymbolAndQualifierIfInvisibleAccess(
    expression: FirQualifiedAccessExpression,
    session: FirSession,
    symbolFromSmartCast: FirCallableSymbol<*>? = null,
): Pair<FirCallableSymbol<*>, FirResolvedQualifier?>? {
    val reference = expression.calleeReference.resolved ?: return null
    if (reference is FirResolvedErrorReference) {
        // If there was a visibility diagnostic, no need to report another one about visibility
        when (reference.diagnostic) {
            is ConeVisibilityError,
                -> return null
        }
    }
    val dispatchReceiver = expression.dispatchReceiver
    val qualifierReceiverForUnboundReference = runIf(expression is FirCallableReferenceAccess) {
        // In this case (unbound callable reference) dispatch receiver is null, but in fact it's an equivalent case
        expression.explicitReceiver as? FirResolvedQualifier
    }
    if (dispatchReceiver == null && qualifierReceiverForUnboundReference == null) return null

    val symbol = symbolFromSmartCast ?: reference.toResolvedCallableSymbol(discardErrorReference = true) ?: return null
    if (symbol.visibility != Visibilities.Private) return null
    val session = session
    val containingClassSymbol = symbol.containingClassLookupTag()?.toClassSymbol(session)

    if (!isPrivateToThis(symbol.unwrapFakeOverrides(), containingClassSymbol, session)) return null

    val invisible = when (val receiverReference = dispatchReceiver?.toReference(session)) {
        is FirThisReference -> receiverReference.boundSymbol != containingClassSymbol
        else -> true
    }

    return if (invisible) {
        symbol to qualifierReceiverForUnboundReference
    } else {
        null
    }
}

private fun isPrivateToThis(
    symbol: FirCallableSymbol<*>,
    containingClassSymbol: FirClassSymbol<*>?,
    session: FirSession
): Boolean {
    if (containingClassSymbol == null) return false
    if (symbol is FirConstructorSymbol) return false
    if (containingClassSymbol.typeParameterSymbols.all { it.variance == Variance.INVARIANT }) return false
    // KT-68636 data class generated copy method can never have privateToThis visibility
    // We have to explicitly exclude data class copy because a general case isn't yet supported KT-35396
    if (symbol.isDataClassCopy(containingClassSymbol, session)) return false

    if (symbol.resolvedReceiverType?.contradictsWith(Variance.IN_VARIANCE, session) == true) {
        return true
    }
    if (symbol.resolvedReturnType.contradictsWith(
            if (symbol is FirPropertySymbol && symbol.isVar) Variance.INVARIANT
            else Variance.OUT_VARIANCE,
            session
        )
    ) {
        return true
    }
    if (symbol is FirFunctionSymbol<*>) {
        for (parameter in symbol.valueParameterSymbols) {
            if (parameter.resolvedReturnType.contradictsWith(Variance.IN_VARIANCE, session)) {
                return true
            }
        }
    }
    for (parameter in symbol.contextParameterSymbols) {
        if (parameter.resolvedReturnType.contradictsWith(Variance.IN_VARIANCE, session)) {
            return true
        }
    }
    return false
}

fun FirCallableSymbol<*>.isDataClassCopy(containingClass: FirClassSymbol<*>?, session: FirSession): Boolean {
    with(unwrapSubstitutionOverrides()) { // Shadow "non-normalized" this
        val constructor = containingClass?.primaryConstructorIfAny(session)
        return this is FirNamedFunctionSymbol &&
                DataClassResolver.isCopy(name) &&
                containingClass != null &&
                containingClass.isData &&
                containingClass.classKind.isClass &&
                dispatchReceiverType?.classId == containingClass.classId &&
                resolvedReturnType.classId == containingClass.classId &&
                constructor != null &&
                !hasContextParameters &&
                typeParameterSymbols.isEmpty() &&
                receiverParameterSymbol == null &&
                valueParameterSymbols.map { it.isVararg to it.resolvedReturnType } == constructor.valueParameterSymbols.map { it.isVararg to it.resolvedReturnType }
    }
}

private fun ConeKotlinType.contradictsWith(requiredVariance: Variance, session: FirSession): Boolean {
    when (this) {
        is ConeLookupTagBasedType -> {
            if (this is ConeTypeParameterType) {
                return !this.lookupTag.typeParameterSymbol.variance.allowsPosition(requiredVariance)
            }
            if (this is ConeClassLikeType) {
                // It's safe to access fir here, because later we access only variance of type parameters of the class
                // And variance can not be changed after raw fir stage
                @OptIn(SymbolInternals::class)
                val classLike = this.lookupTag.toSymbol(session)?.fir ?: return false
                for ((index, argument) in this.typeArguments.withIndex()) {
                    val typeParameterRef = classLike.typeParameters.getOrNull(index)
                    if (typeParameterRef !is FirTypeParameter) continue
                    val requiredVarianceForArgument = when (
                        EnrichedProjectionKind.getEffectiveProjectionKind(typeParameterRef.variance, argument.variance)
                    ) {
                        EnrichedProjectionKind.OUT -> requiredVariance
                        EnrichedProjectionKind.IN -> requiredVariance.opposite()
                        EnrichedProjectionKind.INV -> Variance.INVARIANT
                        EnrichedProjectionKind.STAR -> continue // CONFLICTING_PROJECTION error was reported
                    }
                    val argType = argument.type ?: continue
                    if (argType.contradictsWith(requiredVarianceForArgument, session)) {
                        return true
                    }
                }
            }
        }
        is ConeFlexibleType -> {
            return lowerBound.contradictsWith(requiredVariance, session)
        }
        is ConeDefinitelyNotNullType -> {
            return original.contradictsWith(requiredVariance, session)
        }
        is ConeIntersectionType -> {
            return this.intersectedTypes.any { it.contradictsWith(requiredVariance, session) }
        }
        is ConeCapturedType -> {
            // Looks like not possible here
            return false
        }
        is ConeIntegerConstantOperatorType,
        is ConeIntegerLiteralConstantType,
        is ConeStubTypeForTypeVariableInSubtyping,
        is ConeTypeVariableType,
            -> return false
    }
    return false
}
