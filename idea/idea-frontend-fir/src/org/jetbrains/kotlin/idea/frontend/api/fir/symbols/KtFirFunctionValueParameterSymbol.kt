/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionParameterSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbolKind
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.name.Name

internal class KtFirFunctionValueParameterSymbol(
    fir: FirValueParameterImpl,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtFunctionParameterSymbol(), KtFirSymbol<FirValueParameterImpl> {
    override val fir: FirValueParameterImpl by weakRef(fir)
    override val psi: PsiElement? by cached { fir.findPsi(fir.session) }

    override val name: Name get() = withValidityAssertion { fir.name }
    override val isVararg: Boolean get() = withValidityAssertion { fir.isVararg }
    override val type: KtType by cached { builder.buildKtType(fir.returnTypeRef) }
    override val symbolKind: KtSymbolKind get() = KtSymbolKind.LOCAL
}