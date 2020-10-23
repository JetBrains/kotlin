/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.scopes

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.classId
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.wrapSubstitutionScopeIfNeed
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.jvm.JvmMappedScope
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl


fun wrapScopeWithJvmMapped(
    klass: FirClass<*>,
    declaredMemberScope: FirScope,
    useSiteSession: FirSession,
    scopeSession: ScopeSession
): FirScope {
    val classId = klass.classId
    val kotlinUnsafeFqName = classId.asSingleFqName().toUnsafe()
    val javaClassId = JavaToKotlinClassMap.mapKotlinToJava(kotlinUnsafeFqName)
        ?: return declaredMemberScope
    val symbolProvider = useSiteSession.firSymbolProvider
    val javaClass = symbolProvider.getClassLikeSymbolByFqName(javaClassId)?.fir as? FirRegularClass
        ?: return declaredMemberScope
    val preparedSignatures = JvmMappedScope.prepareSignatures(javaClass, JavaToKotlinClassMap.isMutable(kotlinUnsafeFqName))
    return if (preparedSignatures.isNotEmpty()) {
        javaClass.unsubstitutedScope(useSiteSession, scopeSession, withForcedTypeCalculator = false).let { javaClassUseSiteScope ->
            val jvmMappedScope = JvmMappedScope(declaredMemberScope, javaClassUseSiteScope, preparedSignatures)
            if (klass !is FirRegularClass) {
                jvmMappedScope
            } else {
                // We should substitute Java type parameters with base Kotlin type parameters to match overrides properly
                // It's necessary for MutableMap, which has *two* JavaMappedScope inside (one for itself and another for base Map)
                (klass.symbol.constructType(
                    klass.typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false) }.toTypedArray(),
                    false
                ) as ConeClassLikeType).wrapSubstitutionScopeIfNeed(
                    useSiteSession, jvmMappedScope, klass, scopeSession,
                    derivedClass = klass,
                )
            }
        }
    } else {
        declaredMemberScope
    }
}
