/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiTypeParameter
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolProviderByJavaPsi
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.firClassByPsiClassProvider
import org.jetbrains.kotlin.analysis.project.structure.getKtModule
import org.jetbrains.kotlin.name.Name

@OptIn(KtAnalysisApiInternals::class)
internal class KtFirSymbolProviderByJavaPsi(
    override val analysisSession: KtFirAnalysisSession,
) : KtSymbolProviderByJavaPsi(), KtFirAnalysisSessionComponent {
    override fun getNamedClassSymbol(psiClass: PsiClass): KtNamedClassOrObjectSymbol? {
        if (psiClass.qualifiedName == null) return null
        if (psiClass is PsiTypeParameter) return null
        val module = psiClass.getKtModule(analysisSession.project)
        val provider = firResolveSession.getSessionFor(module).firClassByPsiClassProvider
        val firClass = provider.getFirClass(psiClass) ?: return null
        return firSymbolBuilder.classifierBuilder.buildNamedClassOrObjectSymbol(firClass)
    }

    override fun getCallableSymbol(callable: PsiMember): KtCallableSymbol? {
        if (callable !is PsiMethod && callable !is PsiField) return null
        val name = callable.name?.let(Name::identifier) ?: return null
        val containingClass = callable.containingClass ?: return null
        val classSymbol = getNamedClassSymbol(containingClass) ?: return null
        return with(analysisSession)  {
            classSymbol.getDeclaredMemberScope()
                .getCallableSymbols { it == name }
                .firstOrNull { it.psi == callable }
        }
    }
}