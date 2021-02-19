/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.scopes

import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtMemberScope
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtUnsubstitutedScope
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithMembers

internal class KtFirMemberScope(
    override val owner: KtSymbolWithMembers,
    firScope: FirTypeScope,
    token: ValidityToken,
    builder: KtSymbolByFirBuilder
) : KtFirDelegatingScope<FirTypeScope>(builder, token), KtMemberScope, KtUnsubstitutedScope<KtMemberScope>, ValidityTokenOwner {
    override val firScope: FirTypeScope by weakRef(firScope)
}

