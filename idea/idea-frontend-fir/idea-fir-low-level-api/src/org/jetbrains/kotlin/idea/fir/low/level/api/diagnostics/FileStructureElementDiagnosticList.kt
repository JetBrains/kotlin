/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic

internal class FileStructureElementDiagnosticList(
    private val map: Map<PsiElement, List<FirPsiDiagnostic>>
) {
    fun diagnosticsFor(element: PsiElement): List<FirPsiDiagnostic> = map[element] ?: emptyList()

    inline fun forEach(action: (List<FirPsiDiagnostic>) -> Unit) = map.values.forEach(action)
}
