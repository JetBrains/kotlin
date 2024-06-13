/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.analysis.api.components.KaSymbolRelationProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.buildSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.getClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.getContainingKtModule
import org.jetbrains.kotlin.analysis.api.fir.utils.withSymbolAttachment
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.isForeignValue
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getContainingFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.originalDeclaration
import org.jetbrains.kotlin.analysis.project.structure.DanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.project.structure.KtDanglingFileModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.getImplementationStatus
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.declarations.expectForActual
import org.jetbrains.kotlin.fir.declarations.getSealedClassInheritors
import org.jetbrains.kotlin.fir.diagnostics.ConeDestructuringDeclarationsOnTopLevel
import org.jetbrains.kotlin.fir.resolve.FirSamResolver
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.unwrapFakeOverridesOrDelegated
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility
import org.jetbrains.kotlin.util.ImplementationStatus
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal class KaFirSymbolRelationProvider(
    override val analysisSessionProvider: () -> KaFirSession
) : KaSessionComponent<KaFirSession>(), KaSymbolRelationProvider, KaFirSessionComponent {
    override val KaSymbol.containingSymbol: KaDeclarationSymbol?
        get() = withValidityAssertion {
            if (!hasParentSymbol(this)) {
                return null
            }

            getContainingDeclarationForDependentDeclaration(this)?.let { return it }

            val firSymbol = firSymbol
            val symbolFirSession = firSymbol.llFirSession
            val symbolModule = symbolFirSession.ktModule

            if (firSymbol is FirErrorPropertySymbol && firSymbol.diagnostic is ConeDestructuringDeclarationsOnTopLevel) {
                return null
            }

            if (symbolModule is KtDanglingFileModule && symbolModule.resolutionMode == DanglingFileResolutionMode.IGNORE_SELF) {
                if (hasParentPsi(this)) {
                    // getSymbol(ClassId) returns a symbol from the original file, so here we avoid using it
                    return getContainingDeclarationByPsi(this)
                }
            }

            when (this) {
                is KaLocalVariableSymbol,
                is KaAnonymousFunctionSymbol,
                is KaAnonymousObjectSymbol,
                is KaDestructuringDeclarationSymbol -> {
                    return getContainingDeclarationByPsi(this)
                }

                is KaClassInitializerSymbol -> {
                    val outerFirClassifier = firSymbol.getContainingClassSymbol(symbolFirSession)
                    if (outerFirClassifier != null) {
                        return firSymbolBuilder.buildSymbol(outerFirClassifier) as? KaDeclarationSymbol
                    }
                }

                is KaValueParameterSymbol -> {
                    return firSymbolBuilder.callableBuilder.buildCallableSymbol(this.firSymbol.fir.containingFunctionSymbol)
                }

                is KaCallableSymbol -> {
                    val outerFirClassifier = firSymbol.getContainingClassSymbol(symbolFirSession)
                    if (outerFirClassifier != null) {
                        return firSymbolBuilder.buildSymbol(outerFirClassifier) as? KaDeclarationSymbol
                    }

                    if (firSymbol.origin == FirDeclarationOrigin.DynamicScope) {
                        // A callable declaration from dynamic scope has no containing declaration as it comes from a dynamic type
                        // which is not based on a specific classifier
                        return null
                    }
                }

                is KaClassLikeSymbol -> {
                    val outerClassId = classId?.outerClassId
                    if (outerClassId != null) { // Won't work for local and top-level classes, or classes inside a script
                        val outerFirClassifier = symbolFirSession.firProvider.getFirClassifierByFqName(outerClassId) ?: return null
                        return firSymbolBuilder.buildSymbol(outerFirClassifier) as? KaDeclarationSymbol
                    }
                }
            }

            return getContainingDeclarationByPsi(this)
        }

    private fun hasParentSymbol(symbol: KaSymbol): Boolean {
        when (symbol) {
            is KaReceiverParameterSymbol -> {
                // KT-55124
                return true
            }

            !is KaDeclarationSymbol -> {
                // File, package, etc.
                return false
            }

            is KaSamConstructorSymbol -> {
                // SAM constructors are always top-level
                return false
            }

            is KaScriptSymbol -> {
                // Scripts are always top-level
                return false
            }

            else -> {}
        }

        if (symbol.isTopLevel) {
            val containingFile = (symbol.firSymbol.fir as? FirElementWithResolveState)?.getContainingFile()
            if (containingFile == null || containingFile.declarations.firstOrNull() !is FirScript) {
                // Should be replaced with proper check after KT-61451 and KT-61887
                return false
            }
        }

        val firSymbol = symbol.firSymbol
        if (firSymbol is FirPropertySymbol && firSymbol.isForeignValue) {
            return false
        }

        return true
    }

    fun getContainingDeclarationByPsi(symbol: KaSymbol): KaDeclarationSymbol? {
        val containingDeclaration = getContainingPsi(symbol) ?: return null
        return with(analysisSession) { containingDeclaration.symbol }
    }

    private fun getContainingDeclarationForDependentDeclaration(symbol: KaSymbol): KaDeclarationSymbol? {
        return when (symbol) {
            is KaReceiverParameterSymbol -> symbol.owningCallableSymbol
            is KaBackingFieldSymbol -> symbol.owningProperty
            is KaPropertyAccessorSymbol -> firSymbolBuilder.buildSymbol(symbol.firSymbol.propertySymbol) as KaDeclarationSymbol
            is KaTypeParameterSymbol -> firSymbolBuilder.buildSymbol(symbol.firSymbol.containingDeclarationSymbol) as? KaDeclarationSymbol
            is KaValueParameterSymbol -> firSymbolBuilder.buildSymbol(symbol.firSymbol.containingFunctionSymbol) as? KaDeclarationSymbol
            else -> null
        }
    }

    override val KaSymbol.containingFile: KaFileSymbol?
        get() = withValidityAssertion {
            if (this is KaFileSymbol) {
                return null
            }

            val firSymbol = when (this) {
                is KaFirReceiverParameterSymbol -> {
                    // symbol from receiver parameter
                    firSymbol
                }
                else -> {
                    // general FIR-based symbol
                    firSymbol
                }
            }

            val firFileSymbol = firSymbol.fir.getContainingFile()?.symbol ?: return null
            return firSymbolBuilder.buildFileSymbol(firFileSymbol)
        }

    override val KaSymbol.containingModule: KtModule
        get() = withValidityAssertion {
            getContainingKtModule(analysisSession.firResolveSession)
        }

    private fun getContainingPsi(symbol: KaSymbol): KtDeclaration? {
        val source = symbol.firSymbol.source
            ?: errorWithAttachment("PSI should present for declaration built by Kotlin code") {
                withSymbolAttachment("symbolForContainingPsi", analysisSession, symbol)
            }

        getContainingPsiForFakeSource(source)?.let { return it }

        val psi = source.psi
            ?: errorWithAttachment("PSI not found for source kind '${source.kind}'") {
                withSymbolAttachment("symbolForContainingPsi", analysisSession, symbol)
            }

        if (source.kind != KtRealSourceElementKind) {
            errorWithAttachment("Cannot compute containing PSI for unknown source kind '${source.kind}' (${psi::class.simpleName})") {
                withSymbolAttachment("symbolForContainingPsi", analysisSession, symbol)
            }
        }

        if (isSyntheticSymbolWithParentSource(symbol)) {
            return psi as KtDeclaration
        }

        if (isOrdinarySymbolWithSource(symbol)) {
            val result = psi.getContainingPsiDeclaration()

            if (result == null) {
                val containingFile = psi.containingFile
                if (containingFile is KtCodeFragment) {
                    // All content inside a code fragment is implicitly local, but there is no non-local parent
                    return null
                }

                errorWithAttachment("Containing declaration should present for nested declaration ${psi::class}") {
                    withSymbolAttachment("symbolForContainingPsi", analysisSession, symbol)
                }
            }

            return result
        }

        errorWithAttachment("Unsupported declaration origin ${symbol.origin} ${psi::class}") {
            withSymbolAttachment("symbolForContainingPsi", analysisSession, symbol)
        }
    }

    private fun hasParentPsi(symbol: KaSymbol): Boolean {
        val source = symbol.firSymbol.source?.takeIf { it.psi is KtElement } ?: return false

        return getContainingPsiForFakeSource(source) != null
                || isSyntheticSymbolWithParentSource(symbol)
                || isOrdinarySymbolWithSource(symbol)
    }

    private fun isSyntheticSymbolWithParentSource(symbol: KaSymbol): Boolean {
        return when (symbol.origin) {
            KaSymbolOrigin.SOURCE_MEMBER_GENERATED -> true
            else -> false
        }
    }

    private fun isOrdinarySymbolWithSource(symbol: KaSymbol): Boolean {
        return symbol.origin == KaSymbolOrigin.SOURCE
                || symbol.firSymbol.fir.origin == FirDeclarationOrigin.ScriptCustomization.ResultProperty
    }

    private fun getContainingPsiForFakeSource(source: KtSourceElement): KtDeclaration? {
        return when (source.kind) {
            KtFakeSourceElementKind.ImplicitConstructor -> source.psi as KtDeclaration
            KtFakeSourceElementKind.PropertyFromParameter -> source.psi?.parentOfType<KtPrimaryConstructor>()!!
            KtFakeSourceElementKind.EnumInitializer -> source.psi as KtEnumEntry
            KtFakeSourceElementKind.EnumGeneratedDeclaration -> source.psi as KtDeclaration
            KtFakeSourceElementKind.ScriptParameter -> source.psi as KtScript
            else -> null
        }
    }

    private fun PsiElement.getContainingPsiDeclaration(): KtDeclaration? {
        for (parent in parents) {
            if (parent is KtDeclaration && parent !is KtDestructuringDeclaration) {
                return parent.originalDeclaration ?: parent
            }
        }

        return null
    }

    override val KaClassLikeSymbol.samConstructor: KaSamConstructorSymbol?
        get() = withValidityAssertion {
            val classId = classId ?: return null
            val owner = analysisSession.getClassLikeSymbol(classId) ?: return null
            val firSession = analysisSession.firSession
            val resolver = FirSamResolver(firSession, analysisSession.getScopeSessionFor(firSession))
            return resolver.getSamConstructor(owner)?.let {
                analysisSession.firSymbolBuilder.functionLikeBuilder.buildSamConstructorSymbol(it.symbol)
            }
        }

    private val overridesProvider = KaFirSymbolDeclarationOverridesProvider(analysisSessionProvider)

    override val KaCallableSymbol.allOverriddenSymbols: Sequence<KaCallableSymbol>
        get() = withValidityAssertion {
            overridesProvider.getAllOverriddenSymbols(this)
        }

    override val KaCallableSymbol.directlyOverriddenSymbols: Sequence<KaCallableSymbol>
        get() = withValidityAssertion {
            overridesProvider.getDirectlyOverriddenSymbols(this)
        }

    override fun KaClassOrObjectSymbol.isSubClassOf(superClass: KaClassOrObjectSymbol): Boolean = withValidityAssertion {
        return overridesProvider.isSubClassOf(this, superClass)
    }

    override fun KaClassOrObjectSymbol.isDirectSubClassOf(superClass: KaClassOrObjectSymbol): Boolean = withValidityAssertion {
        return overridesProvider.isDirectSubClassOf(this, superClass)
    }

    override val KaCallableSymbol.intersectionOverriddenSymbols: List<KaCallableSymbol>
        get() = withValidityAssertion {
            overridesProvider.getIntersectionOverriddenSymbols(this)
        }

    override fun KaCallableSymbol.getImplementationStatus(parentClassSymbol: KaClassOrObjectSymbol): ImplementationStatus? {
        withValidityAssertion {
            require(this is KaFirSymbol<*>)
            require(parentClassSymbol is KaFirSymbol<*>)

            // Inspecting implementation status requires resolving to status
            val memberFir = firSymbol.fir as? FirCallableDeclaration ?: return null
            val parentClassFir = parentClassSymbol.firSymbol.fir as? FirClass ?: return null
            memberFir.lazyResolveToPhase(FirResolvePhase.STATUS)

            val scopeSession = analysisSession.getScopeSessionFor(analysisSession.firSession)
            return memberFir.symbol.getImplementationStatus(SessionHolderImpl(rootModuleSession, scopeSession), parentClassFir.symbol)
        }
    }

    override val KaCallableSymbol.fakeOverrideOriginal: KaCallableSymbol
        get() = withValidityAssertion {
            require(this is KaFirSymbol<*>)

            val originalDeclaration = firSymbol.fir as FirCallableDeclaration
            val unwrappedDeclaration = originalDeclaration.unwrapFakeOverridesOrDelegated()

            return unwrappedDeclaration.buildSymbol(analysisSession.firSymbolBuilder) as KaCallableSymbol
        }

    @Suppress("OVERRIDE_DEPRECATION")
    override val KaCallableSymbol.originalContainingClassForOverride: KaClassOrObjectSymbol?
        get() = withValidityAssertion {
            require(this is KaFirSymbol<*>)

            val targetDeclaration = firSymbol.fir as FirCallableDeclaration
            val unwrappedDeclaration = targetDeclaration.unwrapFakeOverridesOrDelegated()

            val unwrappedFirSymbol = unwrappedDeclaration.symbol
            val unwrappedKtSymbol = analysisSession.firSymbolBuilder.callableBuilder.buildCallableSymbol(unwrappedFirSymbol)
            return with(analysisSession) { unwrappedKtSymbol.containingSymbol as? KaClassOrObjectSymbol }
        }

    override fun KaDeclarationSymbol.getExpectsForActual(): List<KaDeclarationSymbol> = withValidityAssertion {
        require(this is KaFirSymbol<*>)
        val firSymbol = firSymbol
        if (firSymbol !is FirCallableSymbol && firSymbol !is FirClassSymbol && firSymbol !is FirTypeAliasSymbol) {
            return emptyList()
        }

        return firSymbol.expectForActual?.get(ExpectActualMatchingCompatibility.MatchedSuccessfully)
            ?.map { analysisSession.firSymbolBuilder.buildSymbol(it) as KaDeclarationSymbol }.orEmpty()
    }

    override val KaNamedClassOrObjectSymbol.sealedClassInheritors: List<KaNamedClassOrObjectSymbol>
        get() = withValidityAssertion {
            require(modality == Modality.SEALED)
            require(this is KaFirNamedClassOrObjectSymbol)

            val inheritorClassIds = firSymbol.fir.getSealedClassInheritors(analysisSession.firSession)

            return with(analysisSession) {
                inheritorClassIds.mapNotNull { findClass(it) as? KaNamedClassOrObjectSymbol }
            }
        }

    @Deprecated("Use the declaration scope instead.")
    override val KaNamedClassOrObjectSymbol.enumEntries: List<KaEnumEntrySymbol>
        get() = withValidityAssertion {
            require(classKind == KaClassKind.ENUM_CLASS)
            return with(analysisSession) {
                staticDeclaredMemberScope.callables.filterIsInstance<KaEnumEntrySymbol>().toList()
            }
        }
}