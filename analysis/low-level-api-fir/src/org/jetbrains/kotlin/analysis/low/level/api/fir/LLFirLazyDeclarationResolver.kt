/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirResolvableSession
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver
import org.jetbrains.kotlin.fir.types.toClassSymbol

@ThreadSafeMutableState
internal class LLFirLazyDeclarationResolver : FirLazyDeclarationResolver() {
    override fun startResolvingPhase(phase: FirResolvePhase) {}
    override fun finishResolvingPhase(phase: FirResolvePhase) {}

    override fun lazyResolveToPhase(element: FirElementWithResolveState, toPhase: FirResolvePhase) {
        val session = element.llFirResolvableSession ?: return
        session.moduleComponents.firModuleLazyDeclarationResolver.lazyResolve(
            target = element,
            toPhase = toPhase,
        )
    }

    override fun lazyResolveToPhaseWithCallableMembers(clazz: FirClass, toPhase: FirResolvePhase) {
        val fir = clazz as? FirRegularClass ?: return
        val session = fir.llFirResolvableSession ?: return
        session.moduleComponents.firModuleLazyDeclarationResolver.lazyResolveWithCallableMembers(
            target = fir,
            toPhase = toPhase,
        )

        if (toPhase == FirResolvePhase.STATUS && fir.declarations.none { it is FirCallableDeclaration }) {
            for (superType in fir.superConeTypes) {
                val classSymbol = superType.toClassSymbol(session) ?: continue
                lazyResolveToPhaseWithCallableMembers(classSymbol.fir, toPhase)
            }
        }
    }

    override fun lazyResolveToPhaseRecursively(element: FirElementWithResolveState, toPhase: FirResolvePhase) {
        val session = element.llFirResolvableSession ?: return
        session.moduleComponents.firModuleLazyDeclarationResolver.lazyResolveRecursively(
            target = element,
            toPhase = toPhase,
        )
    }
}
