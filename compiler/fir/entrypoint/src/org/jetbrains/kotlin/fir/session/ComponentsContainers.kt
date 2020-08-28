/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirModuleBasedSession
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.CheckersComponent
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.ExtendedDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExtendedExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkersComponent
import org.jetbrains.kotlin.fir.extensions.FirExtensionService
import org.jetbrains.kotlin.fir.extensions.FirPredicateBasedProvider
import org.jetbrains.kotlin.fir.extensions.FirRegisteredPluginAnnotations
import org.jetbrains.kotlin.fir.java.registerJavaVisibilityChecker
import org.jetbrains.kotlin.fir.registerJvmEffectiveVisibilityResolver
import org.jetbrains.kotlin.fir.resolve.FirQualifierResolver
import org.jetbrains.kotlin.fir.resolve.FirTypeResolver
import org.jetbrains.kotlin.fir.resolve.calls.jvm.registerJvmCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirQualifierResolverImpl
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirTypeResolverImpl
import org.jetbrains.kotlin.fir.resolve.registerJavaClassMapper
import org.jetbrains.kotlin.fir.resolve.registerJavaSyntheticNamesProvider
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.GeneratedClassIndex
import org.jetbrains.kotlin.fir.scopes.impl.FirDeclaredMemberScopeProvider
import org.jetbrains.kotlin.fir.types.FirCorrespondingSupertypesCache

// -------------------------- Required components --------------------------

fun FirSession.registerCommonComponents() {
    register(FirDeclaredMemberScopeProvider::class, FirDeclaredMemberScopeProvider())
    register(FirCorrespondingSupertypesCache::class, FirCorrespondingSupertypesCache(this))

    register(FirExtensionService::class, FirExtensionService(this))
    register(FirRegisteredPluginAnnotations::class, FirRegisteredPluginAnnotations.create(this))
    register(FirPredicateBasedProvider::class, FirPredicateBasedProvider.create(this))
    register(GeneratedClassIndex::class, GeneratedClassIndex.create())
}

// -------------------------- Resolve components --------------------------

// TODO: rename to `registerCommonResolveComponents
fun FirModuleBasedSession.registerResolveComponents() {
    register(FirQualifierResolver::class, FirQualifierResolverImpl(this))
    register(FirTypeResolver::class, FirTypeResolverImpl(this))
}

fun FirModuleBasedSession.registerJavaSpecificComponents() {
    registerJavaVisibilityChecker()
    registerJvmCallConflictResolverFactory()
    registerJvmEffectiveVisibilityResolver()
    registerJavaClassMapper()
    registerJavaSyntheticNamesProvider()
}

// -------------------------- Checker components --------------------------

/*
 * TODO: in future rename to `registerCheckersComponent` and configure
 *    exact checkers according to platforms of current session
 */
fun FirModuleBasedSession.registerCheckersComponent() {
    register(CheckersComponent::class, CheckersComponent.componentWithDefaultCheckers())
}

fun FirModuleBasedSession.registerExtendedCheckersComponent() {
    this.checkersComponent.register(ExtendedExpressionCheckers)
    this.checkersComponent.register(ExtendedDeclarationCheckers)
}
