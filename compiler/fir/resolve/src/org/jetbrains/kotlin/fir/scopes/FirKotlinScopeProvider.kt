/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*

class FirKotlinScopeProvider(
    val declaredMemberScopeDecorator: (
        klass: FirClass<*>,
        declaredMemberScope: FirScope,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ) -> FirScope = { _, declaredMemberScope, _, _ -> declaredMemberScope }
) : FirScopeProvider() {
    override fun getUseSiteMemberScope(
        klass: FirClass<*>,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirTypeScope {
        return scopeSession.getOrBuild(klass.symbol, USE_SITE) {
            val declaredScope = useSiteSession.declaredMemberScope(klass)
            val decoratedDeclaredMemberScope =
                declaredMemberScopeDecorator(klass, declaredScope, useSiteSession, scopeSession)

            val delegateFields = klass.declarations.filterIsInstance<FirField>().filter { it.isSynthetic }
            val scopes = lookupSuperTypes(klass, lookupInterfaces = true, deep = false, useSiteSession = useSiteSession)
                .mapNotNull { useSiteSuperType ->
                    useSiteSuperType.scopeForSupertype(useSiteSession, scopeSession, klass, decoratedDeclaredMemberScope, delegateFields)
                }
            FirClassUseSiteMemberScope(
                useSiteSession,
                FirTypeIntersectionScope.prepareIntersectionScope(
                    useSiteSession, FirStandardOverrideChecker(useSiteSession), scopes,
                    klass.defaultType(),
                ),
                decoratedDeclaredMemberScope,
            )
        }
    }

    override fun getStaticMemberScopeForCallables(
        klass: FirClass<*>,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope? {
        return when (klass.classKind) {
            ClassKind.ENUM_CLASS -> FirOnlyCallablesScope(FirStaticScope(useSiteSession.declaredMemberScope(klass)))
            else -> null
        }
    }

    override fun getNestedClassifierScope(klass: FirClass<*>, useSiteSession: FirSession, scopeSession: ScopeSession): FirScope? {
        return useSiteSession.nestedClassifierScope(klass)
    }
}


data class ConeSubstitutionScopeKey(
    val lookupTag: ConeClassLikeLookupTag, val isFromExpectClass: Boolean, val substitutor: ConeSubstitutor
) : ScopeSessionKey<FirClass<*>, FirClassSubstitutionScope>()

data class DelegatedMemberScopeKey(val callableId: CallableId) : ScopeSessionKey<FirField, FirDelegatedMemberScope>()

fun FirClass<*>.unsubstitutedScope(
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    withForcedTypeCalculator: Boolean
): FirTypeScope {
    val scope = scopeProvider.getUseSiteMemberScope(this, useSiteSession, scopeSession)
    if (withForcedTypeCalculator) return FirScopeWithFakeOverrideTypeCalculator(scope, FakeOverrideTypeCalculator.Forced)
    return scope
}

fun FirClass<*>.scopeForClass(
    substitutor: ConeSubstitutor,
    useSiteSession: FirSession,
    scopeSession: ScopeSession
): FirTypeScope = scopeForClassImpl(
    substitutor, useSiteSession, scopeSession,
    skipPrivateMembers = false,
    classFirDispatchReceiver = this,
    // TODO: why it's always false?
    isFromExpectClass = false
)

fun ConeKotlinType.scopeForSupertype(
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    subClass: FirClass<*>,
    declaredMemberScope: FirScope,
    delegateFields: List<FirField>?,
): FirTypeScope? {
    if (this !is ConeClassLikeType) return null
    if (this is ConeClassErrorType) return null
    val symbol = lookupTag.toSymbol(useSiteSession)
    return if (symbol is FirRegularClassSymbol) {
        val delegateField = delegateFields?.find { useSiteSession.typeContext.equalTypes(it.returnTypeRef.coneType, this) }
        symbol.fir.scopeForSupertype(
            substitutor(symbol, this, useSiteSession),
            useSiteSession, scopeSession, delegateField,
            subClass = subClass,
            declaredMemberScope
        ).let {
            it as? FirTypeScope ?: error("$it is expected to be FirOverrideAwareScope")
        }
    } else {
        null
    }
}

private fun substitutor(symbol: FirRegularClassSymbol, type: ConeClassLikeType, useSiteSession: FirSession): ConeSubstitutor {
    if (type.typeArguments.isEmpty()) return ConeSubstitutor.Empty
    val originalSubstitution = createSubstitution(symbol.fir.typeParameters, type, useSiteSession)
    return substitutorByMap(originalSubstitution, useSiteSession)
}

fun FirClass<*>.scopeForSupertype(
    substitutor: ConeSubstitutor,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    delegateField: FirField?,
    subClass: FirClass<*>,
    declaredMemberScope: FirScope,
): FirTypeScope = scopeForClassImpl(
    substitutor,
    useSiteSession,
    scopeSession,
    skipPrivateMembers = true,
    classFirDispatchReceiver = subClass,
    isFromExpectClass = (subClass as? FirRegularClass)?.isExpect == true
).let {
    if (delegateField != null) {
        scopeSession.getOrBuild(delegateField, DelegatedMemberScopeKey(delegateField.symbol.callableId)) {
            FirDelegatedMemberScope(it, useSiteSession, subClass, delegateField, declaredMemberScope)
        }
    } else {
        it
    }
}

private fun FirClass<*>.scopeForClassImpl(
    substitutor: ConeSubstitutor,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    skipPrivateMembers: Boolean,
    classFirDispatchReceiver: FirClass<*>,
    isFromExpectClass: Boolean
): FirTypeScope {
    val basicScope = unsubstitutedScope(useSiteSession, scopeSession, withForcedTypeCalculator = false)
    if (substitutor == ConeSubstitutor.Empty) return basicScope

    return scopeSession.getOrBuild(
        this, ConeSubstitutionScopeKey(classFirDispatchReceiver.symbol.toLookupTag(), isFromExpectClass, substitutor)
    ) {
        FirClassSubstitutionScope(
            useSiteSession, basicScope,  substitutor, classFirDispatchReceiver.defaultType(),
            skipPrivateMembers, makeExpect = isFromExpectClass
        )
    }
}
