/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.scopes.*
import org.jetbrains.kotlin.analysis.api.fir.symbols.*
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirType
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.getAvailableScopesForPosition
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseScopeImplicitReceiverValue
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseScopeContext
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseScopeImplicitArgumentValue
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.scopes.KaBaseCompositeScope
import org.jetbrains.kotlin.analysis.api.impl.base.scopes.KaBaseCompositeTypeScope
import org.jetbrains.kotlin.analysis.api.impl.base.scopes.KaBaseEmptyScope
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.scopes.KaTypeScope
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.ContextCollector
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.delegateFields
import org.jetbrains.kotlin.fir.java.JavaScopeProvider
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticPropertiesScope
import org.jetbrains.kotlin.fir.resolve.calls.referencedMemberSymbol
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.scopeSessionKey
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseWithCallableMembers
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KaFirScopeProvider(
    override val analysisSessionProvider: () -> KaFirSession
) : KaBaseSessionComponent<KaFirSession>(), KaScopeProvider, KaFirSessionComponent {
    private fun getScopeSession(): ScopeSession {
        return analysisSession.getScopeSessionFor(analysisSession.firSession)
    }

    private fun KaDeclarationContainerSymbol.getFirForScope(): FirClass = when (this) {
        is KaFirNamedClassSymbol -> firSymbol.fir
        is KaFirPsiJavaClassSymbol -> firSymbol.fir
        is KaFirAnonymousObjectSymbol -> firSymbol.fir
        else -> error(
            "`${this::class.qualifiedName}` needs to be specially handled by the scope provider or is an unknown" +
                    " ${KaDeclarationContainerSymbol::class.simpleName} implementation."
        )
    }

    override val KaDeclarationContainerSymbol.memberScope: KaScope
        get() = withValidityAssertion {
            val firScope = getFirForScope().unsubstitutedScope(
                analysisSession.firSession,
                getScopeSession(),
                withForcedTypeCalculator = false,
                memberRequiredPhase = FirResolvePhase.STATUS,
            )
            return KaFirDelegatingNamesAwareScope(firScope, analysisSession.firSymbolBuilder)
        }

    override val KaDeclarationContainerSymbol.staticMemberScope: KaScope
        get() = withValidityAssertion {
            val fir = getFirForScope()
            val firScope = fir.scopeProvider.getStaticScope(fir, analysisSession.firSession, getScopeSession())
                ?: return createEmptyScope()

            return KaFirDelegatingNamesAwareScope(firScope, analysisSession.firSymbolBuilder)
        }

    override val KaDeclarationContainerSymbol.declaredMemberScope: KaScope
        get() = withValidityAssertion {
            getDeclaredMemberScope(this, DeclaredMemberScopeKind.NON_STATIC)
        }

    override val KaDeclarationContainerSymbol.staticDeclaredMemberScope: KaScope
        get() = withValidityAssertion {
            getDeclaredMemberScope(this, DeclaredMemberScopeKind.STATIC)
        }

    override val KaDeclarationContainerSymbol.combinedDeclaredMemberScope: KaScope
        get() = withValidityAssertion {
            getDeclaredMemberScope(this, DeclaredMemberScopeKind.COMBINED)
        }

    private enum class DeclaredMemberScopeKind {
        NON_STATIC,

        STATIC,

        /**
         * A scope containing both non-static and static members. A smart combined scope (as opposed to a naive combination of [KaScope]s
         * with [getCompositeScope]) avoids duplicate inner classes, as they are contained in non-static and static scopes.
         *
         * A proper combined declared member scope kind also makes it easier to cache combined scopes directly (if needed).
         */
        COMBINED,
    }

    private fun getDeclaredMemberScope(symbol: KaDeclarationContainerSymbol, kind: DeclaredMemberScopeKind): KaScope {
        val firDeclaration = symbol.firSymbol.fir
        val firScope = when (firDeclaration) {
            is FirJavaClass -> getFirJavaDeclaredMemberScope(firDeclaration, kind) ?: return createEmptyScope()
            else -> getFirKotlinDeclaredMemberScope(symbol, kind)
        }

        return KaFirDelegatingNamesAwareScope(firScope, analysisSession.firSymbolBuilder)
    }

    private fun getFirKotlinDeclaredMemberScope(
        symbol: KaDeclarationContainerSymbol,
        kind: DeclaredMemberScopeKind,
    ): FirContainingNamesAwareScope {
        val combinedScope = getCombinedFirKotlinDeclaredMemberScope(symbol)
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
    private fun getCombinedFirKotlinDeclaredMemberScope(symbol: KaDeclarationContainerSymbol): FirContainingNamesAwareScope {
        val useSiteSession = analysisSession.firSession
        return when (symbol) {
            is KaFirScriptSymbol -> FirScriptDeclarationsScope(useSiteSession, symbol.firSymbol.fir)
            else -> useSiteSession.declaredMemberScope(symbol.getFirForScope(), memberRequiredPhase = null)
        }
    }

    private fun getFirJavaDeclaredMemberScope(
        firJavaClass: FirJavaClass,
        kind: DeclaredMemberScopeKind,
    ): FirContainingNamesAwareScope? {
        val useSiteSession = analysisSession.firSession
        val scopeSession = getScopeSession()

        // Create use site scope to handle signature enhancement properly
        fun getBaseUseSiteScope() = JavaScopeProvider.getUseSiteMemberScope(
            firJavaClass,
            useSiteSession,
            scopeSession,
            memberRequiredPhase = FirResolvePhase.STATUS,
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

    override val KaDeclarationContainerSymbol.delegatedMemberScope: KaScope
        get() = withValidityAssertion {
            val declaredScope = (declaredMemberScope as? KaFirDelegatingNamesAwareScope)?.firScope ?: return createEmptyScope()

            val fir = getFirForScope()

            @OptIn(DirectDeclarationsAccess::class)
            val delegateFields = fir.delegateFields

            if (delegateFields.isEmpty()) {
                return createEmptyScope()
            }

            fir.lazyResolveToPhaseWithCallableMembers(FirResolvePhase.STATUS)

            val firScope = FirDelegatedMemberScope(
                analysisSession.firSession,
                getScopeSession(),
                fir,
                declaredScope,
                delegateFields
            )
            return KaFirDelegatedMemberScope(firScope, analysisSession.firSymbolBuilder)
        }

    override val KaFileSymbol.fileScope: KaScope
        get() = withValidityAssertion {
            check(this is KaFirFileSymbol) { "KtFirScopeProvider can only work with KtFirFileSymbol, but ${this::class} was provided" }
            return KaFirFileScope(this, builder)
        }

    private fun createEmptyScope(): KaScope {
        return KaBaseEmptyScope(token)
    }

    override val KaPackageSymbol.packageScope: KaScope
        get() = withValidityAssertion {
            createPackageScope(fqName)
        }

    override fun List<KaScope>.asCompositeScope(): KaScope = withValidityAssertion {
        return KaBaseCompositeScope.create(this, token)
    }

    override val KaType.scope: KaTypeScope?
        get() = withValidityAssertion {
            check(this is KaFirType) { "KtFirScopeProvider can only work with KtFirType, but ${this::class} was provided" }
            return getFirTypeScope(this)
                ?.withSyntheticPropertiesScopeOrSelf(coneType)
                ?.let { convertToKtTypeScope(it) }
        }

    override val KaTypeScope.declarationScope: KaScope
        get() = withValidityAssertion {
            return when (this) {
                is KaFirDelegatingTypeScope -> KaFirDelegatingNamesAwareScope(firScope, analysisSession.firSymbolBuilder)
                is KaBaseCompositeTypeScope -> KaBaseCompositeScope.create(subScopes.map { it.declarationScope }, token)
                else -> unexpectedElementError<KaTypeScope>(this)
            }
        }

    override val KaType.syntheticJavaPropertiesScope: KaTypeScope?
        get() = withValidityAssertion {
            check(this is KaFirType) { "KtFirScopeProvider can only work with KtFirType, but ${this::class} was provided" }
            val typeScope = getFirTypeScope(this) ?: return null
            return getFirSyntheticPropertiesScope(coneType, typeScope)?.let { convertToKtTypeScope(it) }
        }

    override val KtFile.importingScopeContext: KaScopeContext
        get() = withValidityAssertion {
            val firFile = getOrBuildFirFile(firResolveSession)
            val firFileSession = firFile.moduleData.session
            val firImportingScopes = createImportingScopes(
                firFile,
                firFileSession,
                analysisSession.getScopeSessionFor(firFileSession),
                useCaching = true,
            )

            val firImportingScopesIndexed = firImportingScopes.asReversed().withIndex()

            val ktScopesWithKinds = createScopesWithKind(firImportingScopesIndexed)
            return KaBaseScopeContext(ktScopesWithKinds, implicitValues = emptyList(), token)
        }

    override fun KtFile.scopeContext(position: KtElement): KaScopeContext = withValidityAssertion {
        val fakeFile = position.containingKtFile

        // If the position is in KDoc, we want to pass the owning declaration to the ContextCollector.
        // That way, the resulting scope will contain all the nested declarations which can be references by KDoc.
        val parentKDoc = position.parentOfType<KDoc>()
        val correctedPosition = parentKDoc?.owner ?: position

        val context = ContextCollector.process(
            fakeFile.getOrBuildFirFile(firResolveSession),
            SessionHolderImpl(analysisSession.firSession, getScopeSession()),
            correctedPosition,
        )

        val towerDataContext =
            context?.towerDataContext
                ?: errorWithAttachment("Cannot find context for ${position::class}") {
                    withPsiEntry("position", position)
                }
        val towerDataElementsIndexed = towerDataContext.towerDataElements.asReversed().withIndex()

        val firSymbolBuilder = analysisSession.firSymbolBuilder

        val implicitValues = towerDataElementsIndexed.flatMap { (index, towerDataElement) ->
            buildList {
                val receivers = listOfNotNull(towerDataElement.implicitReceiver) + towerDataElement.contextReceiverGroup.orEmpty()
                for (receiver in receivers) {
                    val receiverValue = KaBaseScopeImplicitReceiverValue(
                        backingType = firSymbolBuilder.typeBuilder.buildKtType(receiver.type),
                        ownerSymbol = firSymbolBuilder.buildSymbol(receiver.referencedMemberSymbol),
                        scopeIndexInTower = index,
                    )

                    add(receiverValue)
                }

                val arguments = towerDataElement.contextParameterGroup.orEmpty()
                for (argument in arguments) {
                    val argumentValue = KaBaseScopeImplicitArgumentValue(
                        backingType = firSymbolBuilder.typeBuilder.buildKtType(argument.type),
                        symbol = firSymbolBuilder.variableBuilder.buildContextParameterSymbol(argument.boundSymbol),
                        scopeIndexInTower = index,
                    )

                    add(argumentValue)
                }
            }
        }

        val firScopes = towerDataElementsIndexed.flatMap { (index, towerDataElement) ->
            val availableScopes = towerDataElement
                .getAvailableScopesForPosition(position) { coneType -> withSyntheticPropertiesScopeOrSelf(coneType) }
                .flatMap { flattenFirScope(it) }
            availableScopes.map { IndexedValue(index, it) }
        }
        val ktScopesWithKinds = createScopesWithKind(firScopes)

        return KaBaseScopeContext(ktScopesWithKinds, implicitValues, token)
    }

    private fun createScopesWithKind(firScopes: Iterable<IndexedValue<FirScope>>): List<KaScopeWithKind> {
        return firScopes.map { (index, firScope) ->
            KaScopeWithKindImpl(convertToKtScope(firScope), getScopeKind(firScope, index))
        }
    }

    private fun flattenFirScope(firScope: FirScope): List<FirScope> = when (firScope) {
        is FirCompositeScope -> firScope.scopes.flatMap { flattenFirScope(it) }
        is FirNameAwareCompositeScope -> firScope.scopes.flatMap { flattenFirScope(it) }
        else -> listOf(firScope)
    }

    private fun convertToKtScope(firScope: FirScope): KaScope {
        return when (firScope) {
            is FirAbstractSimpleImportingScope -> KaFirNonStarImportingScope(firScope, analysisSession.firSymbolBuilder)
            is FirAbstractStarImportingScope -> KaFirStarImportingScope(firScope, analysisSession)
            is FirDefaultStarImportingScope -> KaFirDefaultStarImportingScope(firScope, analysisSession)
            is FirPackageMemberScope -> createPackageScope(firScope.fqName)
            is FirContainingNamesAwareScope -> KaFirDelegatingNamesAwareScope(firScope, analysisSession.firSymbolBuilder)
            else -> TODO(firScope::class.toString())
        }
    }

    private fun getScopeKind(firScope: FirScope, indexInTower: Int): KaScopeKind = when (firScope) {
        is FirNameAwareOnlyCallablesScope -> getScopeKind(firScope.delegate, indexInTower)
        is FirNoClassifiersScope -> getScopeKind(firScope.delegateScope, indexInTower)

        is FirLocalScope -> KaScopeKinds.LocalScope(indexInTower)
        is FirTypeScope -> KaScopeKinds.TypeScope(indexInTower)
        is FirTypeParameterScope -> KaScopeKinds.TypeParameterScope(indexInTower)
        is FirPackageMemberScope -> KaScopeKinds.PackageMemberScope(indexInTower)

        is FirNestedClassifierScope -> KaScopeKinds.StaticMemberScope(indexInTower)
        is FirNestedClassifierScopeWithSubstitution -> KaScopeKinds.StaticMemberScope(indexInTower)
        is FirLazyNestedClassifierScope -> KaScopeKinds.StaticMemberScope(indexInTower)
        is FirStaticScope -> KaScopeKinds.StaticMemberScope(indexInTower)

        is FirExplicitSimpleImportingScope -> KaScopeKinds.ExplicitSimpleImportingScope(indexInTower)
        is FirExplicitStarImportingScope -> KaScopeKinds.ExplicitStarImportingScope(indexInTower)
        is FirDefaultSimpleImportingScope -> KaScopeKinds.DefaultSimpleImportingScope(indexInTower)
        is FirDefaultStarImportingScope -> KaScopeKinds.DefaultStarImportingScope(indexInTower)

        is FirScriptDeclarationsScope -> KaScopeKinds.ScriptMemberScope(indexInTower)

        else -> unexpectedElementError("scope", firScope)
    }

    private fun createPackageScope(fqName: FqName): KaFirPackageScope {
        return KaFirPackageScope(fqName, analysisSession)
    }

    private fun convertToKtTypeScope(firScope: FirScope): KaTypeScope {
        return when (firScope) {
            is FirContainingNamesAwareScope -> KaFirDelegatingTypeScope(firScope, analysisSession.firSymbolBuilder)
            else -> TODO(firScope::class.toString())
        }
    }

    private fun getFirTypeScope(type: KaFirType): FirTypeScope? = type.coneType.scope(
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

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): FirTypeScopeWithSyntheticProperties? {
        val newTypeScope = typeScope.withReplacedSessionOrNull(newSession, newScopeSession)
        val newSyntheticPropertiesScope = syntheticPropertiesScope.withReplacedSessionOrNull(newSession, newScopeSession)
        if (newTypeScope == null && newSyntheticPropertiesScope == null) return null
        return FirTypeScopeWithSyntheticProperties(
            newTypeScope ?: typeScope,
            newSyntheticPropertiesScope ?: syntheticPropertiesScope,
        )
    }
}

private val JAVA_ENHANCEMENT_FOR_DECLARED_MEMBERS = scopeSessionKey<FirRegularClassSymbol, FirContainingNamesAwareScope>()

private val JAVA_ENHANCEMENT_FOR_STATIC_DECLARED_MEMBERS = scopeSessionKey<FirRegularClassSymbol, FirContainingNamesAwareScope>()

private val JAVA_ENHANCEMENT_FOR_ALL_DECLARED_MEMBERS = scopeSessionKey<FirRegularClassSymbol, FirContainingNamesAwareScope>()
