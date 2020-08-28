/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.CheckersComponent
import org.jetbrains.kotlin.fir.extensions.FirExtensionService
import org.jetbrains.kotlin.fir.extensions.FirPredicateBasedProvider
import org.jetbrains.kotlin.fir.extensions.FirRegisteredPluginAnnotations
import org.jetbrains.kotlin.fir.java.FirJavaVisibilityChecker
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.ConeCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticNamesProvider
import org.jetbrains.kotlin.fir.resolve.calls.jvm.JvmCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirQualifierResolverImpl
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirTypeResolverImpl
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.GeneratedClassIndex
import org.jetbrains.kotlin.fir.scopes.impl.FirDeclaredMemberScopeProvider
import org.jetbrains.kotlin.fir.types.FirCorrespondingSupertypesCache

// -------------------------- Required components --------------------------

@OptIn(SessionConfiguration::class)
fun FirSession.registerCommonComponents() {
    register(FirDeclaredMemberScopeProvider::class, FirDeclaredMemberScopeProvider())
    register(FirCorrespondingSupertypesCache::class, FirCorrespondingSupertypesCache(this))

    register(FirExtensionService::class, FirExtensionService(this))
    register(FirRegisteredPluginAnnotations::class, FirRegisteredPluginAnnotations.create(this))
    register(FirPredicateBasedProvider::class, FirPredicateBasedProvider.create(this))
    register(GeneratedClassIndex::class, GeneratedClassIndex.create())
}

// -------------------------- Resolve components --------------------------

/*
 * Resolve components which are same on all platforms
 */
@OptIn(SessionConfiguration::class)
fun FirModuleBasedSession.registerResolveComponents() {
    register(FirQualifierResolver::class, FirQualifierResolverImpl(this))
    register(FirTypeResolver::class, FirTypeResolverImpl(this))
    register(CheckersComponent::class, CheckersComponent())
}

/*
 * Resolve components which have specific implementations on JVM
 */
@OptIn(SessionConfiguration::class)
fun FirModuleBasedSession.registerJavaSpecificComponents() {
    register(FirVisibilityChecker::class, FirJavaVisibilityChecker)
    register(ConeCallConflictResolverFactory::class, JvmCallConflictResolverFactory)
    register(FirEffectiveVisibilityResolver::class, FirJvmEffectiveVisibilityResolver(this))
    register(FirPlatformClassMapper::class, FirJavaClassMapper(this))
    register(FirSyntheticNamesProvider::class, FirJavaSyntheticNamesProvider)
}
