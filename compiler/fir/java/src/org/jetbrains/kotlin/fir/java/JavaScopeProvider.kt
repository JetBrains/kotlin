/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.classId
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.scopes.JavaClassEnhancementScope
import org.jetbrains.kotlin.fir.java.scopes.JavaClassUseSiteMemberScope
import org.jetbrains.kotlin.fir.java.scopes.JavaOverrideChecker
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassErrorType

class JavaScopeProvider(
    val declaredMemberScopeDecorator: (
        klass: FirClass<*>,
        declaredMemberScope: FirScope,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ) -> FirScope = { _, declaredMemberScope, _, _ -> declaredMemberScope },
    val symbolProvider: JavaSymbolProvider
) : FirScopeProvider() {
    override fun getUseSiteMemberScope(
        klass: FirClass<*>,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope =
        buildJavaEnhancementScope(useSiteSession, klass.symbol as FirRegularClassSymbol, scopeSession, mutableSetOf())

    private fun buildJavaEnhancementScope(
        useSiteSession: FirSession,
        symbol: FirRegularClassSymbol,
        scopeSession: ScopeSession,
        visitedSymbols: MutableSet<FirClassLikeSymbol<*>>
    ): JavaClassEnhancementScope {
        return scopeSession.getOrBuild(symbol, JAVA_ENHANCEMENT) {
            JavaClassEnhancementScope(
                useSiteSession,
                buildUseSiteMemberScopeWithJavaTypes(symbol.fir, useSiteSession, scopeSession, visitedSymbols)
            )
        }
    }

    private fun buildDeclaredMemberScope(regularClass: FirRegularClass): FirScope {
        return if (regularClass is FirJavaClass) declaredMemberScopeWithLazyNestedScope(
            regularClass,
            existingNames = regularClass.existingNestedClassifierNames,
            symbolProvider = symbolProvider
        ) else declaredMemberScope(regularClass)
    }

    private fun buildUseSiteMemberScopeWithJavaTypes(
        regularClass: FirRegularClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession,
        visitedSymbols: MutableSet<FirClassLikeSymbol<*>>
    ): JavaClassUseSiteMemberScope {
        return scopeSession.getOrBuild(regularClass.symbol, JAVA_USE_SITE) {
            val declaredScope = buildDeclaredMemberScope(regularClass)
            val wrappedDeclaredScope = declaredMemberScopeDecorator(regularClass, declaredScope, useSiteSession, scopeSession)
            val superTypeEnhancementScopes =
                lookupSuperTypes(regularClass, lookupInterfaces = true, deep = false, useSiteSession = useSiteSession)
                    .mapNotNull { useSiteSuperType ->
                        if (useSiteSuperType is ConeClassErrorType) return@mapNotNull null
                        val symbol = useSiteSuperType.lookupTag.toSymbol(useSiteSession)
                        if (symbol is FirRegularClassSymbol && visitedSymbols.add(symbol)) {
                            // We need JavaClassEnhancementScope here to have already enhanced signatures from supertypes
                            val scope = buildJavaEnhancementScope(useSiteSession, symbol, scopeSession, visitedSymbols)
                            visitedSymbols.remove(symbol)
                            useSiteSuperType.wrapSubstitutionScopeIfNeed(
                                useSiteSession, scope, symbol.fir, scopeSession, regularClass.classId
                            )
                        } else {
                            null
                        }
                    }
            JavaClassUseSiteMemberScope(
                regularClass, useSiteSession,
                FirSuperTypeScope.prepareSupertypeScope(
                    useSiteSession,
                    JavaOverrideChecker(
                        useSiteSession,
                        if (regularClass is FirJavaClass) regularClass.javaTypeParameterStack
                        else JavaTypeParameterStack.EMPTY
                    ),
                    superTypeEnhancementScopes
                ), wrappedDeclaredScope
            )
        }
    }

    override fun getStaticMemberScopeForCallables(
        klass: FirClass<*>,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope? {
        if (klass !is FirRegularClass) return null
        val enhancementScope = scopeSession.getOrBuild(klass.symbol, JAVA_ENHANCEMENT_FOR_STATIC) {
            val declaredScope = buildDeclaredMemberScope(klass)
            val wrappedDeclaredScope = declaredMemberScopeDecorator(klass, declaredScope, useSiteSession, scopeSession)
            JavaClassEnhancementScope(
                useSiteSession, JavaClassUseSiteMemberScope(
                    klass, useSiteSession, superTypesScope = object : FirScope() {}, declaredMemberScope = wrappedDeclaredScope
                )
            )
        }
        return FirOnlyCallablesScope(FirStaticScope(enhancementScope))
    }

    override fun getNestedClassifierScope(klass: FirClass<*>, useSiteSession: FirSession, scopeSession: ScopeSession): FirScope? {
        return lazyNestedClassifierScope(
            klass.classId,
            (klass as FirJavaClass).existingNestedClassifierNames,
            useSiteSession.firSymbolProvider
        )
    }
}

private val JAVA_ENHANCEMENT_FOR_STATIC = scopeSessionKey<FirRegularClassSymbol, JavaClassEnhancementScope>()
private val JAVA_ENHANCEMENT = scopeSessionKey<FirRegularClassSymbol, JavaClassEnhancementScope>()
private val JAVA_USE_SITE = scopeSessionKey<FirRegularClassSymbol, JavaClassUseSiteMemberScope>()
