/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.symbols.FirPhaseManager
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
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
        symbol: FirBasedSymbol<*>,
        requiredPhase: FirResolvePhase
    ) {
        val fir = symbol.fir as FirDeclaration
        try {
            if (fir.resolvePhase < requiredPhase) { //TODO Make thread safe
                lazyDeclarationResolver.lazyResolveDeclaration(
                    firDeclarationToResolve = fir,
                    moduleFileCache = cache,
                    scopeSession = ScopeSession(),
                    toPhase = requiredPhase,
                    checkPCE = true,
                    skipLocalDeclaration = true,
                )
            }
        } catch (e: Throwable) {
            sessionInvalidator.invalidate(fir.moduleData.session)
            throw e
        }
    }
}
