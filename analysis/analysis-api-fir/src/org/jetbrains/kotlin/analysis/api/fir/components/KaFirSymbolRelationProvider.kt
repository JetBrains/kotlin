/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.components.KaSymbolRelationProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.buildSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirConstructorSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.getClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.getContainingKtModule
import org.jetbrains.kotlin.analysis.api.fir.utils.withSymbolAttachment
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.isForeignValue
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getContainingFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.originalDeclaration
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.getImplementationStatus
import org.jetbrains.kotlin.fir.containingClassForLocalAttr
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.ConeDestructuringDeclarationsOnTopLevel
import org.jetbrains.kotlin.fir.resolve.FirSamResolver
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.fir.unwrapFakeOverridesOrDelegated
import org.jetbrains.kotlin.ir.util.kotlinPackageFqn
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility
import org.jetbrains.kotlin.util.ImplementationStatus
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal class KaFirSymbolRelationProvider(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaSymbolRelationProvider, KaFirSessionComponent {
    override val KaSymbol.containingSymbol: KaSymbol?
        get() = withValidityAssertion {
            when (this) {
                is KaPackageSymbol -> null
                is KaFileSymbol -> analysisSession.firSymbolBuilder.createPackageSymbol(kotlinPackageFqn)
                else -> containingDeclaration ?: containingFile
            }
        }

    override val KaSymbol.containingDeclaration: KaDeclarationSymbol?
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

            if (symbolModule is KaDanglingFileModule && symbolModule.resolutionMode == KaDanglingFileResolutionMode.IGNORE_SELF) {
                if (hasParentPsi(this)) {
                    // getSymbol(ClassId) returns a symbol from the original file, so here we avoid using it
                    return getContainingDeclarationByPsi(this)
                }
            }

            when (this) {
                is KaLocalVariableSymbol,
                is KaAnonymousFunctionSymbol,
                is KaAnonymousObjectSymbol,
                is KaDestructuringDeclarationSymbol,
                    -> {
                    return getContainingDeclarationByPsi(this)
                }

                is KaClassInitializerSymbol -> {
                    val outerFirClassifier = firSymbol.getContainingClassSymbol()
                    if (outerFirClassifier != null) {
                        return firSymbolBuilder.buildSymbol(outerFirClassifier) as? KaDeclarationSymbol
                    }
                }

                is KaCallableSymbol -> {
                    val typeAliasForConstructor = (firSymbol as? FirConstructorSymbol)?.typeAliasConstructorInfo?.typeAliasSymbol
                    if (typeAliasForConstructor != null) {
                        return firSymbolBuilder.classifierBuilder.buildTypeAliasSymbol(typeAliasForConstructor)
                    }

                    val outerFirClassifier = firSymbol.getContainingClassSymbol()
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
                    firSymbol.getContainingClassSymbol()?.let { outerFirClassifier ->
                        return firSymbolBuilder.buildSymbol(outerFirClassifier) as? KaDeclarationSymbol
                    }
                    getContainingDeclarationsForLocalClass(firSymbol, symbolFirSession)?.let { return it }
                }
            }

            return getContainingDeclarationByPsi(this)
        }

    private fun getContainingDeclarationsForLocalClass(firSymbol: FirBasedSymbol<*>, symbolFirSession: FirSession): KaDeclarationSymbol? {
        val fir = firSymbol.fir as? FirRegularClass ?: return null
        val containerSymbol = fir.containingClassForLocalAttr?.toSymbol(symbolFirSession) ?: return null
        return firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(containerSymbol)
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

            @OptIn(DirectDeclarationsAccess::class)
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

    private fun getContainingDeclarationForDependentDeclaration(symbol: KaSymbol): KaDeclarationSymbol? = when (symbol) {
        is KaReceiverParameterSymbol -> symbol.owningCallableSymbol
        is KaBackingFieldSymbol -> symbol.owningProperty
        is KaPropertyAccessorSymbol -> firSymbolBuilder.buildSymbol(symbol.firSymbol.propertySymbol) as KaDeclarationSymbol
        is KaTypeParameterSymbol -> firSymbolBuilder.buildSymbol(symbol.firSymbol.containingDeclarationSymbol) as? KaDeclarationSymbol
        is KaValueParameterSymbol -> firSymbolBuilder.buildSymbol(symbol.firSymbol.containingDeclarationSymbol) as? KaDeclarationSymbol
        is KaContextParameterSymbol -> firSymbolBuilder.buildSymbol(symbol.firSymbol.containingDeclarationSymbol) as? KaDeclarationSymbol
        else -> null
    }

    override val KaSymbol.containingFile: KaFileSymbol?
        get() = withValidityAssertion {
            if (this is KaFileSymbol) {
                return null
            }

            val firFileSymbol = this.firSymbol.fir.getContainingFile()?.symbol ?: return null
            return firSymbolBuilder.buildFileSymbol(firFileSymbol)
        }

    override val KaSymbol.containingModule: KaModule
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
            KtFakeSourceElementKind.DataClassGeneratedMembers -> when (val source = source.psi) {
                is KtClassOrObject -> {
                    // for generated `equals`, `hashCode`, `toString` methods the source is the containing `KtClass`
                    source
                }
                is KtParameter -> {
                    // for `componentN` functions, the source points to the parameter by which the `componentN` function was generated
                    val constructor = source.ownerFunction as KtPrimaryConstructor
                    constructor.containingClassOrObject!!
                }
                is KtPrimaryConstructor -> {
                    source.containingClassOrObject!!
                }
                else -> null
            }
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
                analysisSession.firSymbolBuilder.functionBuilder.buildSamConstructorSymbol(it.symbol)
            }
        }

    override val KaSamConstructorSymbol.constructedClass: KaClassLikeSymbol
        get() = withValidityAssertion {
            firSymbol.fir.returnTypeRef.coneType.classId?.toLookupTag()?.let {
                analysisSession.firSymbolBuilder.classifierBuilder.buildClassLikeSymbolByLookupTag(it)
            } ?: errorWithAttachment("Cannot retrieve constructed class for KaSamConstructorSymbol") {
                withSymbolAttachment("KaSamConstructorSymbol", analysisSession, this@constructedClass)
            }
        }

    @KaExperimentalApi
    override val KaConstructorSymbol.originalConstructorIfTypeAliased: KaConstructorSymbol?
        get() = withValidityAssertion {
            require(this is KaFirConstructorSymbol)

            val originalConstructor = firSymbol.typeAliasConstructorInfo?.originalConstructor as? FirConstructor ?: return null

            analysisSession.firSymbolBuilder.functionBuilder.buildConstructorSymbol(originalConstructor.symbol)
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

    override fun KaClassSymbol.isSubClassOf(superClass: KaClassSymbol): Boolean = withValidityAssertion {
        return overridesProvider.isSubClassOf(this, superClass)
    }

    override fun KaClassSymbol.isDirectSubClassOf(superClass: KaClassSymbol): Boolean = withValidityAssertion {
        return overridesProvider.isDirectSubClassOf(this, superClass)
    }

    override val KaCallableSymbol.intersectionOverriddenSymbols: List<KaCallableSymbol>
        get() = withValidityAssertion {
            overridesProvider.getIntersectionOverriddenSymbols(this)
        }

    override fun KaCallableSymbol.getImplementationStatus(parentClassSymbol: KaClassSymbol): ImplementationStatus? {
        withValidityAssertion {
            if (this is KaReceiverParameterSymbol) return null

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
            if (this is KaReceiverParameterSymbol) return this

            require(this is KaFirSymbol<*>)

            val originalDeclaration = firSymbol.fir as FirCallableDeclaration
            val unwrappedDeclaration = originalDeclaration.unwrapFakeOverridesOrDelegated()

            return unwrappedDeclaration.buildSymbol(analysisSession.firSymbolBuilder) as KaCallableSymbol
        }

    override fun KaDeclarationSymbol.getExpectsForActual(): List<KaDeclarationSymbol> = withValidityAssertion {
        if (this is KaReceiverParameterSymbol) {
            this.firSymbol.expectForActual?.get(ExpectActualMatchingCompatibility.MatchedSuccessfully).orEmpty()
                .filterIsInstance<FirCallableSymbol<*>>()
                // TODO: KT-73050. This code in fact does nothing
                .mapNotNull { callableSymbol ->
                    callableSymbol.receiverParameterSymbol?.let {
                        analysisSession.firSymbolBuilder.callableBuilder.buildExtensionReceiverSymbol(it)
                    }
                }
        }

        require(this is KaFirSymbol<*>)
        val firSymbol = firSymbol
        if (firSymbol !is FirCallableSymbol && firSymbol !is FirClassSymbol && firSymbol !is FirTypeAliasSymbol) {
            return emptyList()
        }

        return firSymbol.expectForActual?.get(ExpectActualMatchingCompatibility.MatchedSuccessfully)
            ?.map { analysisSession.firSymbolBuilder.buildSymbol(it) as KaDeclarationSymbol }.orEmpty()
    }

    override val KaNamedClassSymbol.sealedClassInheritors: List<KaNamedClassSymbol>
        get() = withValidityAssertion {
            require(modality == KaSymbolModality.SEALED)
            require(this is KaFirNamedClassSymbol)

            val inheritorClassIds = firSymbol.fir.getSealedClassInheritors(analysisSession.firSession)

            return with(analysisSession) {
                inheritorClassIds.mapNotNull { findClass(it) as? KaNamedClassSymbol }
            }
        }
}