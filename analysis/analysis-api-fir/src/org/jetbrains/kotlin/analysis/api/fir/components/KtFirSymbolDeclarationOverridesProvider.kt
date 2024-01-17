/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirAnonymousObjectSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.components.KtSymbolDeclarationOverridesProviderBase
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.fir.unwrapFakeOverrides

internal class KtFirSymbolDeclarationOverridesProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken
) : KtSymbolDeclarationOverridesProviderBase(), KtFirAnalysisSessionComponent {
    override fun <T : KtSymbol> getAllOverriddenSymbols(
        callableSymbol: T,
    ): List<KtCallableSymbol> {
        require(callableSymbol is KtFirSymbol<*>)
        if (callableSymbol is KtFirBackingFieldSymbol) return emptyList()
        if (callableSymbol is KtValueParameterSymbol) {
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

    override fun <T : KtSymbol> getDirectlyOverriddenSymbols(callableSymbol: T): List<KtCallableSymbol> {
        require(callableSymbol is KtFirSymbol<*>)
        if (callableSymbol is KtFirBackingFieldSymbol) return emptyList()
        if (callableSymbol is KtValueParameterSymbol) {
            return callableSymbol.getDirectlyOverriddenSymbols()
        }
        if (callableSymbol is KtCallableSymbol && callableSymbol.firSymbol is FirIntersectionCallableSymbol) {
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

    private inline fun <T : KtSymbol> processOverrides(
        callableSymbol: T,
        crossinline process: (FirTypeScope, FirDeclaration) -> Unit
    ) {
        require(callableSymbol is KtFirSymbol<*>)
        val containingDeclaration = with(analysisSession) {
            (callableSymbol as? KtCallableSymbol)?.originalContainingClassForOverride
        } ?: return
        when (containingDeclaration) {
            is KtFirNamedClassOrObjectSymbol -> processOverrides(containingDeclaration, callableSymbol, process)
            is KtFirAnonymousObjectSymbol -> processOverrides(containingDeclaration, callableSymbol, process)
            else -> throw IllegalStateException("Expected $containingDeclaration to be a KtFirNamedClassOrObjectSymbol or KtFirAnonymousObjectSymbol")
        }
    }

    private inline fun processOverrides(
        containingDeclaration: KtFirSymbol<FirClassSymbol<*>>,
        callableSymbol: KtFirSymbol<*>,
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

    override fun isSubClassOf(subClass: KtClassOrObjectSymbol, superClass: KtClassOrObjectSymbol): Boolean {
        return isSubClassOf(subClass, superClass, checkDeep = true)
    }

    override fun isDirectSubClassOf(subClass: KtClassOrObjectSymbol, superClass: KtClassOrObjectSymbol): Boolean {
        return isSubClassOf(subClass, superClass, checkDeep = false)
    }

    private fun isSubClassOf(subClass: KtClassOrObjectSymbol, superClass: KtClassOrObjectSymbol, checkDeep: Boolean): Boolean {
        require(subClass is KtFirSymbol<*>)
        require(superClass is KtFirSymbol<*>)

        if (subClass == superClass) return false
        subClass.firSymbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
        return isSubClassOf(
            subClass = subClass.firSymbol.fir as FirClass,
            superClass = superClass.firSymbol.fir as FirClass,
            checkDeep
        )


    }

    private fun isSubClassOf(subClass: FirClass, superClass: FirClass, checkDeep: Boolean): Boolean {
        if (subClass.superConeTypes.any { it.toRegularClassSymbol(rootModuleSession) == superClass.symbol }) return true
        if (!checkDeep) return false
        subClass.superConeTypes.forEach { superType ->
            val superOfSub = superType.toRegularClassSymbol(rootModuleSession) ?: return@forEach
            superOfSub.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
            if (isSubClassOf(superOfSub.fir, superClass, checkDeep = true)) return true
        }
        return false
    }

    override fun getIntersectionOverriddenSymbols(symbol: KtCallableSymbol): List<KtCallableSymbol> {
        require(symbol is KtFirSymbol<*>)
        if (symbol.origin != KtSymbolOrigin.INTERSECTION_OVERRIDE) return emptyList()
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
