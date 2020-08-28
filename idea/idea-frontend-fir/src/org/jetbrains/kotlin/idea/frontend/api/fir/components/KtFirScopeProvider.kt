/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractSimpleImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractStarImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.idea.fir.getOrBuildFirOfType
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.LowLevelFirApiFacade
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.components.KtScopeContext
import org.jetbrains.kotlin.idea.frontend.api.components.KtScopeProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.scopes.*
import org.jetbrains.kotlin.idea.frontend.api.fir.scopes.KtFirDeclaredMemberScope
import org.jetbrains.kotlin.idea.frontend.api.fir.scopes.KtFirDelegatingScope
import org.jetbrains.kotlin.idea.frontend.api.fir.scopes.KtFirMemberScope
import org.jetbrains.kotlin.idea.frontend.api.fir.scopes.KtFirNonStarImportingScope
import org.jetbrains.kotlin.idea.frontend.api.fir.scopes.KtFirPackageScope
import org.jetbrains.kotlin.idea.frontend.api.fir.scopes.KtFirStarImportingScope
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.types.KtFirType
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.scopes.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import java.util.*

internal class KtFirScopeProvider(
    analysisSession: KtAnalysisSession,
    private val builder: KtSymbolByFirBuilder,
    private val project: Project,
    firResolveState: FirModuleResolveState,
    override val token: ValidityToken,
) : KtScopeProvider(), ValidityTokenOwner {
    override val analysisSession: KtAnalysisSession by weakRef(analysisSession)
    private val firResolveState by weakRef(firResolveState)
    private val firScopeStorage = FirScopeRegistry()

    private val memberScopeCache = IdentityHashMap<KtClassOrObjectSymbol, KtMemberScope>()
    private val declaredMemberScopeCache = IdentityHashMap<KtClassOrObjectSymbol, KtDeclaredMemberScope>()
    private val packageMemberScopeCache = IdentityHashMap<KtPackageSymbol, KtPackageScope>()

    override fun getMemberScope(classSymbol: KtClassOrObjectSymbol): KtMemberScope = withValidityAssertion {
        memberScopeCache.getOrPut(classSymbol) {
            check(classSymbol is KtFirClassOrObjectSymbol)
            val firScope =
                classSymbol.firRef.withFir(FirResolvePhase.SUPER_TYPES) { fir ->
                    fir.unsubstitutedScope(fir.session, ScopeSession())
                }.also(firScopeStorage::register)
            KtFirMemberScope(classSymbol, firScope, token, builder)
        }
    }

    override fun getDeclaredMemberScope(classSymbol: KtClassOrObjectSymbol): KtDeclaredMemberScope = withValidityAssertion {
        declaredMemberScopeCache.getOrPut(classSymbol) {
            check(classSymbol is KtFirClassOrObjectSymbol)
            val firScope = classSymbol.firRef.withFir(FirResolvePhase.SUPER_TYPES) { declaredMemberScope(it) }
                .also(firScopeStorage::register)
            KtFirDeclaredMemberScope(classSymbol, firScope, token, builder)
        }
    }

    override fun getPackageScope(packageSymbol: KtPackageSymbol): KtPackageScope = withValidityAssertion {
        packageMemberScopeCache.getOrPut(packageSymbol) {
            val firPackageScope =
                FirPackageMemberScope(
                    packageSymbol.fqName,
                    firResolveState.firIdeSourcesSession/*TODO use correct session here*/
                ).also(firScopeStorage::register)
            KtFirPackageScope(firPackageScope, project, builder, token)
        }
    }

    override fun getCompositeScope(subScopes: List<KtScope>): KtCompositeScope = withValidityAssertion {
        KtFirCompositeScope(subScopes, token)
    }

    override fun getTypeScope(type: KtType): KtScope? {
        check(type is KtFirType) { "KtFirScopeProvider can only work with KtFirType, but ${type::class} was provided" }

        val firTypeScope = type.coneType.scope(firResolveState.firIdeSourcesSession, ScopeSession()) ?: return null
        return convertToKtScope(firTypeScope)
    }

    override fun getScopeContextForPosition(
        originalFile: KtFile,
        originalPosition: PsiElement,
        positionInFakeFile: KtElement
    ): KtScopeContext = withValidityAssertion {
        val originalFirFile = originalFile.getOrBuildFirOfType<FirFile>(firResolveState)
        val fakeEnclosingFunction = positionInFakeFile.getNonStrictParentOfType<KtNamedFunction>()
            ?: error("Cannot find enclosing function for ${positionInFakeFile.getElementTextInContext()}")
        val originalEnclosingFunction = originalPosition.getNonStrictParentOfType<KtNamedFunction>()
            ?: error("Cannot find original enclosing function for $originalPosition")

        val completionContext = LowLevelFirApiFacade.buildCompletionContextForFunction(
            originalFirFile,
            fakeEnclosingFunction,
            originalEnclosingFunction,
            state = firResolveState
        )

        val towerDataContext = completionContext.getTowerDataContext(positionInFakeFile)

        val implicitReceivers = towerDataContext.nonLocalTowerDataElements.mapNotNull { it.implicitReceiver }.distinct()
        val implicitReceiversTypes = implicitReceivers.map { builder.buildKtType(it.type) }

        val implicitReceiverScopes = implicitReceivers.mapNotNull { it.implicitScope }
        val nonLocalScopes = towerDataContext.nonLocalTowerDataElements.mapNotNull { it.scope }.distinct()
        val firLocalScopes = towerDataContext.localScopes

        @OptIn(ExperimentalStdlibApi::class)
        val allKtScopes = buildList {
            implicitReceiverScopes.mapTo(this, ::convertToKtScope)
            nonLocalScopes.mapTo(this, ::convertToKtScope)
            firLocalScopes.mapTo(this, ::convertToKtScope)
        }

        KtScopeContext(getCompositeScope(allKtScopes), implicitReceiversTypes)
    }

    private fun convertToKtScope(firScope: FirScope): KtScope {
        firScopeStorage.register(firScope)
        return when (firScope) {
            is FirAbstractSimpleImportingScope -> KtFirNonStarImportingScope(firScope, builder, token)
            is FirAbstractStarImportingScope -> KtFirStarImportingScope(firScope, builder, project, token)
            is FirPackageMemberScope -> KtFirPackageScope(firScope, project, builder, token)
            is FirContainingNamesAwareScope -> KtFirDelegatingScopeImpl(firScope, builder, token)
            else -> TODO(firScope::class.toString())
        }
    }
}

private class KtFirDelegatingScopeImpl<S>(
    firScope: S, builder: KtSymbolByFirBuilder,
    token: ValidityToken
) : KtFirDelegatingScope<S>(builder, token), ValidityTokenOwner where S : FirContainingNamesAwareScope, S : FirScope {
    override val firScope: S by weakRef(firScope)
}

/**
 * Stores strong references to all instances of [FirScope] used
 * Needed as the only entity which may have a strong references to FIR internals is [KtFirAnalysisSession] & [KtAnalysisSessionComponent]
 * Entities which needs storing [FirScope] instances will store them as weak references via [org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef]
 */
internal class FirScopeRegistry {
    private val scopes = mutableListOf<FirScope>()

    fun register(scope: FirScope) {
        scopes += scope
    }
}
