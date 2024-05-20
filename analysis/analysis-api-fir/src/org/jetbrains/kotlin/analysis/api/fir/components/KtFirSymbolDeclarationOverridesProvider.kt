/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirAnonymousObjectSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSymbolDeclarationOverridesProviderBase
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.analysis.api.fir.utils.isSubClassOf

internal class KaFirSymbolDeclarationOverridesProvider(
    override val analysisSession: KaFirSession,
    override val token: KaLifetimeToken
) : KaSymbolDeclarationOverridesProviderBase(), KaFirSessionComponent {
    override fun <T : KaSymbol> getAllOverriddenSymbols(
        callableSymbol: T,
    ): List<KaCallableSymbol> {
        require(callableSymbol is KaFirSymbol<*>)
        if (callableSymbol is KaFirBackingFieldSymbol) return emptyList()
        if (callableSymbol is KaValueParameterSymbol) {
            return callableSymbol.getAllOverriddenSymbols()
        }
        (callableSymbol.firSymbol as? FirIntersectionCallableSymbol)?.let { intersectionSymbol ->
            return intersectionSymbol.intersections.flatMap {
                getAllOverriddenSymbols(analysisSession.firSymbolBuilder.callableBuilder.buildCallableSymbol(it))
            }
        }

        val overriddenElement = mutableSetOf<FirCallableSymbol<*>>()
        processOverrides(callableSymbol) { firTypeScope, firCallableDeclaration ->
            firTypeScope.processAllOverriddenDeclarations(firCallableDeclaration) { overriddenDeclaration ->
                overriddenDeclaration.symbol.collectIntersectionOverridesSymbolsTo(
                    overriddenElement,
                    callableSymbol.analysisSession.useSiteSession
                )
            }
        }

        return overriddenElement.map { analysisSession.firSymbolBuilder.callableBuilder.buildCallableSymbol(it) }
    }

    override fun <T : KaSymbol> getDirectlyOverriddenSymbols(callableSymbol: T): List<KaCallableSymbol> {
        require(callableSymbol is KaFirSymbol<*>)
        if (callableSymbol is KaFirBackingFieldSymbol) return emptyList()
        if (callableSymbol is KaValueParameterSymbol) {
            return callableSymbol.getDirectlyOverriddenSymbols()
        }
        if (callableSymbol is KaCallableSymbol && callableSymbol.firSymbol is FirIntersectionCallableSymbol) {
            return getIntersectionOverriddenSymbols(callableSymbol)
        }

        val overriddenElement = mutableSetOf<FirCallableSymbol<*>>()
        processOverrides(callableSymbol) { firTypeScope, firCallableDeclaration ->
            firTypeScope.processDirectOverriddenDeclarations(firCallableDeclaration) { overriddenDeclaration ->
                overriddenDeclaration.symbol.collectIntersectionOverridesSymbolsTo(
                    overriddenElement,
                    callableSymbol.analysisSession.useSiteSession
                )
            }
        }

        return overriddenElement.map { analysisSession.firSymbolBuilder.callableBuilder.buildCallableSymbol(it) }
    }

    private fun FirTypeScope.processCallableByName(declaration: FirDeclaration) = when (declaration) {
        is FirSimpleFunction -> processFunctionsByName(declaration.name) { }
        is FirProperty -> processPropertiesByName(declaration.name) { }
        else -> Unit
    }

    private fun FirTypeScope.processAllOverriddenDeclarations(
        declaration: FirDeclaration,
        processor: (FirCallableDeclaration) -> Unit
    ) = when (declaration) {
        is FirSimpleFunction -> processOverriddenFunctions(declaration.symbol) { symbol ->
            processor.invoke(symbol.fir)
            ProcessorAction.NEXT
        }
        is FirProperty -> processOverriddenProperties(declaration.symbol) { symbol ->
            processor.invoke(symbol.fir)
            ProcessorAction.NEXT
        }
        else -> ProcessorAction.STOP
    }

    private fun FirTypeScope.processDirectOverriddenDeclarations(
        declaration: FirDeclaration,
        processor: (FirCallableDeclaration) -> Unit
    ) = when (declaration) {
        is FirSimpleFunction -> processDirectOverriddenFunctionsWithBaseScope(declaration.symbol) { symbol, _ ->
            processor.invoke(symbol.fir)
            ProcessorAction.NEXT
        }
        is FirProperty -> processDirectOverriddenPropertiesWithBaseScope(declaration.symbol) { symbol, _ ->
            processor.invoke(symbol.fir)
            ProcessorAction.NEXT
        }
        else -> ProcessorAction.STOP
    }

    private inline fun <T : KaSymbol> processOverrides(
        callableSymbol: T,
        crossinline process: (FirTypeScope, FirDeclaration) -> Unit
    ) {
        if (callableSymbol !is KaCallableSymbol) {
            return
        }

        require(callableSymbol is KaFirSymbol<*>)

        val containingDeclaration = with(analysisSession) {
            callableSymbol.getContainingSymbol() as? KaClassOrObjectSymbol
        } ?: return

        when (containingDeclaration) {
            is KaFirNamedClassOrObjectSymbol -> processOverrides(containingDeclaration, callableSymbol, process)
            is KaFirAnonymousObjectSymbol -> processOverrides(containingDeclaration, callableSymbol, process)
            else -> throw IllegalStateException("Expected $containingDeclaration to be a KtFirNamedClassOrObjectSymbol or KtFirAnonymousObjectSymbol")
        }
    }

    private inline fun processOverrides(
        containingDeclaration: KaFirSymbol<FirClassSymbol<*>>,
        callableSymbol: KaFirSymbol<*>,
        crossinline process: (FirTypeScope, FirDeclaration) -> Unit
    ) {
        containingDeclaration.firSymbol.lazyResolveToPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
        val firContainer = containingDeclaration.firSymbol.fir
        val firCallableDeclaration = callableSymbol.firSymbol.fir

        val firSession = callableSymbol.analysisSession.useSiteSession
        val firTypeScope = firContainer.unsubstitutedScope(
            firSession,
            analysisSession.getScopeSessionFor(firSession),
            withForcedTypeCalculator = false,
            memberRequiredPhase = FirResolvePhase.STATUS,
        )

        firTypeScope.processCallableByName(firCallableDeclaration)
        process(firTypeScope, firCallableDeclaration)
    }

    private fun FirCallableSymbol<*>.collectIntersectionOverridesSymbolsTo(
        to: MutableCollection<FirCallableSymbol<*>>,
        useSiteSession: FirSession,
    ) {
        when (this) {
            is FirIntersectionCallableSymbol -> {
                getIntersectionOverriddenSymbols(useSiteSession).forEach { it.collectIntersectionOverridesSymbolsTo(to, useSiteSession) }
            }
            else -> {
                to += this.fir.unwrapFakeOverrides().symbol
            }
        }
    }

    override fun isSubClassOf(subClass: KaClassOrObjectSymbol, superClass: KaClassOrObjectSymbol): Boolean {
        return isSubClassOf(subClass, superClass, allowIndirectSubtyping = true)
    }

    override fun isDirectSubClassOf(subClass: KaClassOrObjectSymbol, superClass: KaClassOrObjectSymbol): Boolean {
        return isSubClassOf(subClass, superClass, allowIndirectSubtyping = false)
    }

    private fun isSubClassOf(subClass: KaClassOrObjectSymbol, superClass: KaClassOrObjectSymbol, allowIndirectSubtyping: Boolean): Boolean {
        require(subClass is KaFirSymbol<*>)
        require(superClass is KaFirSymbol<*>)

        if (subClass == superClass) return false
        return isSubClassOf(
            subClass = subClass.firSymbol.fir as FirClass,
            superClass = superClass.firSymbol.fir as FirClass,
            rootModuleSession,
            allowIndirectSubtyping,
        )
    }

    override fun getIntersectionOverriddenSymbols(symbol: KaCallableSymbol): List<KaCallableSymbol> {
        require(symbol is KaFirSymbol<*>)
        if (symbol.origin != KaSymbolOrigin.INTERSECTION_OVERRIDE) return emptyList()
        return symbol.firSymbol
            .getIntersectionOverriddenSymbols(symbol.analysisSession.useSiteSession)
            .map { analysisSession.firSymbolBuilder.callableBuilder.buildCallableSymbol(it) }

    }

    private fun FirBasedSymbol<*>.getIntersectionOverriddenSymbols(useSiteSession: FirSession): Collection<FirCallableSymbol<*>> {
        require(this is FirCallableSymbol<*>) {
            "Required FirCallableSymbol but ${this::class} found"
        }
        return when (this) {
            is FirIntersectionCallableSymbol -> getNonSubsumedOverriddenSymbols(useSiteSession, analysisSession.getScopeSessionFor(useSiteSession))
            else -> listOf(this)
        }
    }
}
