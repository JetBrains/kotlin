/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.idea.fir.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.fir.AnalysisSessionFirImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

internal class KtSimpleNameReferenceFirImpl(
    expression: KtSimpleNameExpression
) : KtSimpleNameReference(expression), FirKtReference {
    override fun getResolvedToPsi(
        analysisSession: AnalysisSessionFirImpl,
        session: FirSession,
        state: FirModuleResolveState
    ) = FirReferenceResolveHelper.resolveSimpleNameReference(this, session, state)

    override fun doCanBeReferenceTo(candidateTarget: PsiElement): Boolean {
        return true // TODO
    }

    override fun isReferenceToWithoutExtensionChecking(candidateTarget: PsiElement): Boolean {
        return resolve() == candidateTarget //todo
    }

    override fun handleElementRename(newElementName: String): PsiElement? {
        TODO("Not yet implemented")
    }

    override fun bindToElement(element: PsiElement, shorteningMode: ShorteningMode): PsiElement {
        TODO("Not yet implemented")
    }

    override fun bindToFqName(fqName: FqName, shorteningMode: ShorteningMode, targetElement: PsiElement?): PsiElement {
        TODO("Not yet implemented")
    }

    override fun getImportAlias(): KtImportAlias? {
        TODO("Not yet implemented")
    }

    override val resolver get() = KtFirReferenceResolver
}