/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirNameAwareCompositeScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf

@ThreadSafeMutableState
class FirDeclaredMemberScopeProvider(val useSiteSession: FirSession) : FirSessionComponent {
    private val declaredMemberCache: FirCache<FirClass, FirContainingNamesAwareScope, DeclaredMemberScopeContext> =
        useSiteSession.firCachesFactory.createCache { klass, context ->
            createDeclaredMemberScope(klass, context.useLazyNestedClassifierScope, context.existingNames, context.symbolProvider)
        }

    private val nestedClassifierCache: FirCache<FirClass, FirNestedClassifierScope?, Nothing?> =
        useSiteSession.firCachesFactory.createCache { klass, _ -> createNestedClassifierScope(klass) }

    fun declaredMemberScope(
        klass: FirClass,
        useLazyNestedClassifierScope: Boolean,
        existingNames: List<Name>?,
        symbolProvider: FirSymbolProvider?
    ): FirContainingNamesAwareScope {
        return declaredMemberCache.getValue(klass, DeclaredMemberScopeContext(useLazyNestedClassifierScope, existingNames, symbolProvider))
    }

    private data class DeclaredMemberScopeContext(
        val useLazyNestedClassifierScope: Boolean,
        val existingNames: List<Name>?,
        val symbolProvider: FirSymbolProvider?
    )

    private fun createDeclaredMemberScope(
        klass: FirClass,
        useLazyNestedClassifierScope: Boolean,
        existingNames: List<Name>?,
        symbolProvider: FirSymbolProvider?
    ): FirContainingNamesAwareScope {
        val origin = klass.origin
        return when {
            origin.generated -> {
                FirGeneratedClassDeclaredMemberScope.create(
                    useSiteSession,
                    MemberGenerationContext(klass.symbol, declaredScope = null),
                    needNestedClassifierScope = true
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
                        MemberGenerationContext(klass.symbol, baseScope),
                        needNestedClassifierScope = false
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
        return if (klass.origin.generated) {
            FirGeneratedClassNestedClassifierScope.create(useSiteSession, klass, baseScope = null)
        } else {
            val baseScope = FirNestedClassifierScopeImpl(klass, useSiteSession)
            val generatedScope = FirGeneratedClassNestedClassifierScope.create(useSiteSession, klass, baseScope)
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

fun FirSession.declaredMemberScope(klass: FirClass): FirContainingNamesAwareScope {
    return declaredMemberScopeProvider
        .declaredMemberScope(klass, useLazyNestedClassifierScope = false, existingNames = null, symbolProvider = null)
}

fun FirSession.declaredMemberScope(klass: FirClassSymbol<*>): FirContainingNamesAwareScope {
    return declaredMemberScope(klass.fir)
}

fun FirClassSymbol<*>.declaredMemberScope(session: FirSession): FirContainingNamesAwareScope {
    return session.declaredMemberScope(fir)
}

fun FirSession.declaredMemberScopeWithLazyNestedScope(
    klass: FirClass,
    existingNames: List<Name>,
    symbolProvider: FirSymbolProvider
): FirContainingNamesAwareScope {
    return declaredMemberScopeProvider
        .declaredMemberScope(klass, useLazyNestedClassifierScope = true, existingNames = existingNames, symbolProvider = symbolProvider)
}

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
