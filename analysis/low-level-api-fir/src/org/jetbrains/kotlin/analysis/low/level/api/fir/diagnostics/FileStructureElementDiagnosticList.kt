/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLDiagnostic

internal class FileStructureElementDiagnosticList(
    private val map: Map<PsiElement, List<LLDiagnostic>>
) {
    fun diagnosticsFor(element: PsiElement): List<LLDiagnostic> = map[element] ?: emptyList()

    inline fun forEach(action: (List<LLDiagnostic>) -> Unit) = map.values.forEach(action)
}
