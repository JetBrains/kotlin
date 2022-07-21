/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolProviderByJavaPsi

@OptIn(KtAnalysisApiInternals::class)
internal class KtFe10SymbolProviderByJavaPsi(
    override val analysisSession: KtFe10AnalysisSession
) : KtSymbolProviderByJavaPsi(), Fe10KtAnalysisSessionComponent {
    override fun getNamedClassSymbol(psiClass: PsiClass): KtNamedClassOrObjectSymbol? {
        return null /*TODO*/
    }

    override fun getCallableSymbol(callable: PsiMember): KtCallableSymbol? {
        return null /*TODO*/
    }
}