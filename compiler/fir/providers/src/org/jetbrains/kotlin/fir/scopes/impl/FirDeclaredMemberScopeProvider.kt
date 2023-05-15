/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirNameAwareCompositeScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseWithCallableMembers
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf

@ThreadSafeMutableState
class FirDeclaredMemberScopeProvider(val useSiteSession: FirSession) : FirSessionComponent {
    private val declaredMemberCache: FirCache<FirClass, FirContainingNamesAwareScope, DeclaredMemberScopeContext> =
        useSiteSession.firCachesFactory.createCache { klass, context ->
            createDeclaredMemberScope(
                klass = klass,
                useLazyNestedClassifierScope = context.useLazyNestedClassifierScope,
                existingNames = context.existingNames,
                symbolProvider = context.symbolProvider,
            )
        }

    private val nestedClassifierCache: FirCache<FirClass, FirNestedClassifierScope?, Nothing?> =
        useSiteSession.firCachesFactory.createCache { klass, _ -> createNestedClassifierScope(klass) }

    fun declaredMemberScope(
        klass: FirClass,
        useLazyNestedClassifierScope: Boolean,
        existingNames: List<Name>?,
        symbolProvider: FirSymbolProvider?,
        memberRequiredPhase: FirResolvePhase?,
    ): FirContainingNamesAwareScope {
        memberRequiredPhase?.let {
            klass.lazyResolveToPhaseWithCallableMembers(it)
        }

        return declaredMemberCache.getValue(
            klass,
            DeclaredMemberScopeContext(
                useLazyNestedClassifierScope,
                existingNames,
                symbolProvider,
            )
        )
    }

    private data class DeclaredMemberScopeContext(
        val useLazyNestedClassifierScope: Boolean,
        val existingNames: List<Name>?,
        val symbolProvider: FirSymbolProvider?,
    )

    private fun createDeclaredMemberScope(
        klass: FirClass,
        useLazyNestedClassifierScope: Boolean,
        existingNames: List<Name>?,
        symbolProvider: FirSymbolProvider?,
    ): FirContainingNamesAwareScope {
        val origin = klass.origin
        return when {
            origin.generated -> {
                FirGeneratedClassDeclaredMemberScope.create(
                    useSiteSession,
                    klass.symbol,
                    regularDeclaredScope = null,
                    scopeForGeneratedClass = true
                ) ?: FirTypeScope.Empty
            }
            else -> {
                val baseScope = FirClassDeclaredMemberScopeImpl(
                    useSiteSession,
                    klass,
                    useLazyNestedClassifierScope,
                    existingNames,
                    symbolProvider
                )
                val generatedScope = runIf(origin.fromSource || origin.generated) {
                    FirGeneratedClassDeclaredMemberScope.create(
                        useSiteSession,
                        klass.symbol,
                        regularDeclaredScope = baseScope,
                        scopeForGeneratedClass = false
                    )
                }
                if (generatedScope != null) {
                    FirNameAwareCompositeScope(listOf(baseScope, generatedScope))
                } else {
                    baseScope
                }
            }
        }
    }

    fun nestedClassifierScope(klass: FirClass): FirNestedClassifierScope? {
        return nestedClassifierCache.getValue(klass)
    }

    private fun createNestedClassifierScope(klass: FirClass): FirNestedClassifierScope? {
        val origin = klass.origin
        return if (origin.generated) {
            FirGeneratedClassNestedClassifierScope.create(useSiteSession, klass.symbol, regularNestedClassifierScope = null)
        } else {
            val baseScope = FirNestedClassifierScopeImpl(klass, useSiteSession)
            val generatedScope = runIf(origin.fromSource) {
                FirGeneratedClassNestedClassifierScope.create(useSiteSession, klass.symbol, regularNestedClassifierScope = baseScope)
            }
            if (generatedScope != null) {
                FirCompositeNestedClassifierScope(
                    listOf(baseScope, generatedScope),
                    klass,
                    useSiteSession
                )
            } else {
                baseScope
            }
        }?.takeUnless { it.isEmpty() }
    }
}

fun FirSession.declaredMemberScope(klass: FirClass, memberRequiredPhase: FirResolvePhase?): FirContainingNamesAwareScope {
    return declaredMemberScopeProvider.declaredMemberScope(
        klass = klass,
        useLazyNestedClassifierScope = false,
        existingNames = null,
        symbolProvider = null,
        memberRequiredPhase = memberRequiredPhase,
    )
}

fun FirSession.declaredMemberScope(klass: FirClassSymbol<*>, memberRequiredPhase: FirResolvePhase?): FirContainingNamesAwareScope {
    return declaredMemberScope(klass.fir, memberRequiredPhase)
}

fun FirClassSymbol<*>.declaredMemberScope(session: FirSession, memberRequiredPhase: FirResolvePhase?): FirContainingNamesAwareScope {
    return session.declaredMemberScope(fir, memberRequiredPhase)
}

fun FirSession.declaredMemberScopeWithLazyNestedScope(
    klass: FirClass,
    existingNames: List<Name>,
    symbolProvider: FirSymbolProvider
): FirContainingNamesAwareScope = declaredMemberScopeProvider.declaredMemberScope(
    klass = klass,
    useLazyNestedClassifierScope = true,
    existingNames = existingNames,
    symbolProvider = symbolProvider,
    memberRequiredPhase = null,
)

fun FirSession.nestedClassifierScope(klass: FirClass): FirNestedClassifierScope? {
    return declaredMemberScopeProvider
        .nestedClassifierScope(klass)
}

fun lazyNestedClassifierScope(
    classId: ClassId,
    existingNames: List<Name>,
    symbolProvider: FirSymbolProvider
): FirLazyNestedClassifierScope? {
    if (existingNames.isEmpty()) return null
    return FirLazyNestedClassifierScope(classId, existingNames, symbolProvider)
}

val FirSession.declaredMemberScopeProvider: FirDeclaredMemberScopeProvider by FirSession.sessionComponentAccessor()
