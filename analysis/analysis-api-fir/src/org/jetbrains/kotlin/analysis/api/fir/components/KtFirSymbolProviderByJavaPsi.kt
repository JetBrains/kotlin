/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.*
import com.intellij.psi.impl.compiled.ClsElementImpl
import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirPsiJavaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolProviderByJavaPsi
import org.jetbrains.kotlin.asJava.KtLightClassMarker
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.Name

@OptIn(KaAnalysisApiInternals::class)
internal class KaFirSymbolProviderByJavaPsi(
    override val analysisSession: KaFirSession,
) : KaSymbolProviderByJavaPsi(), KaFirSessionComponent {
    override fun getNamedClassSymbol(psiClass: PsiClass): KaNamedClassOrObjectSymbol? {
        if (psiClass.qualifiedName == null) return null
        if (psiClass is PsiTypeParameter) return null
        if (psiClass is KtLightClassMarker) return null
        if (psiClass.isKotlinCompiledClass()) return null

        return KaFirPsiJavaClassSymbol(psiClass, analysisSession)
    }

    private fun PsiClass.isKotlinCompiledClass() =
        this is ClsElementImpl && hasAnnotation(JvmAnnotationNames.METADATA_FQ_NAME.asString())

    override fun getCallableSymbol(callable: PsiMember): KaCallableSymbol? {
        if (callable !is PsiMethod && callable !is PsiField) return null
        val name = callable.name?.let(Name::identifier) ?: return null
        val containingClass = callable.containingClass ?: return null
        val classSymbol = getNamedClassSymbol(containingClass) ?: return null
        return with(analysisSession) {
            classSymbol.getCombinedDeclaredMemberScope().getCallableSymbols(name).firstOrNull { it.psi == callable }
        }
    }
}