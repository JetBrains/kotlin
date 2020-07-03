/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.modality
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.ValidityOwner
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCommonSymbolModality
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtJavaFieldSymbol
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.name.Name

internal class KtFirJavaFieldSymbol(
    fir: FirField,
    override val token: ValidityOwner,
    private val builder: KtSymbolByFirBuilder
) : KtJavaFieldSymbol(), KtFirSymbol<FirField> {
    override val fir: FirField by weakRef(fir)
    override val psi: PsiElement? by cached { fir.findPsi(fir.session) }

    override val type: KtType by cached { builder.buildKtType(fir.returnTypeRef) }
    override val isVal: Boolean get() = withValidityAssertion { fir.isVal }
    override val name: Name get() = withValidityAssertion { fir.name }

    override val modality: KtCommonSymbolModality get() = withValidityAssertion { fir.modality.getSymbolModality() }
}