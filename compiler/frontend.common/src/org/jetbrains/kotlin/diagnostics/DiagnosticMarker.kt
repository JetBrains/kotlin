/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.K1Deprecation

interface DiagnosticMarker {

    /**
     * When working with [KtDiagnosticWithSource] consider using [KtDiagnosticWithSource.element] instead
     */
    @K1Deprecation
    val psiElement: PsiElement
        get() = error("psiElement should be called only on diagnostics with KtPsiSourceElement inside")

    val factoryName: String
    val severity: Severity
    val textRanges: List<TextRange>
}
