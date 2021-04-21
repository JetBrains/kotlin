/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.idea.fir.findPsi
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.firRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtTypeAliasSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KtFirTypeAliasSymbol(
    fir: FirTypeAlias,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken
) : KtTypeAliasSymbol(), KtFirSymbol<FirTypeAlias> {
    override val firRef = firRef(fir, resolveState)
    override val psi: PsiElement? by firRef.withFirAndCache { fir -> fir.findPsi(fir.declarationSiteSession) }
    override val name: Name get() = firRef.withFir { it.name }
    override val classIdIfNonLocal: ClassId get() = firRef.withFir { it.symbol.classId }

    override fun createPointer(): KtSymbolPointer<KtTypeAliasSymbol> {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }
        TODO("Creating symbols for library typealiases is not supported yet")
    }
}
