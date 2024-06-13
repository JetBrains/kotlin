/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.components.KaSessionMixIn
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion

@KaAnalysisApiInternals
public abstract class KaSymbolProviderByJavaPsi : KaSessionComponent() {
    public abstract fun getNamedClassSymbol(psiClass: PsiClass): KaNamedClassOrObjectSymbol?

    public abstract fun getCallableSymbol(callable: PsiMember): KaCallableSymbol?
}

@KaAnalysisApiInternals
public typealias KtSymbolProviderByJavaPsi = KaSymbolProviderByJavaPsi

@KaAnalysisApiInternals
public interface KaSymbolProviderByJavaPsiMixIn : KaSessionMixIn {
    public fun PsiClass.getNamedClassSymbol(): KaNamedClassOrObjectSymbol? =
        withValidityAssertion { analysisSession.symbolProviderByJavaPsi.getNamedClassSymbol(this) }

    public fun PsiMember.getCallableSymbol(): KaCallableSymbol? =
        withValidityAssertion { analysisSession.symbolProviderByJavaPsi.getCallableSymbol(this) }
}

@KaAnalysisApiInternals
public typealias KtSymbolProviderByJavaPsiMixIn = KaSymbolProviderByJavaPsiMixIn