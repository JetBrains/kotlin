/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType

class KotlinScopeProvider(
    val declaredMemberScopeDecorator: (
        klass: FirClass<*>,
        declaredMemberScope: FirScope,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ) -> FirScope = { _, declaredMemberScope, _, _ -> declaredMemberScope }
) : FirScopeProvider() {


    private fun substitutor(symbol: FirRegularClassSymbol, type: ConeClassLikeType, useSiteSession: FirSession): ConeSubstitutor {
        if (type.typeArguments.isEmpty()) return ConeSubstitutor.Empty
        val originalSubstitution = createSubstitution(symbol.fir.typeParameters, type.typeArguments, useSiteSession)
        return substitutorByMap(originalSubstitution)
    }

    private fun buildRawUseSiteScope(
        klass: FirClass<*>,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope {
        return scopeSession.getOrBuild(klass.symbol, USE_SITE) {
            val declaredScope = declaredMemberScope(klass)
            val decoratedDeclaredMemberScope =
                declaredMemberScopeDecorator(klass, declaredScope, useSiteSession, scopeSession)

            val scopes = lookupSuperTypes(klass, lookupInterfaces = true, deep = false, useSiteSession = useSiteSession)
                .mapNotNull { useSiteSuperType ->
                    if (useSiteSuperType is ConeClassErrorType) return@mapNotNull null
                    val symbol = useSiteSuperType.lookupTag.toSymbol(useSiteSession)
                    if (symbol is FirRegularClassSymbol) {
                        symbol.fir.scope(substitutor(symbol, useSiteSuperType, useSiteSession), useSiteSession, scopeSession)
                    } else {
                        null
                    }
                }
            FirClassUseSiteMemberScope(
                useSiteSession,
                FirSuperTypeScope.prepareSupertypeScope(useSiteSession, FirStandardOverrideChecker(useSiteSession), scopes),
                decoratedDeclaredMemberScope
            )
        }
    }

    override fun getUseSiteMemberScope(
        klass: FirClass<*>,
        substitutor: ConeSubstitutor,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope {
        if (substitutor == ConeSubstitutor.Empty) return buildRawUseSiteScope(klass, useSiteSession, scopeSession)
        return scopeSession.getOrBuild(
            klass, ConeSubstitutionScopeKey(substitutor)
        ) {
            FirClassSubstitutionScope(useSiteSession, buildRawUseSiteScope(klass, useSiteSession, scopeSession), scopeSession, substitutor)
        }
    }

    override fun getStaticMemberScopeForCallables(
        klass: FirClass<*>,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope? {
        return when (klass.classKind) {
            ClassKind.ENUM_CLASS -> FirStaticScope(declaredMemberScope(klass))
            // TODO: should be wrapped with FirStaticScope, non-static callables should be processed separately
            ClassKind.OBJECT -> getUseSiteMemberScope(klass, ConeSubstitutor.Empty, useSiteSession, scopeSession)
            else -> null
        }
    }
}


data class ConeSubstitutionScopeKey(val substitutor: ConeSubstitutor) : ScopeSessionKey<FirClass<*>, FirClassSubstitutionScope>()

fun FirClass<*>.scope(substitutor: ConeSubstitutor, useSiteSession: FirSession, scopeSession: ScopeSession): FirScope {
    return scopeProvider.getUseSiteMemberScope(
        this, substitutor, useSiteSession, scopeSession
    )
}