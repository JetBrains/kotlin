/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.extensions.FirDeclarationsForMetadataProviderExtension
import org.jetbrains.kotlin.fir.extensions.declarationForMetadataProviders
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.FqName

abstract class FirProvidedDeclarationsForMetadataService : FirSessionComponent {
    companion object {
        fun create(session: FirSession): FirProvidedDeclarationsForMetadataService {
            val extensionProviders = session.extensionService.declarationForMetadataProviders
            return if (extensionProviders.isEmpty()) Empty else FirProvidedDeclarationsForMetadataServiceImpl(session, extensionProviders)
        }
    }

    abstract fun getProvidedTopLevelDeclarations(packageFqName: FqName, scopeSession: ScopeSession): List<FirDeclaration>
    abstract fun getProvidedConstructors(owner: FirClassSymbol<*>, scopeSession: ScopeSession): List<FirConstructor>
    abstract fun getProvidedCallables(owner: FirClassSymbol<*>, scopeSession: ScopeSession): List<FirCallableDeclaration>
    abstract fun getProvidedNestedClassifiers(owner: FirClassSymbol<*>, scopeSession: ScopeSession): List<FirClassLikeSymbol<*>>

    private object Empty : FirProvidedDeclarationsForMetadataService() {
        override fun getProvidedTopLevelDeclarations(packageFqName: FqName, scopeSession: ScopeSession): List<FirDeclaration> {
            return emptyList()
        }

        override fun getProvidedConstructors(owner: FirClassSymbol<*>, scopeSession: ScopeSession): List<FirConstructor> {
            return emptyList()
        }

        override fun getProvidedCallables(owner: FirClassSymbol<*>, scopeSession: ScopeSession): List<FirCallableDeclaration> {
            return emptyList()
        }

        override fun getProvidedNestedClassifiers(owner: FirClassSymbol<*>, scopeSession: ScopeSession): List<FirClassLikeSymbol<*>> {
            return emptyList()
        }
    }
}

private class FirProvidedDeclarationsForMetadataServiceImpl(
    session: FirSession,
    private val extensionDeclarationProviders: List<FirDeclarationsForMetadataProviderExtension>
) : FirProvidedDeclarationsForMetadataService() {
    private val cachesFactory = session.firCachesFactory

    private val topLevelsCache: FirCache<FqName, List<FirDeclaration>, ScopeSession> =
        cachesFactory.createCache(::computeTopLevelDeclarations)

    private val membersCache: FirCache<FirClassSymbol<*>, ClassDeclarations, ScopeSession> =
        cachesFactory.createCache(::computeMemberDeclarations)

    private fun computeTopLevelDeclarations(packageFqName: FqName, scopeSession: ScopeSession): List<FirDeclaration> {
        return buildList {
            for (extensionProvider in extensionDeclarationProviders) {
                for (declaration in extensionProvider.provideTopLevelDeclarations(packageFqName, scopeSession)) {
                    add(declaration)
                }
            }
        }
    }

    override fun getProvidedTopLevelDeclarations(packageFqName: FqName, scopeSession: ScopeSession): List<FirDeclaration> {
        return topLevelsCache.getValue(packageFqName, scopeSession)
    }

    override fun getProvidedConstructors(owner: FirClassSymbol<*>, scopeSession: ScopeSession): List<FirConstructor> {
        return membersCache.getValue(owner, scopeSession).providedConstructors
    }

    override fun getProvidedCallables(owner: FirClassSymbol<*>, scopeSession: ScopeSession): List<FirCallableDeclaration> {
        return membersCache.getValue(owner, scopeSession).providedCallables
    }

    override fun getProvidedNestedClassifiers(owner: FirClassSymbol<*>, scopeSession: ScopeSession): List<FirClassLikeSymbol<*>> {
        return membersCache.getValue(owner, scopeSession).providedNestedClasses
    }

    private data class ClassDeclarations(
        val providedCallables: List<FirCallableDeclaration>,
        val providedConstructors: List<FirConstructor>,
        val providedNestedClasses: List<FirClassLikeSymbol<*>>,
    )

    private fun computeMemberDeclarations(symbol: FirClassSymbol<*>, scopeSession: ScopeSession): ClassDeclarations {
        val providedCallables = mutableListOf<FirCallableDeclaration>()
        val providedConstructors = mutableListOf<FirConstructor>()
        val providedNestedClassifiers = mutableListOf<FirClassLikeSymbol<*>>()

        for (extensionProvider in extensionDeclarationProviders) {
            for (declaration in extensionProvider.provideDeclarationsForClass(symbol.fir, scopeSession)) {
                when (declaration) {
                    is FirConstructor -> providedConstructors += declaration
                    is FirCallableDeclaration -> providedCallables += declaration
                    is FirClassLikeDeclaration -> providedNestedClassifiers += declaration.symbol
                    else -> error("Unsupported declaration type in: $symbol ${declaration.render()}")
                }
            }
        }

        return ClassDeclarations(providedCallables, providedConstructors, providedNestedClassifiers)
    }
}

val FirSession.providedDeclarationsForMetadataService: FirProvidedDeclarationsForMetadataService by FirSession.sessionComponentAccessor()
