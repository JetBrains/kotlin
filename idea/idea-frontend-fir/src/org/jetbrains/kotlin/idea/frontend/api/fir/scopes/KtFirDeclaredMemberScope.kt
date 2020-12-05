/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.scopes

import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtDeclaredMemberScope
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtUnsubstitutedScope
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithDeclarations

internal class KtFirDeclaredMemberScope(
    override val owner: KtSymbolWithDeclarations,
    firScope: FirClassDeclaredMemberScope,
    token: ValidityToken,
    builder: KtSymbolByFirBuilder
) : KtFirDelegatingScope<FirClassDeclaredMemberScope>(builder, token),
    KtDeclaredMemberScope,
    KtUnsubstitutedScope<KtFirDeclaredMemberScope>,
    ValidityTokenOwner {
    override val firScope: FirClassDeclaredMemberScope by weakRef(firScope)
}

