/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.extensions.FirDeclarationsForMetadataProviderExtension
import org.jetbrains.kotlin.fir.extensions.declarationForMetadataProviders
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.toFirRegularClass
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.getOrPut
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

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

    abstract fun registerDeclaration(declaration: FirCallableDeclaration)

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

        override fun registerDeclaration(declaration: FirCallableDeclaration) {
            shouldNotBeCalled()
        }
    }
}

private class FirProvidedDeclarationsForMetadataServiceImpl(
    private val session: FirSession,
    private val extensionDeclarationProviders: List<FirDeclarationsForMetadataProviderExtension>
) : FirProvidedDeclarationsForMetadataService() {
    private val topLevelsCache: MutableMap<FqName, MutableList<FirDeclaration>> =
        mutableMapOf()

    private val memberCache: MutableMap<FirClassSymbol<*>, ClassDeclarations> =
        mutableMapOf()

    override fun registerDeclaration(declaration: FirCallableDeclaration) {
        val containingClass = declaration.containingClassLookupTag()?.toFirRegularClass(session)
        if (containingClass == null) {
            val list = topLevelsCache.getOrPut(declaration.symbol.callableId.packageName) { mutableListOf() }
            list += declaration
        } else {
            val declarations = memberCache.getOrPut(containingClass.symbol) { ClassDeclarations() }
            when (declaration) {
                is FirConstructor -> declarations.providedConstructors += declaration
                else -> declarations.providedCallables += declaration
            }
        }
    }

    override fun getProvidedTopLevelDeclarations(packageFqName: FqName, scopeSession: ScopeSession): List<FirDeclaration> {
        return topLevelsCache[packageFqName] ?: emptyList()
    }

    override fun getProvidedConstructors(owner: FirClassSymbol<*>, scopeSession: ScopeSession): List<FirConstructor> {
        return memberCache[owner]?.providedConstructors ?: emptyList()
    }

    override fun getProvidedCallables(owner: FirClassSymbol<*>, scopeSession: ScopeSession): List<FirCallableDeclaration> {
        return memberCache[owner]?.providedCallables ?: emptyList()
    }

    override fun getProvidedNestedClassifiers(owner: FirClassSymbol<*>, scopeSession: ScopeSession): List<FirClassLikeSymbol<*>> {
        // TODO: remove
        return emptyList()
    }

    private class ClassDeclarations {
        val providedCallables: MutableList<FirCallableDeclaration> = mutableListOf()
        val providedConstructors: MutableList<FirConstructor> = mutableListOf()
    }
}

val FirSession.providedDeclarationsForMetadataService: FirProvidedDeclarationsForMetadataService by FirSession.sessionComponentAccessor()
