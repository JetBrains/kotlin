/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.resolve.substitution.ConeRawScopeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.FirScopeWithCallableCopyReturnTypeUpdater
import org.jetbrains.kotlin.fir.scopes.impl.FirTypeIntersectionScope
import org.jetbrains.kotlin.fir.scopes.impl.dynamicMembersStorage
import org.jetbrains.kotlin.fir.scopes.impl.getOrBuildScopeForIntegerConstantOperatorType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.ClassId

fun FirSmartCastExpression.smartcastScope(
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    requiredMembersPhase: FirResolvePhase? = null,
): FirTypeScope? {
    val smartcastType = smartcastTypeWithoutNullableNothing?.coneType ?: smartcastType.coneType
    val smartcastScope = smartcastType.scope(
        useSiteSession = useSiteSession,
        scopeSession = scopeSession,
        callableCopyTypeCalculator = CallableCopyTypeCalculator.DoNothing,
        requiredMembersPhase = requiredMembersPhase,
    )

    if (isStable) {
        return smartcastScope
    }

    val originalScope = originalExpression.resolvedType.scope(
        useSiteSession = useSiteSession,
        scopeSession = scopeSession,
        callableCopyTypeCalculator = CallableCopyTypeCalculator.DoNothing,
        requiredMembersPhase = requiredMembersPhase,
    ) ?: return smartcastScope

    if (smartcastScope == null) {
        return originalScope
    }
    return FirUnstableSmartcastTypeScope(smartcastScope, originalScope)
}

fun ConeClassLikeType.delegatingConstructorScope(
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    derivedClassLookupTag: ConeClassLikeLookupTag,
    outerType: ConeClassLikeType?
): FirTypeScope? {
    val fir = fullyExpandedType(useSiteSession).lookupTag.toClassSymbol(useSiteSession)?.fir ?: return null

    val substitutor = when {
        outerType != null -> {
            val outerFir = outerType.lookupTag.toClassSymbol(useSiteSession)?.fir ?: return null
            substitutorByMap(
                createSubstitutionForScope(outerFir.typeParameters, outerType, useSiteSession),
                useSiteSession,
            )
        }
        else -> ConeSubstitutor.Empty
    }

    return fir.scopeForClass(substitutor, useSiteSession, scopeSession, derivedClassLookupTag, FirResolvePhase.DECLARATIONS)
}

fun ConeKotlinType.scope(
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    callableCopyTypeCalculator: CallableCopyTypeCalculator,
    requiredMembersPhase: FirResolvePhase?,
): FirTypeScope? {
    val scope = scope(useSiteSession, scopeSession, requiredMembersPhase) ?: return null
    if (callableCopyTypeCalculator == CallableCopyTypeCalculator.DoNothing) return scope
    return FirScopeWithCallableCopyReturnTypeUpdater(scope, callableCopyTypeCalculator)
}

private fun ConeKotlinType.scope(
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    requiredMembersPhase: FirResolvePhase?,
): FirTypeScope? = when (this) {
    is ConeErrorType -> null
    is ConeClassLikeType -> classScope(useSiteSession, scopeSession, requiredMembersPhase, lookupTag)
    is ConeTypeParameterType -> {
        val symbol = lookupTag.symbol
        scopeSession.getOrBuild(symbol, TYPE_PARAMETER_SCOPE_KEY) {
            val intersectionType = ConeTypeIntersector.intersectTypes(
                useSiteSession.typeContext,
                symbol.resolvedBounds.map { it.coneType }
            )

            intersectionType.scope(useSiteSession, scopeSession, requiredMembersPhase) ?: FirTypeScope.Empty
        }
    }
    is ConeRawType -> lowerBound.scope(useSiteSession, scopeSession, requiredMembersPhase)
    is ConeDynamicType -> useSiteSession.dynamicMembersStorage.getDynamicScopeFor(scopeSession)
    is ConeFlexibleType -> lowerBound.scope(useSiteSession, scopeSession, requiredMembersPhase)
    is ConeIntersectionType -> FirTypeIntersectionScope.prepareIntersectionScope(
        useSiteSession,
        FirIntersectionScopeOverrideChecker(useSiteSession),
        intersectedTypes.mapNotNullTo(mutableListOf()) {
            it.scope(useSiteSession, scopeSession, requiredMembersPhase)
        },
        this
    )

    is ConeDefinitelyNotNullType -> original.scope(useSiteSession, scopeSession, requiredMembersPhase)
    is ConeIntegerConstantOperatorType -> scopeSession.getOrBuildScopeForIntegerConstantOperatorType(useSiteSession, this)
    is ConeIntegerLiteralConstantType -> error("ILT should not be in receiver position")
    // See testData/diagnostics/tests/inference/builderInference/memberScopeOfCapturedTypeForPostponedCall.kt
    is ConeCapturedType -> {
        val supertypes =
            constructor.supertypes?.takeIf { it.isNotEmpty() }
                ?: listOf(useSiteSession.builtinTypes.anyType.coneType)
        useSiteSession.typeContext.intersectTypes(supertypes).scope(useSiteSession, scopeSession, requiredMembersPhase)
    }
    else -> null
}

private fun ConeClassLikeType.classScope(
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    requiredMembersPhase: FirResolvePhase?,
    memberOwnerLookupTag: ConeClassLikeLookupTag
): FirTypeScope? {
    val fullyExpandedType = fullyExpandedType(useSiteSession)
    val fir = fullyExpandedType.lookupTag.toClassSymbol(useSiteSession)?.fir ?: return null
    val substitutor = when {
        attributes.contains(CompilerConeAttributes.RawType) -> ConeRawScopeSubstitutor(useSiteSession)
        else -> substitutorByMap(
            createSubstitutionForScope(fir.typeParameters, fullyExpandedType, useSiteSession),
            useSiteSession,
        )
    }

    return fir.scopeForClass(substitutor, useSiteSession, scopeSession, memberOwnerLookupTag, requiredMembersPhase)
}

fun FirClassLikeSymbol<*>.defaultType(): ConeClassLikeType = fir.defaultType()

fun FirClassLikeDeclaration.defaultType(): ConeClassLikeType =
    ConeClassLikeTypeImpl(
        symbol.toLookupTag(),
        typeParameters.map {
            ConeTypeParameterTypeImpl(
                it.symbol.toLookupTag(),
                isMarkedNullable = false
            )
        }.toTypedArray(),
        isMarkedNullable = false
    )

fun ClassId.defaultType(parameters: List<FirTypeParameterSymbol>): ConeClassLikeType =
    ConeClassLikeTypeImpl(
        this.toLookupTag(),
        parameters.map {
            ConeTypeParameterTypeImpl(
                it.toLookupTag(),
                isMarkedNullable = false
            )
        }.toTypedArray(),
        isMarkedNullable = false,
    )

val TYPE_PARAMETER_SCOPE_KEY: ScopeSessionKey<FirTypeParameterSymbol, FirTypeScope> = scopeSessionKey()
