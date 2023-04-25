/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.scopes

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.createSubstitutionForScope
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.scopeSessionKey
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope
import org.jetbrains.kotlin.fir.scopes.jvm.JvmMappedScope
import org.jetbrains.kotlin.fir.scopes.platformClassMapper
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol

fun wrapScopeWithJvmMapped(
    klass: FirClass,
    declaredMemberScope: FirContainingNamesAwareScope,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
    memberRequiredPhase: FirResolvePhase?,
): FirContainingNamesAwareScope {
    val classId = klass.classId
    val kotlinUnsafeFqName = classId.asSingleFqName().toUnsafe()
    val javaClassId = JavaToKotlinClassMap.mapKotlinToJava(kotlinUnsafeFqName)
        ?: return declaredMemberScope
    val symbolProvider = useSiteSession.symbolProvider
    val javaClass = symbolProvider.getClassLikeSymbolByClassId(javaClassId)?.fir as? FirRegularClass
        ?: return declaredMemberScope
    val preparedSignatures = JvmMappedScope.prepareSignatures(javaClass, JavaToKotlinClassMap.isMutable(kotlinUnsafeFqName))
    return if (preparedSignatures.isNotEmpty()) {
        javaClass.unsubstitutedScope(
            useSiteSession,
            scopeSession,
            withForcedTypeCalculator = false,
            memberRequiredPhase = memberRequiredPhase,
        ).let { javaClassUseSiteScope ->
            val jvmMappedScope = JvmMappedScope(
                useSiteSession,
                klass,
                javaClass,
                declaredMemberScope,
                javaClassUseSiteScope,
                preparedSignatures
            )
            if (klass !is FirRegularClass) {
                jvmMappedScope
            } else {
                // We should substitute Java type parameters with base Kotlin type parameters to match overrides properly
                // It's necessary for MutableMap, which has *two* JavaMappedScope inside (one for itself and another for base Map)
                wrapSubstitutionScopeIfNeed(
                    useSiteSession, jvmMappedScope, klass, scopeSession,
                    derivedClass = klass,
                )
            }
        }
    } else {
        declaredMemberScope
    }
}

private fun wrapSubstitutionScopeIfNeed(
    session: FirSession,
    useSiteMemberScope: FirTypeScope,
    declaration: FirClass,
    builder: ScopeSession,
    derivedClass: FirRegularClass
): FirTypeScope {
    if (declaration.typeParameters.isEmpty()) return useSiteMemberScope
    return builder.getOrBuild(declaration.symbol, PLATFORM_TYPE_PARAMETERS_SUBSTITUTION_SCOPE_KEY) {
        val platformClass = session.platformClassMapper.getCorrespondingPlatformClass(declaration) ?: return@getOrBuild useSiteMemberScope
        // This kind of substitution is necessary when method which is mapped from Java (e.g. Java Map.forEach)
        // is called on an external type, like MyMap<String, String>,
        // to determine parameter types properly (e.g. String, String instead of K, V)
        val platformTypeParameters = platformClass.typeParameters
        val platformSubstitution = createSubstitutionForScope(platformTypeParameters, declaration.defaultType(), session)
        val substitutor = substitutorByMap(platformSubstitution, session)
        FirClassSubstitutionScope(
            session, useSiteMemberScope, PLATFORM_TYPE_PARAMETERS_SUBSTITUTION_SCOPE_KEY, substitutor,
            dispatchReceiverTypeForSubstitutedMembers = derivedClass.defaultType(),
            skipPrivateMembers = true,
            derivedClassLookupTag = derivedClass.symbol.toLookupTag()
        )
    }
}

private val PLATFORM_TYPE_PARAMETERS_SUBSTITUTION_SCOPE_KEY = scopeSessionKey<FirClassSymbol<*>, FirTypeScope>()
