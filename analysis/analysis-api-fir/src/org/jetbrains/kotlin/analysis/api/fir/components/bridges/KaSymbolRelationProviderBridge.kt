/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.components.KaCallableImplementationState
import org.jetbrains.kotlin.analysis.api.components.KaSymbolRelationProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.KaFirSessionComponent
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsSymbolRelationProvider
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.fir.analysis.checkers.getImplementationStatus
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.util.ImplementationStatus
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableImplementationState as KaEndpointCallableImplementationState
import org.jetbrains.kotlin.analysis.api.symbols.allOverriddenSymbols as allOverriddenSymbolsEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.containingDeclaration as containingDeclarationEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.containingFile as containingFileEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.containingModule as containingModuleEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.containingSymbol as containingSymbolEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.directlyOverriddenSymbols as directlyOverriddenSymbolsEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.fakeOverrideOriginal as fakeOverrideOriginalEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.functionalInterface as functionalInterfaceEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.functionalInterfaceFunction as functionalInterfaceFunctionEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.getExpectsForActual as getExpectsForActualEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.hasConflictingSignatureWith as hasConflictingSignatureWithEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.implementationState as implementationStateEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.intersectionOverriddenSymbols as intersectionOverriddenSymbolsEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.isDirectSubClassOf as isDirectSubClassOfEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.isSubClassOf as isSubClassOfEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.originalConstructorIfTypeAliased as originalConstructorIfTypeAliasedEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.samConstructor as samConstructorEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.sealedClassInheritors as sealedClassInheritorsEndpoint

@OptIn(KaExperimentalApi::class)
internal class KaSymbolRelationProviderBridge(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaSymbolRelationProvider, KaFirSessionComponent {
    private val proxy: KaInternalsSymbolRelationProvider
        get() = analysisSession.symbolRelationProvider

    override val KaSymbol.containingSymbol: KaSymbol?
        get() = context(analysisSession) { containingSymbolEndpoint }

    override val KaSymbol.containingDeclaration: KaDeclarationSymbol?
        get() = context(analysisSession) { containingDeclarationEndpoint }

    override val KaSymbol.containingFile: KaFileSymbol?
        get() = context(analysisSession) { containingFileEndpoint }

    override val KaSymbol.containingModule: KaModule
        get() = context(analysisSession) { containingModuleEndpoint }

    override val KaClassLikeSymbol.samConstructor: KaSamConstructorSymbol?
        get() = context(analysisSession) { samConstructorEndpoint }

    @KaExperimentalApi
    override val KaClassLikeSymbol.functionalInterfaceFunction: KaNamedFunctionSymbol?
        get() = context(analysisSession) { functionalInterfaceFunctionEndpoint }

    override val KaSamConstructorSymbol.functionalInterface: KaClassLikeSymbol
        get() = context(analysisSession) { functionalInterfaceEndpoint }

    @KaExperimentalApi
    override val KaSamConstructorSymbol.functionalInterfaceFunction: KaNamedFunctionSymbol
        get() = context(analysisSession) { functionalInterfaceFunctionEndpoint }

    @KaExperimentalApi
    override val KaConstructorSymbol.originalConstructorIfTypeAliased: KaConstructorSymbol?
        get() = context(analysisSession) { originalConstructorIfTypeAliasedEndpoint }

    override val KaCallableSymbol.allOverriddenSymbols: Sequence<KaCallableSymbol>
        get() = context(analysisSession) { allOverriddenSymbolsEndpoint }

    override val KaCallableSymbol.directlyOverriddenSymbols: Sequence<KaCallableSymbol>
        get() = context(analysisSession) { directlyOverriddenSymbolsEndpoint }

    override fun KaClassSymbol.isSubClassOf(superClass: KaClassSymbol): Boolean =
        context(analysisSession) { isSubClassOfEndpoint(superClass) }

    override fun KaClassSymbol.isDirectSubClassOf(superClass: KaClassSymbol): Boolean =
        context(analysisSession) { isDirectSubClassOfEndpoint(superClass) }

    override val KaCallableSymbol.intersectionOverriddenSymbols: List<KaCallableSymbol>
        get() = context(analysisSession) { intersectionOverriddenSymbolsEndpoint }

    @KaExperimentalApi
    @Deprecated("Use 'implementationState()' instead", level = DeprecationLevel.HIDDEN)
    override fun KaCallableSymbol.getImplementationStatus(parentClassSymbol: KaClassSymbol): ImplementationStatus? =
        withValidityAssertion {
            if (this is KaReceiverParameterSymbol) return null

            require(this is KaFirSymbol<*>)
            require(parentClassSymbol is KaFirSymbol<*>)

            // Inspecting implementation status requires resolving to status
            val memberFir = firSymbol.fir as? FirCallableDeclaration ?: return null
            val parentClassFir = parentClassSymbol.firSymbol.fir as? FirClass ?: return null
            memberFir.lazyResolveToPhase(FirResolvePhase.STATUS)

            val scopeSession = analysisSession.getScopeSessionFor(analysisSession.firSession)
            with(SessionHolderImpl(rootModuleSession, scopeSession)) {
                memberFir.symbol.getImplementationStatus(parentClassFir.symbol)
            }
        }

    @KaExperimentalApi
    override fun KaCallableSymbol.implementationState(implementerClassSymbol: KaClassSymbol): KaCallableImplementationState? =
        context(analysisSession) { implementationStateEndpoint(implementerClassSymbol) }?.toLegacyImplementationState()

    override val KaCallableSymbol.fakeOverrideOriginal: KaCallableSymbol
        get() = context(analysisSession) { fakeOverrideOriginalEndpoint }

    @KaExperimentalApi
    override fun KaDeclarationSymbol.getExpectsForActual(): List<KaDeclarationSymbol> =
        context(analysisSession) { getExpectsForActualEndpoint() }

    override val KaNamedClassSymbol.sealedClassInheritors: List<KaNamedClassSymbol>
        get() = context(analysisSession) { sealedClassInheritorsEndpoint }

    override fun KaFunctionSymbol.hasConflictingSignatureWith(other: KaFunctionSymbol, targetPlatform: TargetPlatform): Boolean =
        context(analysisSession) { hasConflictingSignatureWithEndpoint(other, targetPlatform) }
}

@OptIn(KaExperimentalApi::class)
private fun KaEndpointCallableImplementationState.toLegacyImplementationState(): KaCallableImplementationState = when (this) {
    // The engine produces shared impl instances (KaCallable*ImplementationStateImpl) that implement both the new endpoint
    // and the legacy supporting type, so narrowing back to the legacy surface preserves identity and rendering.
    is KaEndpointCallableImplementationState.Explicit -> this as KaCallableImplementationState.Explicit
    is KaEndpointCallableImplementationState.Inherited -> this as KaCallableImplementationState.Inherited
    is KaEndpointCallableImplementationState.Missing -> this as KaCallableImplementationState.Missing
    else -> error("Unexpected implementation state: $this")
}
