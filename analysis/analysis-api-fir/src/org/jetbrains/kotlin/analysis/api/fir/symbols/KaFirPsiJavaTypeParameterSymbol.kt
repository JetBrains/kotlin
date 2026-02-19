/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiTypeParameter
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaBaseEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance

/**
 * [KaFirPsiJavaTypeParameterSymbol] is a PSI-based type parameter symbol with a lazy [firSymbol]. Some properties such as [name] are
 * computed based on the PSI. This is used by [KaFirPsiJavaClassSymbol] to avoid building its own FIR symbol when a list of type parameters
 * is requested.
 */
internal class KaFirPsiJavaTypeParameterSymbol(
    override val backingPsi: PsiTypeParameter,
    override val analysisSession: KaFirSession,
    origin: KaSymbolOrigin,
    computeFirSymbol: () -> FirTypeParameterSymbol,
) : KaFirTypeParameterSymbolBase<PsiTypeParameter>() {
    private val backingOrigin: KaSymbolOrigin = origin

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            if (backingPsi.annotations.isEmpty())
                KaBaseEmptyAnnotationList(token)
            else
                KaFirAnnotationListForDeclaration.create(firSymbol, builder)
        }

    override val name: Name
        get() = withValidityAssertion {
            backingPsi.name?.let { Name.identifier(it) } ?: SpecialNames.NO_NAME_PROVIDED
        }

    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsi }

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion { backingOrigin }

    override val variance: Variance
        get() = withValidityAssertion { Variance.INVARIANT }

    override val isReified: Boolean
        get() = withValidityAssertion { false }

    override val location: KaSymbolLocation
        get() = withValidityAssertion {
            if (backingPsi.owner is PsiClass) {
                KaSymbolLocation.CLASS
            } else {
                KaSymbolLocation.LOCAL
            }
        }

    override val lazyFirSymbol: Lazy<FirTypeParameterSymbol> = lazyPub(computeFirSymbol)
}
