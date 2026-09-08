/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.fir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithSource

@KaImplementationDetail
val KtDiagnosticWithSource.psi: PsiElement
    get() = (element as KtPsiSourceElement).psi
