/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.resolve.declaredMemberScopeProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

@ThreadSafeMutableState
class FirDeclaredMemberScopeProvider(val useSiteSession: FirSession) : FirSessionComponent {
    private val declaredMemberCache: FirCache<FirClass, FirClassDeclaredMemberScope, DeclaredMemberScopeContext> =
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
    ): FirClassDeclaredMemberScope {
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
    ): FirClassDeclaredMemberScope {
        return if (klass.origin.generated) {
            FirGeneratedClassDeclaredMemberScope(useSiteSession, klass)
        } else {
            FirClassDeclaredMemberScopeImpl(useSiteSession, klass, useLazyNestedClassifierScope, existingNames, symbolProvider)
        }
    }

    fun nestedClassifierScope(klass: FirClass): FirNestedClassifierScope? {
        return nestedClassifierCache.getValue(klass)
    }

    private fun createNestedClassifierScope(klass: FirClass): FirNestedClassifierScope? {
        return if (klass.origin.generated) {
            FirGeneratedClassNestedClassifierScope(klass, useSiteSession)
        } else {
            FirNestedClassifierScopeImpl(klass, useSiteSession)
        }.takeUnless { it.isEmpty() }
    }
}

fun FirSession.declaredMemberScope(klass: FirClass): FirClassDeclaredMemberScope {
    return declaredMemberScopeProvider
        .declaredMemberScope(klass, useLazyNestedClassifierScope = false, existingNames = null, symbolProvider = null)
}

fun FirSession.declaredMemberScope(klass: FirClassSymbol<*>): FirClassDeclaredMemberScope {
    return declaredMemberScope(klass.fir)
}

fun FirSession.declaredMemberScopeWithLazyNestedScope(
    klass: FirClass,
    existingNames: List<Name>,
    symbolProvider: FirSymbolProvider
): FirScope {
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
