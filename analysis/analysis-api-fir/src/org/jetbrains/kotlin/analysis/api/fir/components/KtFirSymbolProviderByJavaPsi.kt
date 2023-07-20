/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.*
import com.intellij.psi.impl.compiled.ClsElementImpl
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirPsiJavaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolProviderByJavaPsi
import org.jetbrains.kotlin.asJava.KtLightClassMarker
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.Name

@OptIn(KtAnalysisApiInternals::class)
internal class KtFirSymbolProviderByJavaPsi(
    override val analysisSession: KtFirAnalysisSession,
) : KtSymbolProviderByJavaPsi(), KtFirAnalysisSessionComponent {
    override fun getNamedClassSymbol(psiClass: PsiClass): KtNamedClassOrObjectSymbol? {
        if (psiClass.qualifiedName == null) return null
        if (psiClass is PsiTypeParameter) return null
        if (psiClass is KtLightClassMarker) return null
        if (psiClass.isKotlinCompiledClass()) return null

        return KtFirPsiJavaClassSymbol(psiClass, analysisSession)
    }

    private fun PsiClass.isKotlinCompiledClass() =
        this is ClsElementImpl && hasAnnotation(JvmAnnotationNames.METADATA_FQ_NAME.asString())

    override fun getCallableSymbol(callable: PsiMember): KtCallableSymbol? {
        if (callable !is PsiMethod && callable !is PsiField) return null
        val name = callable.name?.let(Name::identifier) ?: return null
        val containingClass = callable.containingClass ?: return null
        val classSymbol = getNamedClassSymbol(containingClass) ?: return null
        return with(analysisSession) {
            listOfNotNull(classSymbol.getDeclaredMemberScope(), classSymbol.getStaticMemberScope())
                .asCompositeScope()
                .getCallableSymbols(name)
                .firstOrNull { it.psi == callable }
        }
    }
}