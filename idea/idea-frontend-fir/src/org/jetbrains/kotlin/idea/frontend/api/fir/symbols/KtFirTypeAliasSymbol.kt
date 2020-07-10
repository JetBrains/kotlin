/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.ReadOnlyWeakRef
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtTypeAliasSymbol
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KtFirTypeAliasSymbol(
    fir: FirTypeAlias,
    override val token: ValidityToken
) : KtTypeAliasSymbol(), KtFirSymbol<FirTypeAlias> {
    override val fir: FirTypeAlias by weakRef(fir)
    override val psi: PsiElement? by cached { fir.findPsi(fir.session) }
    override val name: Name get() = withValidityAssertion { fir.name }
    override val classId: ClassId get() = withValidityAssertion { fir.symbol.classId }
}