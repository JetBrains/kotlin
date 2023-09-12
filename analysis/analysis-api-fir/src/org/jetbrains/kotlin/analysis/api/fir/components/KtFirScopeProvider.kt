/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.scopes.*
import org.jetbrains.kotlin.analysis.api.fir.symbols.*
import org.jetbrains.kotlin.analysis.api.fir.types.KtFirType
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.scopes.KtCompositeScope
import org.jetbrains.kotlin.analysis.api.impl.base.scopes.KtEmptyScope
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.scopes.KtTypeScope
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithDeclarations
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.ContextCollector
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.delegateFields
import org.jetbrains.kotlin.fir.java.JavaScopeProvider
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticPropertiesScope
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.scopeSessionKey
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseWithCallableMembers
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KtFirScopeProvider(
    override val analysisSession: KtFirAnalysisSession,
    private val builder: KtSymbolByFirBuilder,
    private val firResolveSession: LLFirResolveSession,
) : KtScopeProvider() {

    private fun getScopeSession(): ScopeSession {
        return analysisSession.getScopeSessionFor(analysisSession.useSiteSession)
    }

    private fun KtSymbolWithMembers.getFirForScope(): FirClass = when (this) {
        is KtFirNamedClassOrObjectSymbol -> firSymbol.fir
        is KtFirPsiJavaClassSymbol -> firSymbol.fir
        is KtFirAnonymousObjectSymbol -> firSymbol.fir
        else -> error(
            "`${this::class.qualifiedName}` needs to be specially handled by the scope provider or is an unknown" +
                    " ${KtSymbolWithDeclarations::class.simpleName} implementation."
        )
    }

    override fun getMemberScope(classSymbol: KtSymbolWithMembers): KtScope {
        val firScope = classSymbol.getFirForScope().unsubstitutedScope(
            analysisSession.useSiteSession,
            getScopeSession(),
            withForcedTypeCalculator = false,
            memberRequiredPhase = FirResolvePhase.STATUS,
        )
        return KtFirDelegatingNamesAwareScope(firScope, builder)
    }

    override fun getStaticMemberScope(symbol: KtSymbolWithMembers): KtScope {
        val fir = symbol.getFirForScope()
        val firScope = fir.scopeProvider.getStaticScope(fir, analysisSession.useSiteSession, getScopeSession()) ?: return getEmptyScope()
        return KtFirDelegatingNamesAwareScope(firScope, builder)
    }

    override fun getDeclaredMemberScope(classSymbol: KtSymbolWithMembers): KtScope =
        getDeclaredMemberScope(classSymbol, DeclaredMemberScopeKind.NON_STATIC)

    override fun getStaticDeclaredMemberScope(classSymbol: KtSymbolWithMembers): KtScope =
        getDeclaredMemberScope(classSymbol, DeclaredMemberScopeKind.STATIC)

    override fun getCombinedDeclaredMemberScope(classSymbol: KtSymbolWithMembers): KtScope =
        getDeclaredMemberScope(classSymbol, DeclaredMemberScopeKind.COMBINED)

    private enum class DeclaredMemberScopeKind {
        NON_STATIC,

        STATIC,

        /**
         * A scope containing both non-static and static members. A smart combined scope (as opposed to a naive combination of [KtScope]s
         * with [getCompositeScope]) avoids duplicate inner classes, as they are contained in non-static and static scopes.
         *
         * A proper combined declared member scope kind also makes it easier to cache combined scopes directly (if needed).
         */
        COMBINED,
    }

    private fun getDeclaredMemberScope(classSymbol: KtSymbolWithMembers, kind: DeclaredMemberScopeKind): KtScope {
        val firDeclaration = classSymbol.firSymbol.fir
        val firScope = when (firDeclaration) {
            is FirJavaClass -> getFirJavaDeclaredMemberScope(firDeclaration, kind) ?: return getEmptyScope()
            else -> getFirKotlinDeclaredMemberScope(classSymbol, kind)
        }

        return KtFirDelegatingNamesAwareScope(firScope, builder)
    }

    private fun getFirKotlinDeclaredMemberScope(
        classSymbol: KtSymbolWithMembers,
        kind: DeclaredMemberScopeKind,
    ): FirContainingNamesAwareScope {
        val combinedScope = getCombinedFirKotlinDeclaredMemberScope(classSymbol)
        return when (kind) {
            DeclaredMemberScopeKind.NON_STATIC -> FirNonStaticMembersScope(combinedScope)
            DeclaredMemberScopeKind.STATIC -> FirStaticScope(combinedScope)
            DeclaredMemberScopeKind.COMBINED -> combinedScope
        }
    }

    /**
     * Returns a declared member scope which contains both static and non-static callables, as well as all classifiers. Java classes need to
     * be handled specially, because [declaredMemberScope] doesn't handle Java enhancement properly.
     */
    private fun getCombinedFirKotlinDeclaredMemberScope(symbolWithMembers: KtSymbolWithMembers): FirContainingNamesAwareScope {
        val useSiteSession = analysisSession.useSiteSession
        return when (symbolWithMembers) {
            is KtFirScriptSymbol -> FirScriptDeclarationsScope(useSiteSession, symbolWithMembers.firSymbol.fir)
            else -> useSiteSession.declaredMemberScope(symbolWithMembers.getFirForScope(), memberRequiredPhase = null)
        }
    }

    private fun getFirJavaDeclaredMemberScope(
        firJavaClass: FirJavaClass,
        kind: DeclaredMemberScopeKind,
    ): FirContainingNamesAwareScope? {
        val useSiteSession = analysisSession.useSiteSession
        val scopeSession = getScopeSession()

        fun getBaseUseSiteScope() = JavaScopeProvider.getUseSiteMemberScope(
            firJavaClass,
            useSiteSession,
            scopeSession,
            memberRequiredPhase = FirResolvePhase.TYPES,
        )

        fun getStaticScope() = JavaScopeProvider.getStaticScope(firJavaClass, useSiteSession, scopeSession)

        val firScope = when (kind) {
            // `FirExcludingNonInnerClassesScope` is a workaround for non-static member scopes containing static classes (see KT-61900).
            DeclaredMemberScopeKind.NON_STATIC -> FirExcludingNonInnerClassesScope(getBaseUseSiteScope())

            DeclaredMemberScopeKind.STATIC -> getStaticScope() ?: return null

            // Java enhancement scopes as provided by `JavaScopeProvider` are either use-site or static scopes, so we need to compose them
            // to get the combined scope. A base declared member scope with Java enhancement doesn't exist, unfortunately.
            DeclaredMemberScopeKind.COMBINED -> {
                // The static scope contains inner classes, so we need to exclude them from the non-static scope to avoid duplicates.
                val nonStaticScope = FirNoClassifiersScope(getBaseUseSiteScope())
                getStaticScope()
                    ?.let { staticScope -> FirNameAwareCompositeScope(listOf(nonStaticScope, staticScope)) }
                    ?: nonStaticScope
            }
        }

        val cacheKey = when (kind) {
            DeclaredMemberScopeKind.NON_STATIC -> JAVA_ENHANCEMENT_FOR_DECLARED_MEMBERS
            DeclaredMemberScopeKind.STATIC -> JAVA_ENHANCEMENT_FOR_STATIC_DECLARED_MEMBERS
            DeclaredMemberScopeKind.COMBINED -> JAVA_ENHANCEMENT_FOR_ALL_DECLARED_MEMBERS
        }

        return scopeSession.getOrBuild(firJavaClass.symbol, cacheKey) {
            FirJavaDeclaredMembersOnlyScope(firScope, firJavaClass)
        }
    }

    override fun getDelegatedMemberScope(classSymbol: KtSymbolWithMembers): KtScope {
        val declaredScope = (getDeclaredMemberScope(classSymbol) as? KtFirDelegatingNamesAwareScope)?.firScope ?: return getEmptyScope()

        val fir = classSymbol.getFirForScope()
        val delegateFields = fir.delegateFields

        if (delegateFields.isEmpty()) {
            return getEmptyScope()
        }

        fir.lazyResolveToPhaseWithCallableMembers(FirResolvePhase.STATUS)

        val firScope = FirDelegatedMemberScope(
            analysisSession.useSiteSession,
            getScopeSession(),
            fir,
            declaredScope,
            delegateFields
        )
        return KtFirDelegatedMemberScope(firScope, builder)
    }

    override fun getFileScope(fileSymbol: KtFileSymbol): KtScope {
        check(fileSymbol is KtFirFileSymbol) { "KtFirScopeProvider can only work with KtFirFileSymbol, but ${fileSymbol::class} was provided" }
        return KtFirFileScope(fileSymbol, builder)
    }

    override fun getEmptyScope(): KtScope {
        return KtEmptyScope(token)
    }

    override fun getPackageScope(packageSymbol: KtPackageSymbol): KtScope {
        return createPackageScope(packageSymbol.fqName)
    }

    override fun getCompositeScope(subScopes: List<KtScope>): KtScope {
        return KtCompositeScope.create(subScopes, token)
    }

    override fun getTypeScope(type: KtType): KtTypeScope? {
        check(type is KtFirType) { "KtFirScopeProvider can only work with KtFirType, but ${type::class} was provided" }
        return getFirTypeScope(type)
            ?.withSyntheticPropertiesScopeOrSelf(type.coneType)
            ?.let { convertToKtTypeScope(it) }
    }

    override fun getSyntheticJavaPropertiesScope(type: KtType): KtTypeScope? {
        check(type is KtFirType) { "KtFirScopeProvider can only work with KtFirType, but ${type::class} was provided" }
        val typeScope = getFirTypeScope(type) ?: return null
        return getFirSyntheticPropertiesScope(type.coneType, typeScope)?.let { convertToKtTypeScope(it) }
    }

    override fun getImportingScopeContext(file: KtFile): KtScopeContext {
        val firFile = file.getOrBuildFirFile(firResolveSession)
        val firFileSession = firFile.moduleData.session
        val firImportingScopes = createImportingScopes(
            firFile,
            firFileSession,
            analysisSession.getScopeSessionFor(firFileSession),
            useCaching = true,
        )

        val ktScopesWithKinds = createScopesWithKind(firImportingScopes.withIndex())
        return KtScopeContext(ktScopesWithKinds, _implicitReceivers = emptyList(), token)
    }

    override fun getScopeContextForPosition(
        originalFile: KtFile,
        positionInFakeFile: KtElement
    ): KtScopeContext {
        val fakeFile = positionInFakeFile.containingKtFile

        // If the position is in KDoc, we want to pass the owning declaration to the ContextCollector.
        // That way, the resulting scope will contain all the nested declarations which can be references by KDoc.
        val parentKDoc = positionInFakeFile.parentOfType<KDoc>()
        val correctedPosition = parentKDoc?.owner ?: positionInFakeFile

        val context = ContextCollector.process(
            fakeFile.getOrBuildFirFile(firResolveSession),
            SessionHolderImpl(analysisSession.useSiteSession, getScopeSession()),
            correctedPosition,
        )

        val towerDataContext =
            context?.towerDataContext
                ?: errorWithAttachment("Cannot find context for ${positionInFakeFile::class}") {
                    withPsiEntry("positionInFakeFile", positionInFakeFile)
                }
        val towerDataElementsIndexed = towerDataContext.towerDataElements.asReversed().withIndex()

        val implicitReceivers = towerDataElementsIndexed.flatMap { (index, towerDataElement) ->
            val receivers = listOfNotNull(towerDataElement.implicitReceiver) + towerDataElement.contextReceiverGroup.orEmpty()

            receivers.map { receiver ->
                KtImplicitReceiver(
                    token,
                    builder.typeBuilder.buildKtType(receiver.type),
                    builder.buildSymbol(receiver.boundSymbol.fir),
                    index
                )
            }
        }

        val firScopes = towerDataElementsIndexed.flatMap { (index, towerDataElement) ->
            val availableScopes = towerDataElement
                .getAvailableScopes { coneType -> withSyntheticPropertiesScopeOrSelf(coneType) }
                .flatMap { flattenFirScope(it) }
            availableScopes.map { IndexedValue(index, it) }
        }
        val ktScopesWithKinds = createScopesWithKind(firScopes)

        return KtScopeContext(ktScopesWithKinds, implicitReceivers, token)
    }

    private fun createScopesWithKind(firScopes: Iterable<IndexedValue<FirScope>>): List<KtScopeWithKind> {
        return firScopes.map { (index, firScope) ->
            KtScopeWithKind(convertToKtScope(firScope), getScopeKind(firScope, index), token)
        }
    }

    private fun flattenFirScope(firScope: FirScope): List<FirScope> = when (firScope) {
        is FirCompositeScope -> firScope.scopes.flatMap { flattenFirScope(it) }
        is FirNameAwareCompositeScope -> firScope.scopes.flatMap { flattenFirScope(it) }
        else -> listOf(firScope)
    }

    private fun convertToKtScope(firScope: FirScope): KtScope {
        return when (firScope) {
            is FirAbstractSimpleImportingScope -> KtFirNonStarImportingScope(firScope, builder)
            is FirAbstractStarImportingScope -> KtFirStarImportingScope(firScope, analysisSession)
            is FirDefaultStarImportingScope -> KtFirDefaultStarImportingScope(firScope, analysisSession)
            is FirPackageMemberScope -> createPackageScope(firScope.fqName)
            is FirContainingNamesAwareScope -> KtFirDelegatingNamesAwareScope(firScope, builder)
            else -> TODO(firScope::class.toString())
        }
    }

    private fun getScopeKind(firScope: FirScope, indexInTower: Int): KtScopeKind = when (firScope) {
        is FirNameAwareOnlyCallablesScope -> getScopeKind(firScope.delegate, indexInTower)

        is FirLocalScope -> KtScopeKind.LocalScope(indexInTower)
        is FirTypeScope -> KtScopeKind.TypeScope(indexInTower)
        is FirTypeParameterScope -> KtScopeKind.TypeParameterScope(indexInTower)
        is FirPackageMemberScope -> KtScopeKind.PackageMemberScope(indexInTower)

        is FirNestedClassifierScope -> KtScopeKind.StaticMemberScope(indexInTower)
        is FirNestedClassifierScopeWithSubstitution -> KtScopeKind.StaticMemberScope(indexInTower)
        is FirLazyNestedClassifierScope -> KtScopeKind.StaticMemberScope(indexInTower)
        is FirStaticScope -> KtScopeKind.StaticMemberScope(indexInTower)

        is FirExplicitSimpleImportingScope -> KtScopeKind.ExplicitSimpleImportingScope(indexInTower)
        is FirExplicitStarImportingScope -> KtScopeKind.ExplicitStarImportingScope(indexInTower)
        is FirDefaultSimpleImportingScope -> KtScopeKind.DefaultSimpleImportingScope(indexInTower)
        is FirDefaultStarImportingScope -> KtScopeKind.DefaultStarImportingScope(indexInTower)

        is FirScriptDeclarationsScope -> KtScopeKind.ScriptMemberScope(indexInTower)

        else -> unexpectedElementError("scope", firScope)
    }

    private fun createPackageScope(fqName: FqName): KtFirPackageScope {
        return KtFirPackageScope(fqName, analysisSession)
    }

    private fun convertToKtTypeScope(firScope: FirScope): KtTypeScope {
        return when (firScope) {
            is FirContainingNamesAwareScope -> KtFirDelegatingTypeScope(firScope, builder)
            else -> TODO(firScope::class.toString())
        }
    }

    private fun getFirTypeScope(type: KtFirType): FirTypeScope? = type.coneType.scope(
        firResolveSession.useSiteFirSession,
        getScopeSession(),
        CallableCopyTypeCalculator.Forced,
        requiredMembersPhase = FirResolvePhase.STATUS,
    )

    private fun getFirSyntheticPropertiesScope(coneType: ConeKotlinType, typeScope: FirTypeScope): FirSyntheticPropertiesScope? =
        FirSyntheticPropertiesScope.createIfSyntheticNamesProviderIsDefined(
            firResolveSession.useSiteFirSession,
            coneType,
            typeScope
        )

    private fun FirTypeScope.withSyntheticPropertiesScopeOrSelf(coneType: ConeKotlinType): FirTypeScope {
        val syntheticPropertiesScope = getFirSyntheticPropertiesScope(coneType, this) ?: return this
        return FirTypeScopeWithSyntheticProperties(typeScope = this, syntheticPropertiesScope)
    }
}

private class FirTypeScopeWithSyntheticProperties(
    val typeScope: FirTypeScope,
    val syntheticPropertiesScope: FirSyntheticPropertiesScope,
) : FirDelegatingTypeScope(typeScope) {
    override fun getCallableNames(): Set<Name> = typeScope.getCallableNames() + syntheticPropertiesScope.getCallableNames()
    override fun mayContainName(name: Name): Boolean = typeScope.mayContainName(name) || syntheticPropertiesScope.mayContainName(name)

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        typeScope.processPropertiesByName(name, processor)
        syntheticPropertiesScope.processPropertiesByName(name, processor)
    }
}

private val JAVA_ENHANCEMENT_FOR_DECLARED_MEMBERS = scopeSessionKey<FirRegularClassSymbol, FirContainingNamesAwareScope>()

private val JAVA_ENHANCEMENT_FOR_STATIC_DECLARED_MEMBERS = scopeSessionKey<FirRegularClassSymbol, FirContainingNamesAwareScope>()

private val JAVA_ENHANCEMENT_FOR_ALL_DECLARED_MEMBERS = scopeSessionKey<FirRegularClassSymbol, FirContainingNamesAwareScope>()
