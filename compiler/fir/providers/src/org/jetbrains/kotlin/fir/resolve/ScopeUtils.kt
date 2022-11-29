/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.resolve.substitution.ConeRawScopeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.FirScopeWithFakeOverrideTypeCalculator
import org.jetbrains.kotlin.fir.scopes.impl.FirTypeIntersectionScope
import org.jetbrains.kotlin.fir.scopes.impl.dynamicMembersStorage
import org.jetbrains.kotlin.fir.scopes.impl.getOrBuildScopeForIntegerConstantOperatorType
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.ClassId

fun FirSmartCastExpression.smartcastScope(
    useSiteSession: FirSession,
    scopeSession: ScopeSession
): FirTypeScope? {
    val smartcastType = smartcastTypeWithoutNullableNothing?.coneType ?: smartcastType.coneType
    val smartcastScope = smartcastType.scope(useSiteSession, scopeSession, FakeOverrideTypeCalculator.DoNothing)
    if (isStable) {
        return smartcastScope
    }
    val originalScope = originalExpression.typeRef.coneType.scope(useSiteSession, scopeSession, FakeOverrideTypeCalculator.DoNothing)
        ?: return smartcastScope

    if (smartcastScope == null) {
        return originalScope
    }
    return FirUnstableSmartcastTypeScope(smartcastScope, originalScope)
}

fun ConeKotlinType.scope(
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    fakeOverrideTypeCalculator: FakeOverrideTypeCalculator
): FirTypeScope? {
    val scope = scope(useSiteSession, scopeSession, FirResolvePhase.DECLARATIONS) ?: return null
    if (fakeOverrideTypeCalculator == FakeOverrideTypeCalculator.DoNothing) return scope
    return FirScopeWithFakeOverrideTypeCalculator(scope, fakeOverrideTypeCalculator)
}

private fun ConeKotlinType.scope(useSiteSession: FirSession, scopeSession: ScopeSession, requiredPhase: FirResolvePhase): FirTypeScope? {
    return when (this) {
        is ConeErrorType -> null
        is ConeClassLikeType -> {
            val fullyExpandedType = fullyExpandedType(useSiteSession)
            val fir = fullyExpandedType.lookupTag.toSymbol(useSiteSession)?.fir as? FirClass ?: return null

            fir.symbol.lazyResolveToPhase(requiredPhase)

            val substitutor = when {
                attributes.contains(CompilerConeAttributes.RawType) -> ConeRawScopeSubstitutor(useSiteSession)
                else -> substitutorByMap(
                    createSubstitution(fir.typeParameters, fullyExpandedType, useSiteSession),
                    useSiteSession,
                )
            }

            fir.scopeForClass(substitutor, useSiteSession, scopeSession)
        }

        is ConeTypeParameterType -> {
            val symbol = lookupTag.symbol
            scopeSession.getOrBuild(symbol, TYPE_PARAMETER_SCOPE_KEY) {
                val intersectionType = ConeTypeIntersector.intersectTypes(
                    useSiteSession.typeContext,
                    symbol.resolvedBounds.map { it.coneType }
                )
                intersectionType.scope(useSiteSession, scopeSession, requiredPhase) ?: FirTypeScope.Empty
            }
        }

        is ConeRawType -> lowerBound.scope(useSiteSession, scopeSession, requiredPhase)
        is ConeDynamicType -> useSiteSession.dynamicMembersStorage.getDynamicScopeFor(scopeSession)
        is ConeFlexibleType -> lowerBound.scope(useSiteSession, scopeSession, requiredPhase)
        is ConeIntersectionType -> FirTypeIntersectionScope.prepareIntersectionScope(
            useSiteSession,
            FirIntersectionScopeOverrideChecker(useSiteSession),
            intersectedTypes.mapNotNullTo(mutableListOf()) {
                it.scope(useSiteSession, scopeSession, requiredPhase)
            },
            this
        )

        is ConeDefinitelyNotNullType -> original.scope(useSiteSession, scopeSession, requiredPhase)
        is ConeIntegerConstantOperatorType -> scopeSession.getOrBuildScopeForIntegerConstantOperatorType(useSiteSession, this)
        is ConeIntegerLiteralConstantType -> error("ILT should not be in receiver position")
        else -> null
    }
}

private fun ConeClassLikeType.obtainFirOfClass(useSiteSession: FirSession, requiredPhase: FirResolvePhase): FirClass? {
    val fullyExpandedType = fullyExpandedType(useSiteSession)
    val fir = fullyExpandedType.lookupTag.toSymbol(useSiteSession)?.fir as? FirClass ?: return null

    return fir.also { it.symbol.lazyResolveToPhase(requiredPhase) }
}

fun FirClassSymbol<*>.defaultType(): ConeClassLikeType = fir.defaultType()

fun FirClass.defaultType(): ConeClassLikeType =
    ConeClassLikeTypeImpl(
        symbol.toLookupTag(),
        typeParameters.map {
            ConeTypeParameterTypeImpl(
                it.symbol.toLookupTag(),
                isNullable = false
            )
        }.toTypedArray(),
        isNullable = false
    )

fun ClassId.defaultType(parameters: List<FirTypeParameterSymbol>): ConeClassLikeType =
    ConeClassLikeTypeImpl(
        ConeClassLikeLookupTagImpl(this),
        parameters.map {
            ConeTypeParameterTypeImpl(
                it.toLookupTag(),
                isNullable = false
            )
        }.toTypedArray(),
        isNullable = false,
    )

val TYPE_PARAMETER_SCOPE_KEY = scopeSessionKey<FirTypeParameterSymbol, FirTypeScope>()
