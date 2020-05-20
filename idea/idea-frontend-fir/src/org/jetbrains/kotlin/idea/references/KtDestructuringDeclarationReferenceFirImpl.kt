/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirComponentCall
import org.jetbrains.kotlin.idea.fir.*
import org.jetbrains.kotlin.idea.frontend.api.fir.AnalysisSessionFirImpl
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry

class KtDestructuringDeclarationReferenceFirImpl(
    element: KtDestructuringDeclarationEntry
) : KtDestructuringDeclarationReference(element), FirKtReference {
    override fun canRename(): Boolean = false //todo

    override fun getResolvedToPsi(
        analysisSession: AnalysisSessionFirImpl,
        session: FirSession,
        state: FirModuleResolveState
    ): Collection<PsiElement> {
        val fir = expression.getOrBuildFirSafe<FirProperty>(state) ?: return emptyList()
        val componentFunctionSymbol = (fir.initializer as? FirComponentCall)?.getCalleeSymbol() ?: return emptyList()
        return listOfNotNull(componentFunctionSymbol.fir.findPsi(session))
    }
}
