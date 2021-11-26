/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KtSymbolDeclarationOverridesProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirAnonymousObjectSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.ResolveType
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirIntersectionOverrideFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirIntersectionOverridePropertySymbol

internal class KtFirSymbolDeclarationOverridesProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtSymbolDeclarationOverridesProvider(), KtFirAnalysisSessionComponent {

    override fun <T : KtSymbol> getAllOverriddenSymbols(
        callableSymbol: T,
    ): List<KtCallableSymbol> {
        if (callableSymbol is KtFirBackingFieldSymbol) return emptyList()
        val overriddenElement = mutableSetOf<FirCallableSymbol<*>>()
        processOverrides(callableSymbol) { firTypeScope, firCallableDeclaration ->
            firTypeScope.processAllOverriddenDeclarations(firCallableDeclaration) { overriddenDeclaration ->
                overriddenDeclaration.symbol.collectIntersectionOverridesSymbolsTo(overriddenElement)
            }
        }
        return overriddenElement.map { analysisSession.firSymbolBuilder.callableBuilder.buildCallableSymbol(it.fir) }
    }

    override fun <T : KtSymbol> getDirectlyOverriddenSymbols(callableSymbol: T): List<KtCallableSymbol> {
        if (callableSymbol is KtFirBackingFieldSymbol) return emptyList()
        val overriddenElement = mutableSetOf<FirCallableSymbol<*>>()
        processOverrides(callableSymbol) { firTypeScope, firCallableDeclaration ->
            firTypeScope.processDirectOverriddenDeclarations(firCallableDeclaration) { overriddenDeclaration ->
                overriddenDeclaration.symbol.collectIntersectionOverridesSymbolsTo(overriddenElement)
            }
        }
        return overriddenElement.map { analysisSession.firSymbolBuilder.callableBuilder.buildCallableSymbol(it.fir) }
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
        containingDeclaration: KtFirSymbol<FirClass>,
        callableSymbol: KtFirSymbol<*>,
        crossinline process: (FirTypeScope, FirDeclaration) -> Unit
    ) {
        containingDeclaration.firRef.withFir(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) { firContainer ->
            callableSymbol.firRef.withFirUnsafe { firCallableDeclaration ->
                val firTypeScope = firContainer.unsubstitutedScope(
                    firContainer.moduleData.session,
                    ScopeSession(),
                    withForcedTypeCalculator = false
                )
                firTypeScope.processCallableByName(firCallableDeclaration)
                process(firTypeScope, firCallableDeclaration)
            }
        }
    }

    private fun FirCallableSymbol<*>.collectIntersectionOverridesSymbolsTo(to: MutableCollection<FirCallableSymbol<*>>) {
        when (this) {
            is FirIntersectionOverrideFunctionSymbol -> {
                intersections.forEach { it.collectIntersectionOverridesSymbolsTo(to) }
            }
            is FirIntersectionOverridePropertySymbol -> {
                intersections.forEach { it.collectIntersectionOverridesSymbolsTo(to) }
            }
            else -> {
                to += this
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
        return subClass.firRef.withFirByType(ResolveType.ClassSuperTypes) { subClassFir ->
            check(subClassFir is FirRegularClass)
            superClass.firRef.withFir { superClassFir ->
                check(superClassFir is FirRegularClass)
                isSubClassOf(subClassFir, superClassFir, checkDeep)
            }
        }
    }

    private fun isSubClassOf(subClass: FirRegularClass, superClass: FirRegularClass, checkDeep: Boolean): Boolean {
        if (subClass.superConeTypes.any { it.toRegularClassSymbol(rootModuleSession) == superClass.symbol }) return true
        if (!checkDeep) return false
        subClass.superConeTypes.forEach { superType ->
            val superOfSub = superType.toRegularClassSymbol(rootModuleSession) ?: return@forEach
            superOfSub.ensureResolved(FirResolvePhase.SUPER_TYPES)
            if (isSubClassOf(superOfSub.fir, superClass, checkDeep = true)) return true
        }
        return false
    }

    override fun getIntersectionOverriddenSymbols(symbol: KtCallableSymbol): Collection<KtCallableSymbol> {
        require(symbol is KtFirSymbol<*>)
        if (symbol.origin != KtSymbolOrigin.INTERSECTION_OVERRIDE) return emptyList()
        return symbol.firRef.withFir { fir ->
            val firSymbol = fir.symbol
            firSymbol.getIntersectionOverriddenSymbols()
                .map { analysisSession.firSymbolBuilder.callableBuilder.buildCallableSymbol(it.fir) }
        }
    }

    private fun FirBasedSymbol<*>.getIntersectionOverriddenSymbols(): Collection<FirCallableSymbol<*>> {
        require(this is FirCallableSymbol<*>) {
            "Required FirCallableSymbol but ${this::class} found"
        }
        return when (this) {
            is FirIntersectionOverrideFunctionSymbol -> intersections
            is FirIntersectionOverridePropertySymbol -> intersections
            else -> listOf(this)
        }
    }
}
