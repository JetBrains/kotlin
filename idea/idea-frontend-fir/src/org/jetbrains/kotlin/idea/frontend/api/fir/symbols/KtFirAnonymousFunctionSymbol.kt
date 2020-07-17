/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.frontend.api.ValidityOwner
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.*

internal class KtFirAnonymousFunctionSymbol(
    fir: FirAnonymousFunction,
    override val token: ValidityOwner,
    private val builder: KtSymbolByFirBuilder
) : KtAnonymousFunctionSymbol(), KtFirSymbol<FirAnonymousFunction> {
    override val fir: FirAnonymousFunction by weakRef(fir)
    override val psi: PsiElement? by cached { fir.findPsi(fir.session) }

    override val type: KtType by cached { builder.buildKtType(fir.returnTypeRef) }
    override val valueParameters: List<KtParameterSymbol> by cached {
        fir.valueParameters.map { valueParameter ->
            check(valueParameter is FirValueParameterImpl)
            builder.buildParameterSymbol(valueParameter)
        }
    }

    override fun createPointer(): KtSymbolPointer<KtAnonymousFunctionSymbol> = NonRestorableKtSymbolPointer
}