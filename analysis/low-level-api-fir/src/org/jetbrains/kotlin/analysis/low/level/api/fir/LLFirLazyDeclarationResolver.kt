/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirResolvableSession
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.isJava
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver
import org.jetbrains.kotlin.fir.resolve.toClassSymbol

@ThreadSafeMutableState
internal class LLFirLazyDeclarationResolver : FirLazyDeclarationResolver() {
    override fun startResolvingPhase(phase: FirResolvePhase) {}
    override fun finishResolvingPhase(phase: FirResolvePhase) {}

    override fun lazyResolveToPhase(element: FirElementWithResolveState, toPhase: FirResolvePhase) {
        assertLazyResolveAllowed()
        val session = element.llFirResolvableSession ?: return
        session.moduleComponents.firModuleLazyDeclarationResolver.lazyResolve(
            target = element,
            toPhase = toPhase,
        )
    }

    override fun lazyResolveToPhaseWithCallableMembers(clazz: FirClass, toPhase: FirResolvePhase) {
        assertLazyResolveAllowed()
        val fir = clazz as? FirRegularClass ?: return
        val session = fir.llFirResolvableSession ?: return
        lazyResolveWithOwnCallableMembers(fir, session, toPhase)

        if (toPhase == FirResolvePhase.STATUS && fir.declarations.none { it is FirCallableDeclaration }) {
            for (superType in fir.superConeTypes) {
                val classSymbol = superType.toClassSymbol(session) ?: continue
                lazyResolveToPhaseWithCallableMembers(classSymbol.fir, toPhase)
            }
        }
    }

    override fun lazyResolveToPhaseWithCallableMembersInSupertypes(clazz: FirClass, useSiteSession: FirSession, toPhase: FirResolvePhase) {
        assertLazyResolveAllowed()
        lazyResolveWithOwnCallableMembers(clazz, toPhase)

        // The walk visits each supertype once, so the classes are resolved without the recursion of `lazyResolveToPhaseWithCallableMembers`
        for (superType in lookupSuperTypes(clazz, lookupInterfaces = true, deep = true, useSiteSession, substituteTypes = false)) {
            val superClass = superType.lookupTag.toRegularClassSymbol(useSiteSession)?.fir ?: continue
            lazyResolveWithOwnCallableMembers(superClass, toPhase)
        }
    }

    /**
     * Resolves [clazz] and its own callable members, but not its supertypes.
     *
     * Java classes are skipped as they are always resolved.
     */
    private fun lazyResolveWithOwnCallableMembers(clazz: FirClass, toPhase: FirResolvePhase) {
        if (clazz !is FirRegularClass || clazz.isJava) return
        val session = clazz.llFirResolvableSession ?: return
        lazyResolveWithOwnCallableMembers(clazz, session, toPhase)
    }

    private fun lazyResolveWithOwnCallableMembers(clazz: FirRegularClass, session: LLFirResolvableModuleSession, toPhase: FirResolvePhase) {
        session.moduleComponents.firModuleLazyDeclarationResolver.lazyResolveWithCallableMembers(
            target = clazz,
            toPhase = toPhase,
        )
    }

    override fun lazyResolveToPhaseRecursively(element: FirElementWithResolveState, toPhase: FirResolvePhase) {
        assertLazyResolveAllowed()
        val session = element.llFirResolvableSession ?: return
        session.moduleComponents.firModuleLazyDeclarationResolver.lazyResolveRecursively(
            target = element,
            toPhase = toPhase,
        )
    }
}
