/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolProviderByJavaPsi

@OptIn(KaAnalysisApiInternals::class)
internal class KaFe10SymbolProviderByJavaPsi(
    override val analysisSession: KaFe10Session
) : KaSymbolProviderByJavaPsi(), KaFe10SessionComponent {
    override fun getNamedClassSymbol(psiClass: PsiClass): KaNamedClassOrObjectSymbol? {
        return null /*TODO*/
    }

    override fun getCallableSymbol(callable: PsiMember): KaCallableSymbol? {
        return null /*TODO*/
    }
}