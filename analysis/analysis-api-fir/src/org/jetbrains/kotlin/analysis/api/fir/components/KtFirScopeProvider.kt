/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticPropertiesScope
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LowLevelFirApiFacadeForResolveOnAir.getTowerContextProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getElementTextInContext
import org.jetbrains.kotlin.analysis.api.ValidityTokenOwner
import org.jetbrains.kotlin.analysis.api.components.KtImplicitReceiver
import org.jetbrains.kotlin.analysis.api.components.KtScopeContext
import org.jetbrains.kotlin.analysis.api.components.KtScopeProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.scopes.*
import org.jetbrains.kotlin.analysis.api.fir.symbols.*
import org.jetbrains.kotlin.analysis.api.fir.types.KtFirType
import org.jetbrains.kotlin.analysis.api.fir.utils.weakRef
import org.jetbrains.kotlin.analysis.api.scopes.*
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithDeclarations
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import java.util.*

internal class KtFirScopeProvider(
    analysisSession: KtFirAnalysisSession,
    builder: KtSymbolByFirBuilder,
    private val project: Project,
    firResolveState: FirModuleResolveState,
    override val token: ValidityToken,
) : KtScopeProvider(), ValidityTokenOwner {
    override val analysisSession: KtFirAnalysisSession by weakRef(analysisSession)
    private val builder by weakRef(builder)
    private val firResolveState by weakRef(firResolveState)

    private val memberScopeCache = IdentityHashMap<KtSymbolWithMembers, KtMemberScope>()
    private val declaredMemberScopeCache = IdentityHashMap<KtSymbolWithMembers, KtDeclaredMemberScope>()
    private val fileScopeCache = IdentityHashMap<KtFileSymbol, KtDeclarationScope<KtSymbolWithDeclarations>>()
    private val packageMemberScopeCache = IdentityHashMap<KtPackageSymbol, KtPackageScope>()

    private inline fun <T> KtSymbolWithMembers.withFirForScope(crossinline body: (FirClass) -> T): T? = when (this) {
        is KtFirNamedClassOrObjectSymbol -> firRef.withFir(FirResolvePhase.TYPES, body)
        is KtFirAnonymousObjectSymbol -> firRef.withFir(FirResolvePhase.TYPES, body)
        is KtFirEnumEntrySymbol -> firRef.withFir(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) {
            val initializer = it.initializer
            check(initializer is FirAnonymousObjectExpression) { "Unexpected enum entry initializer: ${initializer?.javaClass}" }
            body(initializer.anonymousObject)
        }
        else -> error { "Unknown KtSymbolWithDeclarations implementation ${this::class.qualifiedName}" }
    }

    override fun getMemberScope(classSymbol: KtSymbolWithMembers): KtMemberScope = withValidityAssertion {
        memberScopeCache.getOrPut(classSymbol) {

            val firScope = classSymbol.withFirForScope { fir ->
                val firSession = analysisSession.rootModuleSession
                fir.unsubstitutedScope(
                    firSession,
                    ScopeSession(),
                    withForcedTypeCalculator = false
                )
            } ?: return@getOrPut KtFirEmptyMemberScope(classSymbol)

            KtFirMemberScope(classSymbol, firScope, token, builder)
        }
    }

    override fun getStaticMemberScope(symbol: KtSymbolWithMembers): KtScope {
        val firScope = symbol.withFirForScope { fir ->
            fir.scopeProvider.getStaticScope(fir, analysisSession.rootModuleSession, ScopeSession())
        } ?: return KtFirEmptyMemberScope(symbol)
        check(firScope is FirContainingNamesAwareScope)
        return KtFirDelegatingScopeImpl(firScope, builder, token)
    }

    override fun getDeclaredMemberScope(classSymbol: KtSymbolWithMembers): KtDeclaredMemberScope = withValidityAssertion {
        declaredMemberScopeCache.getOrPut(classSymbol) {
            val firScope = classSymbol.withFirForScope {
                analysisSession.rootModuleSession.declaredMemberScope(it)
            } ?: return@getOrPut KtFirEmptyMemberScope(classSymbol)

            KtFirDeclaredMemberScope(classSymbol, firScope, token, builder)
        }
    }

    override fun getFileScope(fileSymbol: KtFileSymbol): KtDeclarationScope<KtSymbolWithDeclarations> = withValidityAssertion {
        fileScopeCache.getOrPut(fileSymbol) {
            check(fileSymbol is KtFirFileSymbol) { "KtFirScopeProvider can only work with KtFirFileSymbol, but ${fileSymbol::class} was provided" }
            KtFirFileScope(fileSymbol, token, builder)
        }
    }

    override fun getPackageScope(packageSymbol: KtPackageSymbol): KtPackageScope = withValidityAssertion {
        packageMemberScopeCache.getOrPut(packageSymbol) {
            KtFirPackageScope(
                packageSymbol.fqName,
                project,
                builder,
                token,
                analysisSession.searchScope,
                analysisSession.targetPlatform
            )
        }
    }


    override fun getCompositeScope(subScopes: List<KtScope>): KtCompositeScope = withValidityAssertion {
        KtFirCompositeScope(subScopes, token)
    }

    override fun getTypeScope(type: KtType): KtScope? {
        check(type is KtFirType) { "KtFirScopeProvider can only work with KtFirType, but ${type::class} was provided" }
        val firSession = firResolveState.rootModuleSession
        val firTypeScope = type.coneType.scope(
            firSession,
            ScopeSession(),
            FakeOverrideTypeCalculator.Forced
        ) ?: return null
        return getCompositeScope(
            listOf(
                convertToKtScope(firTypeScope),
                firTypeScope.getSyntheticPropertiesScope(firSession)
            )
        )
    }

    private fun FirTypeScope.getSyntheticPropertiesScope(firSession: FirSession): KtScope =
        convertToKtScope(FirSyntheticPropertiesScope(firSession, this))

    override fun getScopeContextForPosition(
        originalFile: KtFile,
        positionInFakeFile: KtElement
    ): KtScopeContext = withValidityAssertion {

        val towerDataContext =
            analysisSession.firResolveState.getTowerContextProvider().getClosestAvailableParentContext(positionInFakeFile)
                ?: error("Cannot find enclosing declaration for ${positionInFakeFile.getElementTextInContext()}")

        val implicitReceivers = towerDataContext.nonLocalTowerDataElements.mapNotNull { it.implicitReceiver }.distinct()
        val implicitKtReceivers = implicitReceivers.map { receiver ->
            KtImplicitReceiver(
                token,
                builder.typeBuilder.buildKtType(receiver.type),
                builder.buildSymbol(receiver.boundSymbol.fir),
            )
        }

        val implicitReceiverScopes = implicitReceivers.mapNotNull { it.implicitScope }
        val nonLocalScopes = towerDataContext.nonLocalTowerDataElements.mapNotNull { it.scope }.distinct()
        val firLocalScopes = towerDataContext.localScopes

        @OptIn(ExperimentalStdlibApi::class)
        val allKtScopes = buildList<KtScope> {
            implicitReceiverScopes.mapTo(this, ::convertToKtScope)
            nonLocalScopes.mapTo(this, ::convertToKtScope)
            firLocalScopes.mapTo(this, ::convertToKtScope)
        }

        KtScopeContext(
            getCompositeScope(allKtScopes.asReversed()),
            implicitKtReceivers.asReversed()
        )
    }

    private fun convertToKtScope(firScope: FirScope): KtScope {
        return when (firScope) {
            is FirAbstractSimpleImportingScope -> KtFirNonStarImportingScope(firScope, builder, token)
            is FirAbstractStarImportingScope -> KtFirStarImportingScope(firScope, builder, project, token)
            is FirPackageMemberScope -> KtFirPackageScope(
                firScope.fqName,
                project,
                builder,
                token,
                analysisSession.searchScope,
                analysisSession.targetPlatform
            )
            is FirContainingNamesAwareScope -> KtFirDelegatingScopeImpl(firScope, builder, token)
            is FirMemberTypeParameterScope -> KtFirDelegatingScopeImpl(firScope, builder, token)
            else -> TODO(firScope::class.toString())
        }
    }
}

private class KtFirDelegatingScopeImpl<S : FirContainingNamesAwareScope>(
    override val firScope: S,
    builder: KtSymbolByFirBuilder,
    token: ValidityToken
) : KtFirDelegatingScope<S>(builder, token), ValidityTokenOwner
