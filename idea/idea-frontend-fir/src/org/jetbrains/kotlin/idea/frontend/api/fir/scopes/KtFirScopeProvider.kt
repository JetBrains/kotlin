/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.idea.frontend.api.ValidityOwner
import org.jetbrains.kotlin.idea.frontend.api.ValidityOwnerByValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirPackageSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtDeclaredMemberScope
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtMemberScope
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtPackageScope
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtScopeProvider
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import java.util.*

internal class KtFirScopeProvider(
    override val token: ValidityOwner,
    private val builder: KtSymbolByFirBuilder,
    session: FirSession
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
}