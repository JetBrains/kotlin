/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.scopes

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractSimpleImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractStarImportingScope
import org.jetbrains.kotlin.idea.fir.getOrBuildFirOfType
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.LowLevelFirApiFacade
import org.jetbrains.kotlin.idea.frontend.api.ValidityOwner
import org.jetbrains.kotlin.idea.frontend.api.ValidityOwnerByValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirPackageSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.types.KtFirType
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.scopes.*
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
    override val token: ValidityOwner,
    private val builder: KtSymbolByFirBuilder,
    private val project: Project,
    session: FirSession,
    val firResolveState: FirModuleResolveState
) : KtScopeProvider(), ValidityOwnerByValidityToken {
    private val session by weakRef(session)

    private val memberScopeCache = IdentityHashMap<KtClassOrObjectSymbol, KtMemberScope>()
    private val declaredMemberScopeCache = IdentityHashMap<KtClassOrObjectSymbol, KtDeclaredMemberScope>()
    private val packageMemberScopeCache = IdentityHashMap<KtPackageSymbol, KtPackageScope>()

    override fun getMemberScope(classSymbol: KtClassOrObjectSymbol): KtMemberScope = withValidityAssertion {
        memberScopeCache.getOrPut(classSymbol) {
            check(classSymbol is KtFirClassOrObjectSymbol)
            KtFirMemberScope(classSymbol, token, builder)
        }
    }

    override fun getDeclaredMemberScope(classSymbol: KtClassOrObjectSymbol): KtDeclaredMemberScope = withValidityAssertion {
        declaredMemberScopeCache.getOrPut(classSymbol) {
            check(classSymbol is KtFirClassOrObjectSymbol)
            KtFirDeclaredMemberScope(classSymbol, token, builder)
        }
    }

    override fun getPackageScope(packageSymbol: KtPackageSymbol): KtPackageScope = withValidityAssertion {
        packageMemberScopeCache.getOrPut(packageSymbol) {
            check(packageSymbol is KtFirPackageSymbol)
            KtFirPackageScope(packageSymbol, token, builder, session)
        }
    }

    override fun getCompositeScope(subScopes: List<KtScope>): KtCompositeScope = withValidityAssertion {
        KtFirCompositeScope(subScopes, token)
    }

    override fun getScopeForType(type: KtType): KtScope? {
        check(type is KtFirType) { "KtFirScopePriovider can only work with KtFirType, but ${type::class} was provided" }

        val firTypeScope = type.coneType.scope(session, ScopeSession()) ?: return null
        return convertToKtScope(firTypeScope)
    }

    override fun getScopeContextForPosition(originalFile: KtFile, positionInFakeFile: KtElement): KtScopeContext = withValidityAssertion {
        val originalFirFile = originalFile.getOrBuildFirOfType<FirFile>(firResolveState)
        val fakeEnclosingFunction = positionInFakeFile.getNonStrictParentOfType<KtNamedFunction>()
            ?: error("Cannot find enclosing function for ${positionInFakeFile.getElementTextInContext()}")

        val completionContext = LowLevelFirApiFacade.buildCompletionContextForFunction(
            originalFirFile,
            fakeEnclosingFunction,
            state = firResolveState
        )

        val towerDataContext = completionContext.getTowerDataContext(positionInFakeFile)

        val implicitReceivers = towerDataContext.nonLocalTowerDataElements.mapNotNull { it.implicitReceiver }
        val implicitReceiversTypes = implicitReceivers.map { builder.buildKtType(it.type) }

        val implicitReceiverScopes = implicitReceivers.mapNotNull { it.implicitScope }
        val nonLocalScopes = towerDataContext.nonLocalTowerDataElements.mapNotNull { it.scope }
        val firLocalScopes = towerDataContext.localScopes

        @OptIn(ExperimentalStdlibApi::class)
        val allKtScopes = buildList {
            implicitReceiverScopes.mapTo(this, ::convertToKtScope)
            nonLocalScopes.mapTo(this, ::convertToKtScope)
            firLocalScopes.mapTo(this, ::convertToKtScope)
        }

        KtScopeContext(getCompositeScope(allKtScopes), implicitReceiversTypes)
    }

    private fun convertToKtScope(firScope: FirScope): KtScope = when (firScope) {
        is FirAbstractSimpleImportingScope -> KtFirNonStarImportingScope(firScope, builder, token)
        is FirAbstractStarImportingScope -> KtFirStarImportingScope(firScope, builder, project, token)
        else -> {
            // todo create concrete KtScope here instead of a generic one
            KtFirDelegatingScopeImpl(firScope, builder, token)
        }
    }
}

private class KtFirDelegatingScopeImpl(firScope: FirScope, builder: KtSymbolByFirBuilder, override val token: ValidityOwner) :
    KtFirDelegatingScope(builder), ValidityOwnerByValidityToken {
    override val firScope: FirScope = firScope
}