/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionInvalidator

@ThreadSafeMutableState
internal class LLFirLazyDeclarationResolver(private val sessionInvalidator: LLFirSessionInvalidator) : FirLazyDeclarationResolver() {
    override fun lazyResolveToPhase(symbol: FirBasedSymbol<*>, toPhase: FirResolvePhase) {
        val fir = symbol.fir
        val session = fir.moduleData.session
        if (session !is LLFirResolvableModuleSession) return
        val moduleComponents = session.moduleComponents
        try {
            moduleComponents.firModuleLazyDeclarationResolver.lazyResolveDeclaration(
                firDeclarationToResolve = fir,
                scopeSession = moduleComponents.scopeSessionProvider.getScopeSession(),
                toPhase = toPhase,
                checkPCE = true,
            )
        } catch (e: Throwable) {
            sessionInvalidator.invalidate(session)
            throw e
        }
    }
}
