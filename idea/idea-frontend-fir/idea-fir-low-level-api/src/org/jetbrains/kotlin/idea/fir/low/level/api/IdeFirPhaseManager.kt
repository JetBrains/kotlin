/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.transformers.FirPhaseManager
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirSessionInvalidator

@ThreadSafeMutableState
internal class IdeFirPhaseManager(
    private val lazyDeclarationResolver: FirLazyDeclarationResolver,
    private val cache: ModuleFileCache,
    private val sessionInvalidator: FirSessionInvalidator,
) : FirPhaseManager() {
    override fun ensureResolved(
        symbol: AbstractFirBasedSymbol<*>,
        requiredPhase: FirResolvePhase
    ) {
        val fir = symbol.fir as FirDeclaration
        try {
            lazyDeclarationResolver.lazyResolveDeclaration(fir, cache, requiredPhase, checkPCE = true)
        } catch (e: Throwable) {
            sessionInvalidator.invalidate(fir.declarationSiteSession)
            throw e
        }
    }
}
