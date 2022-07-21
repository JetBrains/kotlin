/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.components.KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.components.KtAnalysisSessionMixIn
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion

@KtAnalysisApiInternals
public abstract class KtSymbolProviderByJavaPsi : KtAnalysisSessionComponent() {
    public abstract fun getNamedClassSymbol(psiClass: PsiClass): KtNamedClassOrObjectSymbol?

    public abstract fun getCallableSymbol(callable: PsiMember): KtCallableSymbol?
}

@KtAnalysisApiInternals
public interface KtSymbolProviderByJavaPsiMixIn : KtAnalysisSessionMixIn {
    public fun PsiClass.getNamedClassSymbol(): KtNamedClassOrObjectSymbol? =
        withValidityAssertion { analysisSession.symbolProviderByJavaPsi.getNamedClassSymbol(this) }

    public fun PsiMember.getCallableSymbol(): KtCallableSymbol? =
        withValidityAssertion { analysisSession.symbolProviderByJavaPsi.getCallableSymbol(this) }
}