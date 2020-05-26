/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionService
import org.jetbrains.kotlin.fir.extensions.FirPredicateBasedProvider
import org.jetbrains.kotlin.fir.extensions.FirRegisteredPluginAnnotations
import org.jetbrains.kotlin.fir.scopes.impl.FirDeclaredMemberScopeProvider
import org.jetbrains.kotlin.fir.types.FirCorrespondingSupertypesCache

abstract class FirSessionBase(sessionProvider: FirSessionProvider?) : FirSession(sessionProvider) {
    init {
        registerComponent(FirDeclaredMemberScopeProvider::class, FirDeclaredMemberScopeProvider())
        registerComponent(FirCorrespondingSupertypesCache::class, FirCorrespondingSupertypesCache(this))

        registerComponent(FirExtensionService::class, FirExtensionService(this))
        registerComponent(FirRegisteredPluginAnnotations::class, FirRegisteredPluginAnnotations.create(this))
        registerComponent(FirPredicateBasedProvider::class, FirPredicateBasedProvider.create(this))
    }
}