/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

internal class KaFirTypeParameterSymbol(
    override val firSymbol: FirTypeParameterSymbol,
    override val analysisSession: KaFirSession,
) : KaFirTypeParameterSymbolBase() {
    override val token: KaLifetimeToken get() = builder.token
    override val psi: PsiElement? get() = withValidityAssertion { firSymbol.findPsi() }

    override val name: Name get() = withValidityAssertion { firSymbol.name }

    override val variance: Variance get() = withValidityAssertion { firSymbol.variance }
    override val isReified: Boolean get() = withValidityAssertion { firSymbol.isReified }

    override val location: KaSymbolLocation
        get() = withValidityAssertion {
            if (firSymbol.containingDeclarationSymbol is FirClassSymbol<*>) {
                KaSymbolLocation.CLASS
            } else {
                KaSymbolLocation.LOCAL
            }
        }
}
