/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.PsiElement
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.buildSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.*
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.getClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.getContainingKtModule
import org.jetbrains.kotlin.analysis.api.fir.utils.withSymbolAttachment
import org.jetbrains.kotlin.analysis.api.impl.base.KaCallableExplicitImplementationStateImpl
import org.jetbrains.kotlin.analysis.api.impl.base.KaCallableInheritedImplementationStateImpl
import org.jetbrains.kotlin.analysis.api.impl.base.KaCallableMissingImplementationStateImpl
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.findSyntheticJavaPropertyAccessor
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsSymbolRelationProvider
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.projectStructure.*
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getContainingFile
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.getImplementationStatus
import org.jetbrains.kotlin.fir.analysis.checkers.isSupertypeOf
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOverloadabilityHelper.ContextParameterShadowing.BothWays
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.diagnostics.ConeDestructuringDeclarationsOnTopLevel
import org.jetbrains.kotlin.fir.resolve.FirSamResolver
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.fir.resolve.calls.FirSimpleSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.mpp.FirExpectActualMatchingContextImpl
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.ir.util.kotlinPackageFqn
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassIdBasedLocality
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.psi.psiUtil.isExpectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualMatcher
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility
import org.jetbrains.kotlin.util.ImplementationStatus
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.analysis.api.fir.components.isDirectSubClassOf as computeIsDirectSubClassOf
import org.jetbrains.kotlin.analysis.api.fir.components.isSubClassOf as computeIsSubClassOf

internal class KaFirSymbolRelationProvider(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaInternalsSymbolRelationProvider, KaFirSessionComponent {
    override fun containingSymbol(symbol: KaSymbol): KaSymbol? = withValidityAssertion {
        when (symbol) {
            is KaPackageSymbol -> null
            is KaFileSymbol -> analysisSession.firSymbolBuilder.createPackageSymbol(kotlinPackageFqn)
            else -> containingDeclaration(symbol) ?: containingFile(symbol)
        }
    }

    override fun containingDeclaration(symbol: KaSymbol): KaDeclarationSymbol? = withValidityAssertion {
        if (!hasParentSymbol(symbol)) {
            return null
        }

        getContainingDeclarationForDependentDeclaration(symbol)?.let { return it }

        // Handle intersection overrides on synthetic properties
        val firSymbol = (symbol.firSymbol as? FirSimpleSyntheticPropertySymbol)?.getterSymbol?.delegateFunctionSymbol
            ?: symbol.firSymbol
        val symbolFirSession = firSymbol.llFirSession
        val symbolModule = symbolFirSession.ktModule

        if (firSymbol is FirErrorPropertySymbol && firSymbol.diagnostic is ConeDestructuringDeclarationsOnTopLevel) {
            return null
        }

        if (symbolModule is KaDanglingFileModule && symbolModule.resolutionMode == KaDanglingFileResolutionMode.IGNORE_SELF) {
            if (hasParentPsi(symbol)) {
                // getSymbol(ClassId) returns a symbol from the original file, so here we avoid using it
                return getContainingDeclarationByPsi(symbol)
            }
        }

        when (symbol) {
            is KaLocalVariableSymbol,
            is KaAnonymousFunctionSymbol,
            is KaAnonymousObjectSymbol,
            is KaDestructuringDeclarationSymbol,
                -> {
                return getContainingDeclarationByPsi(symbol)
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

        return getContainingDeclarationByPsi(symbol)
    }

    private fun getContainingDeclarationsForLocalClass(firSymbol: FirBasedSymbol<*>, symbolFirSession: FirSession): KaDeclarationSymbol? {
        val fir = firSymbol.fir as? FirClassLikeDeclaration ?: return null
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

        return when (symbol) {
            is KaKotlinPropertySymbol, is KaLocalVariableSymbol -> symbol.firSymbol.origin != FirDeclarationOrigin.ForeignValue
            else -> true
        }
    }

    private fun getContainingDeclarationByPsi(symbol: KaSymbol): KaDeclarationSymbol? {
        val containingDeclaration = getContainingPsi(symbol) ?: return null
        val declarationSymbol = with(analysisSession) { containingDeclaration.symbol }

        if (declarationSymbol is KaFirEnumEntrySymbol && symbol !is KaFirEnumEntryInitializerSymbol) {
            // From the FIR point of view, the real containing declaration of enum entry functions or implicit constructor
            // is not the enum entry itself, but its `KaFirEnumEntryInitializerSymbol`.
            return declarationSymbol.initializer
        }

        return declarationSymbol
    }

    private fun getContainingDeclarationForDependentDeclaration(symbol: KaSymbol): KaDeclarationSymbol? = when (symbol) {
        is KaReceiverParameterSymbol -> symbol.owningCallableSymbol
        is KaBackingFieldSymbol -> symbol.owningProperty
        is KaPropertyAccessorSymbol -> firSymbolBuilder.buildSymbol(symbol.firSymbol.propertySymbol) as KaDeclarationSymbol
        is KaTypeParameterSymbol -> firSymbolBuilder.buildSymbol(symbol.firSymbol.containingDeclarationSymbol) as? KaDeclarationSymbol
        is KaValueParameterSymbol -> firSymbolBuilder.buildSymbol(symbol.firSymbol.containingDeclarationSymbol) as? KaDeclarationSymbol
        is KaContextParameterSymbol -> {
            val containingFirSymbol = symbol.firSymbol.containingDeclarationSymbol
            val firSymbol = if (containingFirSymbol is FirDanglingModifierSymbol) {
                containingFirSymbol.getContainingClassSymbol()
                    ?: containingFirSymbol.fir.getContainingFile()?.symbol
                    ?: errorWithAttachment("Containing element is expected for the dangling modifier symbol") {
                        withSymbolAttachment("symbolForContainingPsi", analysisSession, symbol)
                        withFirSymbolEntry("containingFirSymbol", containingFirSymbol)
                    }
            } else {
                containingFirSymbol
            }

            firSymbolBuilder.buildSymbol(firSymbol) as? KaDeclarationSymbol
        }
        else -> null
    }

    override fun containingFile(symbol: KaSymbol): KaFileSymbol? = withValidityAssertion {
        if (symbol is KaFileSymbol || symbol is KaPackageSymbol) {
            return null
        }

        val firFileSymbol = symbol.firSymbol.fir.getContainingFile()?.symbol ?: return null
        return firSymbolBuilder.buildFileSymbol(firFileSymbol)
    }

    override fun containingModule(symbol: KaSymbol): KaModule = withValidityAssertion {
        when (symbol) {
            is KaFirSymbol<*> -> symbol.firSymbol.getContainingKtModule(resolutionFacade)
            is KaPackageSymbol -> analysisSession.useSiteModule
            else -> errorWithAttachment("Unsupported symbol type: ${this::class.simpleName}") {
                withSymbolAttachment("symbol", analysisSession, symbol)
            }
        }
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

                if (psi.parentOfType<KtModifierList>() != null) {
                    // Invalid code: the declaration is nested inside a dangling annotation
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
            // A property synthesized from a constructor parameter is still a class member, not a constructor member.
            KtFakeSourceElementKind.PropertyFromParameter -> source.psi?.parentOfType<KtClassOrObject>()!!
            KtFakeSourceElementKind.EnumInitializer -> source.psi as KtEnumEntry
            is KtFakeSourceElementKind.EnumGeneratedDeclaration -> source.psi as KtDeclaration
            is KtFakeSourceElementKind.ScriptParameter -> source.psi as KtScript
            is KtFakeSourceElementKind.DataClassGeneratedMembers -> when (val source = source.psi) {
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
                return parent
            }
        }

        return null
    }

    override fun samConstructor(symbol: KaClassLikeSymbol): KaSamConstructorSymbol? = withValidityAssertion {
        val classId = symbol.classId ?: return null
        val owner = analysisSession.getClassLikeSymbol(classId) ?: return null
        val firSession = analysisSession.firSession
        val resolver = FirSamResolver(firSession, analysisSession.getScopeSessionFor(firSession))
        return resolver.getSamConstructor(owner)?.let {
            analysisSession.firSymbolBuilder.functionBuilder.buildSamConstructorSymbol(it.symbol)
        }
    }

    override fun functionalInterfaceFunction(symbol: KaClassLikeSymbol): KaNamedFunctionSymbol? = withValidityAssertion {
        val classId = symbol.classId ?: return null
        val firClassOrTypeAlias = analysisSession.getClassLikeSymbol(classId) ?: return null
        val firSession = analysisSession.firSession
        val resolver = FirSamResolver(firSession, analysisSession.getScopeSessionFor(firSession))
        val samFunction = resolver.getSamFunction(firClassOrTypeAlias) ?: return null
        return analysisSession.firSymbolBuilder.functionBuilder.buildNamedFunctionSymbol(samFunction)
    }

    override fun functionalInterface(symbol: KaSamConstructorSymbol): KaClassLikeSymbol = withValidityAssertion {
        symbol.firSymbol.fir.returnTypeRef.coneType.classId?.toLookupTag()?.let {
            analysisSession.firSymbolBuilder.classifierBuilder.buildClassLikeSymbolByLookupTag(it)
        } ?: errorWithAttachment("Cannot retrieve functional interface for KaSamConstructorSymbol") {
            withSymbolAttachment("KaSamConstructorSymbol", analysisSession, symbol)
        }
    }

    override fun functionalInterfaceFunction(symbol: KaSamConstructorSymbol): KaNamedFunctionSymbol = withValidityAssertion {
        functionalInterfaceFunction(functionalInterface(symbol))
            ?: errorWithAttachment("SAM constructor should have a corresponding function in its functional interface") {
                withSymbolAttachment("KaSamConstructorSymbol", analysisSession, symbol)
                withSymbolAttachment("functionalInterface", analysisSession, functionalInterface(symbol))
            }
    }

    @KaExperimentalApi
    override fun originalConstructorIfTypeAliased(symbol: KaConstructorSymbol): KaConstructorSymbol? = withValidityAssertion {
        require(symbol is KaFirConstructorSymbol)

        val originalConstructor = symbol.firSymbol.typeAliasConstructorInfo?.originalConstructor as? FirConstructor ?: return null

        analysisSession.firSymbolBuilder.functionBuilder.buildConstructorSymbol(originalConstructor.symbol)
    }

    override fun allOverriddenSymbols(symbol: KaCallableSymbol): Sequence<KaCallableSymbol> = withValidityAssertion {
        symbol.handleOverriddenSymbols(
            forAccessorSymbol = { getAllOverriddenAccessorSymbols(it) },
            fallback = { getAllOverriddenSymbols(it) },
        )
    }

    override fun directlyOverriddenSymbols(symbol: KaCallableSymbol): Sequence<KaCallableSymbol> = withValidityAssertion {
        symbol.handleOverriddenSymbols(
            forAccessorSymbol = { getDirectlyOverriddenAccessorSymbols(it) },
            fallback = { getDirectlyOverriddenSymbols(it) },
        )
    }

    override fun isSubClassOf(symbol: KaClassSymbol, superClass: KaClassSymbol): Boolean = withValidityAssertion {
        return computeIsSubClassOf(symbol, superClass)
    }

    override fun isDirectSubClassOf(symbol: KaClassSymbol, superClass: KaClassSymbol): Boolean = withValidityAssertion {
        return computeIsDirectSubClassOf(symbol, superClass)
    }

    private inline fun KaCallableSymbol.handleOverriddenSymbols(
        forAccessorSymbol: (KaPropertyAccessorSymbol) -> Sequence<KaCallableSymbol>,
        fallback: (KaCallableSymbol) -> Sequence<KaCallableSymbol>,
    ): Sequence<KaCallableSymbol> = when (this) {
        is KaValueParameterSymbol -> this.primaryConstructorProperty?.let(fallback).orEmpty()
        is KaPropertyAccessorSymbol -> forAccessorSymbol(this)
        is KaNamedFunctionSymbol -> getSyntheticJavaPropertyAccessor(this)?.let(forAccessorSymbol) ?: fallback(this)
        else -> fallback(this)
    }

    override fun intersectionOverriddenSymbols(symbol: KaCallableSymbol): List<KaCallableSymbol> = withValidityAssertion {
        symbol.handleOverriddenSymbols(
            forAccessorSymbol = { getIntersectionOverriddenAccessorSymbols(it).asSequence() },
            fallback = { getIntersectionOverriddenSymbols(it).asSequence() },
        ).toList()
    }

    /**
     * Finds the synthetic property accessor corresponding to the given Java getter or setter method.
     *
     * When Kotlin accesses Java classes, it synthesizes properties from Java getter/setter method pairs
     * (e.g., `getField()`/`setField()` become a synthetic `field` property). This function performs
     * the reverse lookup: given a Java method symbol, it finds the corresponding accessor of the
     * synthetic property.
     *
     * This is used to compute overridden symbols for Java methods that participate in
     * synthetic property generation, and the synthetic property overrides a real Kotlin property.
     *
     * @param functionSymbol A function symbol representing a Java getter or setter method
     * @return The getter or setter of the synthetic property that wraps the given Java method, or `null` if not applicable
     */
    private fun getSyntheticJavaPropertyAccessor(functionSymbol: KaNamedFunctionSymbol): KaPropertyAccessorSymbol? {
        val origin = functionSymbol.origin
        if (origin != KaSymbolOrigin.JAVA_SOURCE && origin != KaSymbolOrigin.JAVA_LIBRARY) return null

        return with(analysisSession) {
            val containingClass = functionSymbol.containingDeclaration as? KaClassSymbol ?: return null
            containingClass.combinedDeclaredMemberScope
                .findSyntheticJavaPropertyAccessor(functionSymbol.name) { propertySymbol, accessorKind, _ ->
                    if (accessorKind.getJavaAccessorSymbol(propertySymbol) == functionSymbol) {
                        accessorKind.getPropertyAccessorSymbol(propertySymbol)
                    } else {
                        null
                    }
                }
        }
    }

    override fun implementationState(
        symbol: KaCallableSymbol,
        implementerClassSymbol: KaClassSymbol,
    ): KaCallableImplementationState? = withValidityAssertion {
        require(symbol is KaFirSymbol<*>)
        require(implementerClassSymbol is KaFirSymbol<*>)

        when (symbol) {
            is KaNamedFunctionSymbol, is KaPropertySymbol, is KaPropertyAccessorSymbol -> {
                val memberFirSymbol = symbol.firSymbol as? FirCallableSymbol<*> ?: return null

                val memberClassFirSymbol = memberFirSymbol.getContainingClassSymbol() as? FirClassSymbol<*> ?: return null
                memberClassFirSymbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)

                val implementerClassFirSymbol = implementerClassSymbol.firSymbol as? FirClassSymbol<*> ?: return null
                implementerClassFirSymbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)

                if (
                    memberClassFirSymbol != implementerClassFirSymbol &&
                    !memberClassFirSymbol.isSupertypeOf(implementerClassFirSymbol, rootModuleSession)
                ) {
                    return null
                }

                memberFirSymbol.lazyResolveToPhase(FirResolvePhase.STATUS)

                val scopeSession = analysisSession.getScopeSessionFor(analysisSession.firSession)
                with(SessionHolderImpl(rootModuleSession, scopeSession)) {
                    return memberFirSymbol.getImplementationStatus(implementerClassFirSymbol).toKaImplementationState()
                }
            }
            else -> {
                return null
            }
        }
    }

    override fun fakeOverrideOriginal(symbol: KaCallableSymbol): KaCallableSymbol = withValidityAssertion {
        require(symbol is KaFirSymbol<*>)
        if (symbol is KaLocalVariableSymbol ||
            symbol is KaEnumEntrySymbol ||
            symbol is KaConstructorSymbol ||
            symbol is KaSamConstructorSymbol ||
            symbol is KaAnonymousFunctionSymbol
        ) {
            return symbol
        }

        // Fake-override attributes are placed on the first-class callables, not on their sub-callables (e.g., value parameters).
        // So for such callables we unwrap the enclosing callable and return the corresponding sub-callable from the base declaration.
        return when (symbol) {
            is KaReceiverParameterSymbol -> {
                val original = fakeOverrideOriginal(symbol.owningCallableSymbol)
                original.receiverParameter
            }
            is KaValueParameterSymbol -> {
                val owner = containingDeclaration(symbol) as? KaFunctionSymbol ?: return symbol
                val index = owner.valueParameters.indexOf(symbol)
                val original = fakeOverrideOriginal(owner) as? KaFunctionSymbol
                original?.valueParameters?.getOrNull(index)
            }
            is KaContextParameterSymbol -> {
                val owner = containingDeclaration(symbol) as? KaCallableSymbol ?: return symbol
                val index = owner.contextParameters.indexOf(symbol)
                val original = fakeOverrideOriginal(owner)
                original.contextParameters.getOrNull(index)
            }
            is KaPropertyAccessorSymbol -> {
                val owner = containingDeclaration(symbol) as? KaPropertySymbol ?: return symbol
                val original = fakeOverrideOriginal(owner) as? KaPropertySymbol ?: return symbol
                if (symbol is KaPropertyGetterSymbol) {
                    original.getter
                } else {
                    original.setter
                }
            }
            else -> {
                val originalDeclaration = symbol.firSymbol.fir as FirCallableDeclaration
                val unwrappedDeclaration = originalDeclaration.unwrapFakeOverridesOrDelegated()
                unwrappedDeclaration.buildSymbol(analysisSession.firSymbolBuilder) as KaCallableSymbol
            }
        } ?: symbol
    }


    override fun getExpectsForActual(symbol: KaDeclarationSymbol): List<KaDeclarationSymbol> = withValidityAssertion {
        symbol.getExpectsForActualSpecial()?.let { return it }

        require(symbol is KaFirSymbol<*>)
        val firSymbol = symbol.firSymbol
        if (firSymbol !is FirCallableSymbol && firSymbol !is FirClassSymbol && firSymbol !is FirTypeAliasSymbol) {
            return emptyList()
        }

        val actualModule = containingModule(symbol)
        if (actualModule is KaLibraryModule || actualModule is KaLibrarySourceModule) {
            // Dependency tree for libraries isn't available (KT-61210) so there's no way other than checking the entire library scope.
            // Notably, the current library isn't filtered as the same library may contain binary roots for both common and platform parts.
            val expectLibraryScope = ProjectScope.getLibrariesScope(project)

            val expectDeclarationProvider = KotlinDeclarationProviderFactory.getInstance(project)
                .createDeclarationProvider(expectLibraryScope, analysisSession.useSiteModule)

            val firSymbols = when (firSymbol) {
                is FirClassLikeSymbol -> computeExpectsForLibraryClass(firSymbol, actualModule, expectDeclarationProvider)
                is FirCallableSymbol -> computeExpectsForLibraryCallable(firSymbol, actualModule, expectDeclarationProvider)
                else -> emptyList()
            }

            return firSymbols.map { analysisSession.firSymbolBuilder.buildSymbol(it) as KaDeclarationSymbol }
        }

        return firSymbol.expectForActual?.get(ExpectActualMatchingCompatibility.MatchedSuccessfully)
            ?.map { analysisSession.firSymbolBuilder.buildSymbol(it) as KaDeclarationSymbol }.orEmpty()
    }

    private fun KaDeclarationSymbol.getExpectsForActualSpecial(): List<KaDeclarationSymbol>? {
        val actualName = this.name

        when (this) {
            is KaReceiverParameterSymbol -> {
                return getExpectsForActualParent(containingDeclaration(this) as? KaCallableSymbol) { expectParent ->
                    expectParent.receiverParameter
                }
            }
            is KaTypeParameterSymbol -> {
                val actualParent = containingDeclaration(this) ?: return emptyList()
                val actualIndex = actualParent.typeParameters.indexOf(this).takeIf { it >= 0 } ?: return emptyList()
                return getExpectsForActualParent(actualParent) { expectParent ->
                    /** See [org.jetbrains.kotlin.resolve.multiplatform.ExpectActualIncompatibility.TypeParameterNames] */
                    expectParent.typeParameters.getOrNull(actualIndex)?.takeIf { it.name == actualName }
                }
            }
            is KaContextParameterSymbol -> {
                val actualParent = containingDeclaration(this) as? KaCallableSymbol ?: return emptyList()
                val actualIndex = actualParent.contextParameters.indexOf(this).takeIf { it >= 0 } ?: return emptyList()
                return getExpectsForActualParent(actualParent) { expectParent ->
                    /** See [org.jetbrains.kotlin.resolve.multiplatform.ExpectActualIncompatibility.ContextParameterNames] */
                    expectParent.contextParameters.getOrNull(actualIndex)?.takeIf { it.name == actualName }
                }
            }
            is KaValueParameterSymbol -> {
                val actualParent = containingDeclaration(this) as? KaFunctionSymbol ?: return emptyList()
                val actualIndex = actualParent.valueParameters.indexOf(this).takeIf { it >= 0 } ?: return emptyList()
                return getExpectsForActualParent(actualParent) { expectParent: KaFunctionSymbol ->
                    /** See [org.jetbrains.kotlin.resolve.multiplatform.ExpectActualIncompatibility.ParameterNames] */
                    expectParent.valueParameters.getOrNull(actualIndex)?.takeIf { it.name == actualName }
                }
            }
            is KaPropertyAccessorSymbol -> {
                val isGetter = this is KaPropertyGetterSymbol
                return getExpectsForActualParent(containingDeclaration(this) as? KaPropertySymbol) { expectProperty ->
                    if (isGetter) expectProperty.getter else expectProperty.setter
                }
            }
            else -> {
                // The given symbol isn't specially treated
                return null
            }
        }
    }

    private inline fun <reified P : KaSymbol, R : KaSymbol> KaDeclarationSymbol.getExpectsForActualParent(
        actualParent: P?,
        transformer: (P) -> R?,
    ): List<R> {
        return (actualParent as? KaDeclarationSymbol)?.let { getExpectsForActual(it) }
            .orEmpty()
            .filterIsInstance<P>()
            .mapNotNull(transformer)
    }

    private fun computeExpectsForLibraryClass(
        actualSymbol: FirClassLikeSymbol<*>,
        actualModule: KaModule,
        expectDeclarationProvider: KotlinDeclarationProvider,
    ): List<FirClassLikeSymbol<*>> {
        val implementingPlatform = actualModule.targetPlatform

        // Even for a type alias, the 'expect' counterpart will be a class ('expect typealias' is prohibited)
        val declarations = expectDeclarationProvider.getAllClassesByClassId(actualSymbol.classId)

        return buildList {
            for (declaration in declarations) {
                if (!declaration.isExpectDeclaration()) {
                    continue
                }

                val declarationPlatforms = resolutionFacade.getModule(declaration).targetPlatform
                if (declarationPlatforms.intersect(implementingPlatform.componentPlatforms).isEmpty()) {
                    continue
                }

                add(declaration.resolveToFirSymbolOfType<FirClassLikeSymbol<*>>(resolutionFacade))
            }
        }
    }

    private fun computeExpectsForLibraryCallable(
        actualSymbol: FirCallableSymbol<*>,
        actualModule: KaModule,
        expectDeclarationProvider: KotlinDeclarationProvider,
    ): List<FirCallableSymbol<*>> {
        @OptIn(ClassIdBasedLocality::class)
        val callableId = actualSymbol.callableId?.takeUnless { it.isLocal } ?: return emptyList()

        val actualSession = actualSymbol.llFirSession

        val result = mutableListOf<FirCallableSymbol<*>>()

        val expectMatchingContext = FirExpectActualMatchingContextImpl.Factory.create(
            actualSession,
            actualSession.getScopeSession(),
            allowedWritingMemberExpectForActualMapping = false
        )

        fun handle(expectSymbol: FirCallableSymbol<*>, expectClass: FirRegularClassSymbol?, actualClass: FirRegularClassSymbol?) {
            when {
                expectSymbol == actualSymbol -> {
                    /** Found the same symbol (possible as [expectDeclarationProvider] contains the [actualSymbol]. */
                    return
                }

                !expectSymbol.isExpect -> return
                expectSymbol.isSubstitutionOrIntersectionOverride -> return
                expectSymbol.visibility == Visibilities.Private -> return
            }

            val compatibility = AbstractExpectActualMatcher
                .getCallablesMatchingCompatibility(expectSymbol, actualSymbol, expectClass, actualClass, expectMatchingContext)

            if (compatibility == ExpectActualMatchingCompatibility.MatchedSuccessfully) {
                result.add(expectSymbol)
            }
        }

        if (callableId.classId != null) {
            val actualClass = actualSymbol.getContainingClassSymbol()
            if (actualClass !is FirRegularClassSymbol || actualClass.isExpect) {
                return emptyList()
            }

            val expectClasses = computeExpectsForLibraryClass(actualClass, actualModule, expectDeclarationProvider)

            for (expectClass in expectClasses) {
                if (expectClass !is FirRegularClassSymbol) {
                    continue
                }

                fun FirScope.processRelevantDeclarations(processor: (FirCallableSymbol<*>) -> Unit) {
                    when (actualSymbol) {
                        is FirFunctionSymbol -> processFunctionsByName(actualSymbol.name, processor)
                        is FirPropertySymbol -> processPropertiesByName(actualSymbol.name, processor)
                    }
                }

                when {
                    actualSymbol is FirConstructorSymbol -> {
                        val scope = expectClass.declaredMemberScope(actualSession, memberRequiredPhase = null)
                        scope.processDeclaredConstructors { handle(it, expectClass, actualClass) }
                    }
                    actualSymbol.isStatic -> {
                        val scope = expectClass.staticScope(SessionHolderImpl(actualSession, actualSession.getScopeSession()))
                        scope?.processRelevantDeclarations { handle(it, expectClass, actualClass) }
                    }
                    else -> {
                        val scope = expectClass.declaredMemberScope(actualSession, memberRequiredPhase = null)
                        scope.processRelevantDeclarations { handle(it, expectClass, actualClass) }
                    }
                }
            }
        } else {
            val candidates = when (actualSymbol) {
                is FirFunctionSymbol -> expectDeclarationProvider.getTopLevelFunctions(callableId)
                is FirPropertySymbol -> expectDeclarationProvider.getTopLevelProperties(callableId)
                else -> emptyList()
            }

            for (candidate in candidates) {
                if (!candidate.hasExpectModifier() || candidate.hasModifier(KtTokens.PRIVATE_KEYWORD)) {
                    // Quick PSI-level check for inapplicable declarations (required 'expect' and 'EXPECTED_PRIVATE_DECLARATION')
                    continue
                }

                val expectSymbol = candidate.resolveToFirSymbolOfType<FirCallableSymbol<*>>(resolutionFacade)
                handle(expectSymbol, expectClass = null, actualClass = null)
            }
        }

        return result
    }

    override fun sealedClassInheritors(symbol: KaNamedClassSymbol): List<KaNamedClassSymbol> = withValidityAssertion {
        require(symbol.modality == KaSymbolModality.SEALED)
        require(symbol is KaFirNamedClassSymbol)

        val inheritorClassIds = symbol.firSymbol.fir.getSealedClassInheritors(analysisSession.firSession)

        return with(analysisSession) {
            inheritorClassIds.mapNotNull { findClass(it) as? KaNamedClassSymbol }
        }
    }

    override fun hasConflictingSignatureWith(
        symbol: KaFunctionSymbol,
        other: KaFunctionSymbol,
        targetPlatform: TargetPlatform,
    ): Boolean = withValidityAssertion {
        val thisFirSymbol = symbol.firSymbol
        val otherFirSymbol = other.firSymbol

        val thisHasLowPriority = hasLowPriorityAnnotation(thisFirSymbol.resolvedAnnotationsWithClassIds)
        val otherHasLowPriority = hasLowPriorityAnnotation(otherFirSymbol.resolvedAnnotationsWithClassIds)
        if (thisHasLowPriority != otherHasLowPriority) {
            return false
        }

        /**
         * [FirDeclarationOverloadabilityHelper] performs signature comparison only from JVM platform perspective.
         * However, as the API needs to be more generic than that, here we perform manual signature comparison
         * before calling [FirDeclarationOverloadabilityHelper].
         * This is done to handle cases which are considered conflicting on JVM but completely valid on other platforms:
         * - Overloads by type parameters
         * ```kotlin
         * fun <T> foo() // Conflicting on JVM, valid on other platforms
         * fun foo()
         * ```
         * - Overloads by vararg/array parameters
         * ```kotlin
         * fun foo(vararg ints: Int) // Conflicting on JVM, valid on other platforms
         * fun foo(ints: IntArray)
         * ```
         */
        if (!targetPlatform.isJvm()) {
            if (thisFirSymbol.typeParameterSymbols.isEmpty() != otherFirSymbol.typeParameterSymbols.isEmpty()) {
                return false
            }

            val thisVarargParameterPosition = symbol.valueParameters.indexOfFirst { it.isVararg }
            val otherVarargParameterPosition = other.valueParameters.indexOfFirst { it.isVararg }
            if (thisVarargParameterPosition != otherVarargParameterPosition) {
                return false
            }
        }

        val overloadabilityHelper = analysisSession.firSession.declarationOverloadabilityHelper

        return if (analysisSession.firSession.languageVersionSettings.supportsFeature(LanguageFeature.ContextParameters)) {
            overloadabilityHelper.getContextParameterShadowing(thisFirSymbol, otherFirSymbol) == BothWays
        } else {
            overloadabilityHelper.isConflicting(
                thisFirSymbol,
                otherFirSymbol,
            )
        }
    }
}

private fun ImplementationStatus.toKaImplementationState(): KaCallableImplementationState = when (this) {
    ImplementationStatus.NOT_IMPLEMENTED -> KaCallableMissingImplementationStateImpl
    ImplementationStatus.VAR_IMPLEMENTED_BY_VAL -> KaCallableExplicitImplementationStateImpl(isComplete = false)
    ImplementationStatus.AMBIGUOUSLY_INHERITED -> KaCallableInheritedImplementationStateImpl(isAmbiguous = true, isOverridable = true)
    ImplementationStatus.INHERITED_OR_SYNTHESIZED -> KaCallableInheritedImplementationStateImpl(isAmbiguous = false, isOverridable = true)
    ImplementationStatus.ALREADY_IMPLEMENTED -> KaCallableExplicitImplementationStateImpl(isComplete = true)
    ImplementationStatus.CANNOT_BE_IMPLEMENTED -> KaCallableInheritedImplementationStateImpl(isAmbiguous = false, isOverridable = false)
}
