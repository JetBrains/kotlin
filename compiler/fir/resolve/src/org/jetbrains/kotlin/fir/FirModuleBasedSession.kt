/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.extensions.FirExtensionService
import org.jetbrains.kotlin.fir.extensions.FirRegisteredPluginAnnotations
import org.jetbrains.kotlin.fir.resolve.FirQualifierResolver
import org.jetbrains.kotlin.fir.extensions.FirPredicateBasedProvider
import org.jetbrains.kotlin.fir.resolve.FirTypeResolver
import org.jetbrains.kotlin.fir.resolve.impl.FirQualifierResolverImpl
import org.jetbrains.kotlin.fir.resolve.impl.FirTypeResolverImpl
import org.jetbrains.kotlin.fir.scopes.impl.FirDeclaredMemberScopeProvider

abstract class FirModuleBasedSession(override val moduleInfo: ModuleInfo, sessionProvider: FirSessionProvider?) :
    FirSessionBase(sessionProvider) {
    init {
        registerComponent(FirQualifierResolver::class, FirQualifierResolverImpl(this))
        registerComponent(FirTypeResolver::class, FirTypeResolverImpl(this))
        registerComponent(FirDeclaredMemberScopeProvider::class, FirDeclaredMemberScopeProvider())
        registerComponent(FirExtensionService::class, FirExtensionService(this))
        registerComponent(FirRegisteredPluginAnnotations::class, FirRegisteredPluginAnnotations.create(this))
        registerComponent(FirPredicateBasedProvider::class, FirPredicateBasedProvider.create(this))
    }
}

