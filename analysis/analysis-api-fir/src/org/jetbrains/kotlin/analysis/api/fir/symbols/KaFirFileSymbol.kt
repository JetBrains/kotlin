/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol
import org.jetbrains.kotlin.psi.KtFile

internal class KaFirFileSymbol private constructor(
    override val backingPsi: KtFile?,
    override val lazyFirSymbol: Lazy<FirFileSymbol>,
    override val analysisSession: KaFirSession,
) : KaFileSymbol(), KaFirKtBasedSymbol<KtFile, FirFileSymbol> {
    constructor(file: KtFile, session: KaFirSession) : this(
        backingPsi = file,
        lazyFirSymbol = lazyPub {
            file.getOrBuildFirFile(session.resolutionFacade).symbol
        },
        analysisSession = session,
    )

    constructor(symbol: FirFileSymbol, session: KaFirSession) : this(
        backingPsi = symbol.fir.psi as? KtFile,
        lazyFirSymbol = lazyOf(symbol),
        analysisSession = session,
    )

    override val psi: PsiElement? get() = withValidityAssertion { backingPsi }

    override fun createPointer(): KaSymbolPointer<KaFileSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource()
            ?: TODO("Creating pointers for not PSI-backed files or files from a library is not supported yet")
    }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            psiOrSymbolAnnotationList()
        }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}
