/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic

internal class FileStructureElementDiagnosticList(
    private val map: Map<PsiElement, List<KtPsiDiagnostic>>
) {
    fun diagnosticsFor(element: PsiElement): List<KtPsiDiagnostic> = map[element] ?: emptyList()

    inline fun forEach(action: (List<KtPsiDiagnostic>) -> Unit) = map.values.forEach(action)
}
