/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.scopes

import org.jetbrains.kotlin.fir.scopes.impl.FirMemberTypeParameterScope
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtScope
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.name.Name

// Does nothing, it is used to close the hole in KtFirScopeProvider
// TODO: check if this scope should provide something useful
class KtFirMemberTypeParameterStubbingScope(
    val firMemberTypeParameterScope: FirMemberTypeParameterScope,
    override val token: ValidityToken,
) : KtScope {
    override fun getCallableNames(): Set<Name> {
        return emptySet()
    }

    override fun getClassLikeSymbolNames(): Set<Name> {
        return emptySet()
    }

    override fun getCallableSymbols(): Sequence<KtCallableSymbol> {
        return emptySequence()
    }

    override fun getClassClassLikeSymbols(): Sequence<KtClassLikeSymbol> {
        return emptySequence()
    }
}