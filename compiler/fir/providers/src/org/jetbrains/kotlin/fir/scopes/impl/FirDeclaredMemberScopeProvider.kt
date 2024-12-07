/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirNameAwareCompositeScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseWithCallableMembers
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import kotlin.time.Duration.Companion.seconds

/**
 * Caches [declared member scopes][FirContainingNamesAwareScope] and [nested classifier scopes][FirNestedClassifierScope] for [FirClass]es.
 *
 * ### Caching strategy
 *
 * The provider uses caches with suggested limits. In the compiler, the cache will simply be an unlimited cache. In the Analysis API,
 * however, these limits will be applied.
 *
 * Scope values are expired after they haven't been accessed for a few seconds. This keeps the cache lean and avoids accumulation of unused
 * scopes. Because this expiration is only checked when the cache is accessed, scope values are additionally held on soft references so that
 * they can be collected under memory pressure.
 */
@ThreadSafeMutableState
class FirDeclaredMemberScopeProvider(val useSiteSession: FirSession) : FirSessionComponent {
    private val declaredMemberCache: FirCache<FirClass, FirContainingNamesAwareScope, DeclaredMemberScopeContext> =
        useSiteSession.firCachesFactory.createCacheWithSuggestedLimits(
            expirationAfterAccess = 5.seconds,
            valueStrength = FirCachesFactory.ValueReferenceStrength.SOFT,
        ) { klass, context ->
            createDeclaredMemberScope(klass = klass, existingNamesForLazyNestedClassifierScope = context.existingNames)
        }

    private val nestedClassifierCache: FirCache<FirClass, FirNestedClassifierScope?, Nothing?> =
        useSiteSession.firCachesFactory.createCacheWithSuggestedLimits(
            expirationAfterAccess = 5.seconds,
            valueStrength = FirCachesFactory.ValueReferenceStrength.SOFT,
        ) { klass, _ ->
            createNestedClassifierScope(klass)
        }

    fun declaredMemberScope(
        klass: FirClass,
        useLazyNestedClassifierScope: Boolean,
        existingNames: List<Name>?,
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
            )
        )
    }

    private data class DeclaredMemberScopeContext(
        val useLazyNestedClassifierScope: Boolean,
        val existingNames: List<Name>?,
    )

    private fun createDeclaredMemberScope(
        klass: FirClass,
        existingNamesForLazyNestedClassifierScope: List<Name>?,
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
                val baseScope = FirClassDeclaredMemberScopeImpl(useSiteSession, klass, existingNamesForLazyNestedClassifierScope)
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
): FirContainingNamesAwareScope = declaredMemberScopeProvider.declaredMemberScope(
    klass = klass,
    useLazyNestedClassifierScope = true,
    existingNames = existingNames,
    memberRequiredPhase = null,
)

fun FirSession.nestedClassifierScope(klass: FirClass): FirNestedClassifierScope? {
    return declaredMemberScopeProvider.nestedClassifierScope(klass)
}

fun lazyNestedClassifierScope(
    session: FirSession,
    classId: ClassId,
    existingNames: List<Name>,
): FirLazyNestedClassifierScope? {
    if (existingNames.isEmpty()) return null
    return FirLazyNestedClassifierScope(session, classId, existingNames)
}

val FirSession.declaredMemberScopeProvider: FirDeclaredMemberScopeProvider by FirSession.sessionComponentAccessor()
