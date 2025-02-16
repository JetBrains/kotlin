/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirScriptSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaScriptSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.symbols.impl.FirScriptSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal class KaFirScriptSymbol private constructor(
    override val backingPsi: KtScript?,
    override val lazyFirSymbol: Lazy<FirScriptSymbol>,
    override val analysisSession: KaFirSession,
) : KaScriptSymbol(), KaFirKtBasedSymbol<KtScript, FirScriptSymbol> {
    constructor(declaration: KtScript, session: KaFirSession) : this(
        backingPsi = declaration,
        lazyFirSymbol = lazyFirSymbol(declaration, session),
        analysisSession = session,
    )

    constructor(symbol: FirScriptSymbol, session: KaFirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? KtScript,
        lazyFirSymbol = lazyOf(symbol),
        analysisSession = session,
    )

    override val psi: PsiElement? get() = withValidityAssertion { backingPsi }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override val name: Name
        get() = withValidityAssertion {
            backingPsi?.containingKtFile?.name?.let {
                Name.special("<script-$it>")
            } ?: firSymbol.fir.name
        }

    override fun createPointer(): KaSymbolPointer<KaScriptSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<KaScriptSymbol>()?.let { return it }

        with(analysisSession) {
            val file = containingFile ?: errorWithAttachment("Containing file is not found") {
                withFirSymbolEntry("firScript", firSymbol)
            }

            KaFirScriptSymbolPointer(file.createPointer(), this@KaFirScriptSymbol)
        }
    }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}