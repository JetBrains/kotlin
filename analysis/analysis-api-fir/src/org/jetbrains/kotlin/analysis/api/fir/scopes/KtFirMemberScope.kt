/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.ValidityTokenOwner
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.weakRef
import org.jetbrains.kotlin.analysis.api.scopes.KtMemberScope
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers

internal class KtFirMemberScope(
    override val owner: KtSymbolWithMembers,
    override val firScope: FirTypeScope,
    token: ValidityToken,
    builder: KtSymbolByFirBuilder
) : KtFirDelegatingScope<FirTypeScope>(builder, token), KtMemberScope, ValidityTokenOwner
