/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.ValidityTokenOwner
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.weakRef
import org.jetbrains.kotlin.analysis.api.scopes.KtDeclaredMemberScope
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers

internal class KtFirDeclaredMemberScope(
    override val owner: KtSymbolWithMembers,
    override val firScope: FirClassDeclaredMemberScope,
    token: ValidityToken,
    builder: KtSymbolByFirBuilder
) : KtFirDelegatingScope<FirClassDeclaredMemberScope>(builder, token),
    KtDeclaredMemberScope,
    ValidityTokenOwner