/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.components.psiDeclarationProvider

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.providers.DecompiledPsiDeclarationProvider.findPsi
import org.jetbrains.kotlin.analysis.test.framework.utils.unwrapMultiReferences
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.KtFile

internal fun findReferencesAtCaret(mainKtFile: KtFile, caretPosition: Int): List<KtReference> =
    mainKtFile.findReferenceAt(caretPosition)?.unwrapMultiReferences().orEmpty().filterIsInstance<KtReference>()

// Mimic [psiForUast] in FIR UAST
internal fun KtAnalysisSession.psiForTest(symbol: KtSymbol, project: Project): PsiElement? {
    return when (symbol.origin) {
        KtSymbolOrigin.LIBRARY -> {
            findPsi(symbol, project) ?: symbol.psi
        }
        KtSymbolOrigin.SUBSTITUTION_OVERRIDE, KtSymbolOrigin.INTERSECTION_OVERRIDE -> {
            psiForTest((symbol as KtCallableSymbol).unwrapFakeOverrides, project)
        }
        else -> symbol.psi
    }
}