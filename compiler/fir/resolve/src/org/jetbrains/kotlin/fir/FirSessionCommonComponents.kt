/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionService
import org.jetbrains.kotlin.fir.extensions.FirPredicateBasedProvider
import org.jetbrains.kotlin.fir.extensions.FirRegisteredPluginAnnotations
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.GeneratedNestedClassIndex
import org.jetbrains.kotlin.fir.scopes.impl.FirDeclaredMemberScopeProvider
import org.jetbrains.kotlin.fir.types.FirCorrespondingSupertypesCache

fun FirSession.registerCommonComponents() {
    register(FirDeclaredMemberScopeProvider::class, FirDeclaredMemberScopeProvider())
    register(FirCorrespondingSupertypesCache::class, FirCorrespondingSupertypesCache(this))

    register(FirExtensionService::class, FirExtensionService(this))
    register(FirRegisteredPluginAnnotations::class, FirRegisteredPluginAnnotations.create(this))
    register(FirPredicateBasedProvider::class, FirPredicateBasedProvider.create(this))
    register(GeneratedNestedClassIndex::class, GeneratedNestedClassIndex.create())
}