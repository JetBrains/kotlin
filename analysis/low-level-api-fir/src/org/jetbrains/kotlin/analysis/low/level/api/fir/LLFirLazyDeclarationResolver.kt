/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol

@ThreadSafeMutableState
internal class LLFirLazyDeclarationResolver : FirLazyDeclarationResolver() {
    override fun startResolvingPhase(phase: FirResolvePhase) {}
    override fun finishResolvingPhase(phase: FirResolvePhase) {}

    override fun lazyResolveToPhase(symbol: FirBasedSymbol<*>, toPhase: FirResolvePhase) {
        val fir = symbol.fir
        val session = fir.moduleData.session
        if (session !is LLFirResolvableModuleSession) return
        val moduleComponents = session.moduleComponents
        moduleComponents.firModuleLazyDeclarationResolver.lazyResolve(
            target = fir,
            scopeSession = moduleComponents.scopeSessionProvider.getScopeSession(),
            toPhase = toPhase,
        )
    }

    override fun lazyResolveToPhaseWithCallableMembers(symbol: FirClassSymbol<*>, toPhase: FirResolvePhase) {
        val fir = symbol.fir as? FirRegularClass ?: return
        val session = fir.moduleData.session
        if (session !is LLFirResolvableModuleSession) return
        // TODO: should be implemented in the context of KT-56543
    }
}
