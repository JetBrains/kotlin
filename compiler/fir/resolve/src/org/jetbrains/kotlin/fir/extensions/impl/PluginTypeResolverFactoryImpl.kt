/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingReplSymbolAttr
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.PluginTypeResolver
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.SupertypeSupplier
import org.jetbrains.kotlin.fir.resolve.TypeResolutionConfiguration
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirSpecificTypeResolverTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.FirSupertypeScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReplSnippetSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe

@FirExtensionApiInternals
class PluginTypeResolverFactoryImpl(
    private val session: FirSession,
) : PluginTypeResolverFactory() {
    private val scopeSession: ScopeSession = ScopeSession()

    private val supertypeSupplier = PluginSupertypeSupplier()

    private val scopeProvider = PluginSupertypeScopeProvider()

    override fun getTypeResolver(klass: FirClassSymbol<*>): PluginTypeResolver {
        return PluginTypeResolverImpl(klass)
    }

    private inner class PluginTypeResolverImpl(klass: FirClassSymbol<*>) : PluginTypeResolver {
        private val transformer = FirSpecificTypeResolverTransformer(
            session,
            supertypeSupplier = supertypeSupplier,
            expandTypeAliases = false,
        )

        private val configuration by lazy {
            // This is lazy so 'FirGeneratedClassNestedClassifierScope' can be created without preparing FirScopes of outer classes.
            // Otherwise, there is the possibility of stack overflow due to lazy properties in 'FirJavaClass'.
            val scopes = scopeProvider.prepareOuterScopes(klass.fir, forStaticNestedClass = false)
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

    private class PluginSupertypeSupplier : SupertypeSupplier() {
        override fun forClass(firClass: FirClass, useSiteSession: FirSession): List<ConeClassLikeType> {
            // TODO(KT-91606): require supertypes to be resolved(?) when generating companion object is fixed.
            return firClass.superTypeRefs.mapNotNull { it.coneTypeSafe<ConeClassLikeType>() }
        }

        override fun expansionForTypeAlias(typeAlias: FirTypeAlias, useSiteSession: FirSession): ConeClassLikeType? {
            // TODO(KT-91606): require supertypes to be resolved(?) when generating companion object is fixed.
            return typeAlias.expandedTypeRef.coneTypeSafe()
        }
    }

    private inner class PluginSupertypeScopeProvider : FirSupertypeScopeProvider(session, scopeSession, supertypeSupplier) {
        override fun replSnippet(declaration: FirClassLikeDeclaration): FirReplSnippetSymbol? {
            return declaration.containingReplSymbolAttr
        }

        override fun resolveAllSupertypesForOuterClass(outerClass: FirClass) {
            // TODO(KT-91606): require supertypes to be resolved(?) when generating companion object is fixed.
            // do nothing
        }
    }
}
