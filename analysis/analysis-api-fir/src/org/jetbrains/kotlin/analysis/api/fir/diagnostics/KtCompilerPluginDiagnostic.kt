/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import com.intellij.psi.PsiElement

interface KtCompilerPluginDiagnostic0 : KtFirDiagnostic<PsiElement> {
    override val diagnosticClass
        get() = KtCompilerPluginDiagnostic0::class
}

interface KtCompilerPluginDiagnostic1 : KtFirDiagnostic<PsiElement> {
    val parameter1: Any?

    override val diagnosticClass
        get() = KtCompilerPluginDiagnostic1::class
}

interface KtCompilerPluginDiagnostic2 : KtFirDiagnostic<PsiElement> {
    val parameter1: Any?
    val parameter2: Any?

    override val diagnosticClass
        get() = KtCompilerPluginDiagnostic2::class
}

interface KtCompilerPluginDiagnostic3 : KtFirDiagnostic<PsiElement> {
    val parameter1: Any?
    val parameter2: Any?
    val parameter3: Any?

    override val diagnosticClass
        get() = KtCompilerPluginDiagnostic3::class
}

interface KtCompilerPluginDiagnostic4 : KtFirDiagnostic<PsiElement> {
    val parameter1: Any?
    val parameter2: Any?
    val parameter3: Any?
    val parameter4: Any?

    override val diagnosticClass
        get() = KtCompilerPluginDiagnostic4::class
}
