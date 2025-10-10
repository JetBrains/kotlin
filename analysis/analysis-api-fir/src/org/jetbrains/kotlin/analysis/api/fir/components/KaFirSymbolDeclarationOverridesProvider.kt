/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.containingDeclaration
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirAnonymousObjectSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.isSubclassOf
import org.jetbrains.kotlin.analysis.api.fir.utils.withSymbolAttachment
import org.jetbrains.kotlin.analysis.api.impl.base.components.analysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirIntersectionCallableSymbol
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

@RequiresOptIn("The overrides API shouldn't be accessible directly, use options from KaSession instead")
internal annotation class KaFirOverridesProviderImplementationDetail

@OptIn(ScopeFunctionRequiresPrewarm::class)
context(relationProvider: KaFirSymbolRelationProvider)
internal fun <T : KaCallableSymbol> getAllOverriddenSymbols(callableSymbol: T): Sequence<KaCallableSymbol> = with(analysisSession) {
    require(callableSymbol is KaFirSymbol<*>)
    val overriddenElement = mutableSetOf<FirCallableSymbol<*>>()
    processOverrides(callableSymbol, skipIsOverrideCheck = false) { firTypeScope, firCallableDeclaration ->
        firTypeScope.processAllOverriddenDeclarations(firCallableDeclaration) { overriddenDeclaration ->
            overriddenDeclaration.symbol.collectIntersectionOverridesSymbolsTo(
                overriddenElement,
                callableSymbol.analysisSession.firSession
            )
        }
    }

    overriddenElement
        .map { firSymbolBuilder.callableBuilder.buildCallableSymbol(it) }
        .asSequence()
}

context(relationProvider: KaFirSymbolRelationProvider)
internal fun <T : KaCallableSymbol> getDirectlyOverriddenSymbols(callableSymbol: T): Sequence<KaCallableSymbol> = context(analysisSession) {
    @OptIn(KaFirOverridesProviderImplementationDetail::class)
    getDirectlyOverriddenSymbols(callableSymbol, skipIsOverrideCheck = false)
}

@KaFirOverridesProviderImplementationDetail
context(analysisSession: KaFirSession)
internal fun <T : KaCallableSymbol> getDirectlyOverriddenSymbols(
    callableSymbol: T,
    skipIsOverrideCheck: Boolean,
): Sequence<KaCallableSymbol> {
    require(callableSymbol is KaFirSymbol<*>)
    val overriddenElement = mutableSetOf<FirCallableSymbol<*>>()
    processOverrides(callableSymbol, skipIsOverrideCheck) { firTypeScope, firCallableDeclaration ->
        firTypeScope.processDirectOverriddenDeclarations(firCallableDeclaration) { overriddenDeclaration ->
            overriddenDeclaration.symbol.collectIntersectionOverridesSymbolsTo(
                overriddenElement,
                callableSymbol.analysisSession.firSession
            )
        }
    }

    return overriddenElement
        .map { analysisSession.firSymbolBuilder.callableBuilder.buildCallableSymbol(it) }
        .asSequence()
}

private fun FirTypeScope.processCallableByName(declaration: FirDeclaration) = when (declaration) {
    is FirNamedFunction -> processFunctionsByName(declaration.name) { }
    is FirProperty -> processPropertiesByName(declaration.name) { }
    else -> Unit
}

@ScopeFunctionRequiresPrewarm
private fun FirTypeScope.processAllOverriddenDeclarations(
    declaration: FirDeclaration,
    processor: (FirCallableDeclaration) -> Unit,
) = when (declaration) {
    is FirNamedFunction -> processOverriddenFunctions(declaration.symbol) { symbol ->
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
    processor: (FirCallableDeclaration) -> Unit,
) = when (declaration) {
    is FirNamedFunction -> processDirectOverriddenFunctionsWithBaseScope(declaration.symbol) { symbol, _ ->
        processor.invoke(symbol.fir)
        ProcessorAction.NEXT
    }
    is FirProperty -> processDirectOverriddenPropertiesWithBaseScope(declaration.symbol) { symbol, _ ->
        processor.invoke(symbol.fir)
        ProcessorAction.NEXT
    }
    else -> ProcessorAction.STOP
}

private fun KaCallableSymbol.mayHaveOverriddenCallables(skipIsOverrideCheck: Boolean): Boolean = when (this) {
    is KaNamedFunctionSymbol -> skipIsOverrideCheck || isOverride
    is KaPropertySymbol -> skipIsOverrideCheck || isOverride
    is KaPropertyAccessorSymbol -> false
    is KaParameterSymbol -> false
    is KaConstructorSymbol -> false
    is KaAnonymousFunctionSymbol -> false
    is KaSamConstructorSymbol -> false
    is KaBackingFieldSymbol -> false
    is KaEnumEntrySymbol -> false
    is KaJavaFieldSymbol -> false
    is KaLocalVariableSymbol -> false
}

context(analysisSession: KaFirSession)
private inline fun <T> processOverrides(
    callableSymbol: T,
    skipIsOverrideCheck: Boolean,
    crossinline process: (FirTypeScope, FirDeclaration) -> Unit,
) where T : KaCallableSymbol, T : KaFirSymbol<*> {
    if (!callableSymbol.mayHaveOverriddenCallables(skipIsOverrideCheck)) {
        return
    }

    when (val containingDeclaration = callableSymbol.containingDeclaration) {
        !is KaClassSymbol -> return
        is KaFirNamedClassSymbol, is KaFirAnonymousObjectSymbol -> processOverrides(containingDeclaration, callableSymbol, process)
        else -> errorWithAttachment("Expected the containing symbol to be a ${KaFirNamedClassSymbol::class.simpleName} or ${KaFirAnonymousObjectSymbol::class.simpleName}") {
            withSymbolAttachment("callable", analysisSession, callableSymbol)
            withSymbolAttachment("containingDeclaration", analysisSession, containingDeclaration)
        }
    }
}

context(analysisSession: KaFirSession)
private inline fun processOverrides(
    containingDeclaration: KaFirSymbol<FirClassSymbol<*>>,
    callableSymbol: KaFirSymbol<*>,
    crossinline process: (FirTypeScope, FirDeclaration) -> Unit,
) {
    val firContainer = containingDeclaration.firSymbol.fir
    val firCallableDeclaration = callableSymbol.firSymbol.fir

    val firSession = callableSymbol.analysisSession.firSession
    val firTypeScope = firContainer.unsubstitutedScope(
        firSession,
        analysisSession.getScopeSessionFor(firSession),
        withForcedTypeCalculator = false,
        memberRequiredPhase = FirResolvePhase.STATUS,
    )

    firTypeScope.processCallableByName(firCallableDeclaration)
    process(firTypeScope, firCallableDeclaration)
}

context(analysisSession: KaFirSession)
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

context(relationProvider: KaFirSymbolRelationProvider)
internal fun isSubClassOf(subClass: KaClassSymbol, superClass: KaClassSymbol): Boolean {
    return isSubClassOf(subClass, superClass, allowIndirectSubtyping = true)
}

context(relationProvider: KaFirSymbolRelationProvider)
internal fun isDirectSubClassOf(subClass: KaClassSymbol, superClass: KaClassSymbol): Boolean {
    return isSubClassOf(subClass, superClass, allowIndirectSubtyping = false)
}

context(relationProvider: KaFirSymbolRelationProvider)
private fun isSubClassOf(subClass: KaClassSymbol, superClass: KaClassSymbol, allowIndirectSubtyping: Boolean): Boolean {
    require(subClass is KaFirSymbol<*>)
    require(superClass is KaFirSymbol<*>)

    if (subClass == superClass) return false
    return isSubclassOf(
        subclass = subClass.firSymbol.fir as FirClass,
        superclass = superClass.firSymbol.fir as FirClass,
        relationProvider.rootModuleSession,
        allowIndirectSubtyping,
    )
}

context(relationProvider: KaFirSymbolRelationProvider)
internal fun getIntersectionOverriddenSymbols(symbol: KaCallableSymbol): List<KaCallableSymbol> = with(analysisSession) {
    if (!symbol.mayHaveOverriddenCallables(skipIsOverrideCheck = false)) {
        return emptyList()
    }

    require(symbol is KaFirSymbol<*>)
    if (symbol.origin != KaSymbolOrigin.INTERSECTION_OVERRIDE) return emptyList()
    symbol.firSymbol
        .getIntersectionOverriddenSymbols(symbol.analysisSession.firSession)
        .map { firSymbolBuilder.callableBuilder.buildCallableSymbol(it) }
}

@OptIn(ScopeFunctionRequiresPrewarm::class)
context(analysisSession: KaFirSession)
private fun FirBasedSymbol<*>.getIntersectionOverriddenSymbols(useSiteSession: FirSession): Collection<FirCallableSymbol<*>> {
    require(this is FirCallableSymbol<*>) {
        "Required FirCallableSymbol but ${this::class} found"
    }
    return when (this) {
        is FirIntersectionCallableSymbol -> getNonSubsumedOverriddenSymbols(
            useSiteSession,
            analysisSession.getScopeSessionFor(useSiteSession)
        )
        else -> listOf(this)
    }
}
