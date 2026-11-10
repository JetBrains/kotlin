/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import kotlinx.collections.immutable.PersistentList
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingReplSymbolAttr
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.SupertypeSupplier
import org.jetbrains.kotlin.fir.resolve.TypeResolutionConfiguration
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirSpecificTypeResolverTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.FirSupertypeScopesProxy
import org.jetbrains.kotlin.fir.resolve.transformers.ReplSnippetProvider
import org.jetbrains.kotlin.fir.resolve.transformers.SupertypesForOuterClassResolver
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReplSnippetSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe

@FirExtensionApiInternals
class FirTypeResolveServiceProvider(
    private val session: FirSession,
) : TypeResolveServiceProvider(), SupertypesForOuterClassResolver, ReplSnippetProvider {
    private val scopeSession: ScopeSession = ScopeSession()

    private val supertypeSupplier = object : SupertypeSupplier() {
        override fun forClass(firClass: FirClass, useSiteSession: FirSession): List<ConeClassLikeType> {
            return firClass.superTypeRefs.mapNotNull { it.coneTypeSafe<ConeClassLikeType>() }
        }

        override fun expansionForTypeAlias(typeAlias: FirTypeAlias, useSiteSession: FirSession): ConeClassLikeType? {
            return typeAlias.expandedTypeRef.coneTypeSafe()
        }
    }

    private val proxy = FirSupertypeScopesProxy(
        session,
        null,
        scopeSession,
        null,
        null,
        supertypeSupplier,
        this,
        this,
    )

    override fun replSnippet(declaration: FirClassLikeDeclaration): FirReplSnippetSymbol? {
        return declaration.containingReplSymbolAttr
    }

    override fun resolveAllSupertypesForOuterClass(outerClass: FirClass) {
        // do nothing
    }

    override fun getTypeResolveService(klass: FirClassSymbol<*>): TypeResolveService {
        // TODO cache?
        return TypeResolveServiceForPlugins(klass)
    }

    private inner class TypeResolveServiceForPlugins(
        klass: FirClassSymbol<*>,
    ) : TypeResolveService() {
        private val transformer = FirSpecificTypeResolverTransformer(
            session,
            supertypeSupplier = supertypeSupplier,
            expandTypeAliases = false,
        )

        private val configuration by lazy {
            // This is lazy so 'FirGeneratedClassNestedClassifierScope' can be created without preparing FirScopes of outer classes.
            val scopes = prepareScopes(klass.fir)
            val containingClassDeclarations = getContainingClassDeclarations(klass)
            val useSiteFile = session.firProvider.getFirClassifierContainerFileIfAny(klass)
            TypeResolutionConfiguration(scopes, containingClassDeclarations, useSiteFile)
        }

        override fun resolveUserType(type: FirUserTypeRef): FirResolvedTypeRef {
            return transformer.withBareTypes(allowed = true) {
                type.transform(transformer, configuration)
            }
        }
    }

    private fun getContainingClassDeclarations(klass: FirClassSymbol<*>): List<FirClass> {
        val firProvider = session.firProvider

        fun recurse(symbol: FirClassLikeSymbol<*>?, builder: MutableList<FirClass>) {
            if (symbol == null) return
            recurse(firProvider.getContainingClass(symbol), builder)
            if (symbol is FirClassSymbol<*>) builder.add(symbol.fir)
        }

        return buildList {
            recurse(firProvider.getContainingClass(klass), this@buildList)
        }
    }

    // See: FirSupertypesResolution.prepareScopes()
    // TODO persistent list is not required
    private fun prepareScopes(
        classLikeDeclaration: FirClassLikeDeclaration,
    ): PersistentList<FirScope> {
        return proxy.prepareScopes(classLikeDeclaration, forStaticNestedClass = false)
    }
}
