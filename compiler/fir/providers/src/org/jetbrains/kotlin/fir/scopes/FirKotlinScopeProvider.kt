/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.delegateFields
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeRawScopeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*

class FirKotlinScopeProvider(
    val declaredMemberScopeDecorator: (
        klass: FirClass,
        declaredMemberScope: FirContainingNamesAwareScope,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ) -> FirContainingNamesAwareScope = { _, declaredMemberScope, _, _ -> declaredMemberScope }
) : FirScopeProvider(), FirSessionComponent {
    override fun getUseSiteMemberScope(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirTypeScope {
        return scopeSession.getOrBuild(klass.symbol, USE_SITE) {
            val declaredScope = useSiteSession.declaredMemberScope(klass)

            val decoratedDeclaredMemberScope =
                declaredMemberScopeDecorator(klass, declaredScope, useSiteSession, scopeSession).let {
                    val delegateFields = klass.delegateFields
                    if (delegateFields.isEmpty())
                        it
                    else
                        FirDelegatedMemberScope(useSiteSession, scopeSession, klass, it, delegateFields)
                }

            val scopes = lookupSuperTypes(
                klass, lookupInterfaces = true, deep = false, useSiteSession = useSiteSession, substituteTypes = true
            ).mapNotNull { useSiteSuperType ->
                useSiteSuperType.scopeForSupertype(useSiteSession, scopeSession, klass)
            }
            FirClassUseSiteMemberScope(
                klass,
                useSiteSession,
                scopes,
                decoratedDeclaredMemberScope,
            )
        }
    }

    override fun getStaticMemberScopeForCallables(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirContainingNamesAwareScope? {
        return when (klass.classKind) {
            ClassKind.ENUM_CLASS -> FirNameAwareOnlyCallablesScope(FirStaticScope(useSiteSession.declaredMemberScope(klass)))
            else -> null
        }
    }

    override fun getNestedClassifierScope(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirContainingNamesAwareScope? {
        return useSiteSession.nestedClassifierScope(klass)
    }
}


data class ConeSubstitutionScopeKey(
    val lookupTag: ConeClassLikeLookupTag, val isFromExpectClass: Boolean, val substitutor: ConeSubstitutor
) : ScopeSessionKey<FirClass, FirClassSubstitutionScope>()

fun FirClass.unsubstitutedScope(
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    withForcedTypeCalculator: Boolean
): FirTypeScope {
    val scope = scopeProvider.getUseSiteMemberScope(this, useSiteSession, scopeSession)
    if (withForcedTypeCalculator) return FirScopeWithFakeOverrideTypeCalculator(scope, FakeOverrideTypeCalculator.Forced)
    return scope
}

fun FirClassSymbol<*>.unsubstitutedScope(
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    withForcedTypeCalculator: Boolean
): FirTypeScope {
    return fir.unsubstitutedScope(useSiteSession, scopeSession, withForcedTypeCalculator)
}

fun FirClass.scopeForClass(
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
    subClass: FirClass,
): FirTypeScope? {
    if (this !is ConeClassLikeType) return null
    if (this is ConeErrorType) return null

    val symbol = lookupTag.toSymbol(useSiteSession) as? FirRegularClassSymbol ?: return null

    val substitutor = when {
        this.type.attributes.contains(CompilerConeAttributes.RawType) -> ConeRawScopeSubstitutor(useSiteSession)
        else -> substitutor(symbol, this, useSiteSession)
    }

    return symbol.fir.scopeForClassImpl(
        substitutor,
        useSiteSession,
        scopeSession,
        skipPrivateMembers = true,
        classFirDispatchReceiver = subClass,
        isFromExpectClass = (subClass as? FirRegularClass)?.isExpect == true
    )
}

private fun substitutor(symbol: FirRegularClassSymbol, type: ConeClassLikeType, useSiteSession: FirSession): ConeSubstitutor {
    if (type.typeArguments.isEmpty()) return ConeSubstitutor.Empty
    val originalSubstitution = createSubstitution(symbol.fir.typeParameters, type, useSiteSession)
    return substitutorByMap(originalSubstitution, useSiteSession)
}

private fun FirClass.scopeForClassImpl(
    substitutor: ConeSubstitutor,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    skipPrivateMembers: Boolean,
    classFirDispatchReceiver: FirClass,
    isFromExpectClass: Boolean
): FirTypeScope {
    val basicScope = unsubstitutedScope(useSiteSession, scopeSession, withForcedTypeCalculator = false)
    if (substitutor == ConeSubstitutor.Empty) return basicScope

    val key = ConeSubstitutionScopeKey(classFirDispatchReceiver.symbol.toLookupTag(), isFromExpectClass, substitutor)
    return scopeSession.getOrBuild(
        this, key
    ) {
        FirClassSubstitutionScope(
            useSiteSession,
            basicScope,
            key, substitutor,
            substitutor.substituteOrSelf(classFirDispatchReceiver.defaultType()).lowerBoundIfFlexible() as ConeClassLikeType,
            skipPrivateMembers,
            makeExpect = isFromExpectClass
        )
    }
}

val FirSession.kotlinScopeProvider: FirKotlinScopeProvider by FirSession.sessionComponentAccessor()
