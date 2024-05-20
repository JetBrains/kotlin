/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import com.intellij.psi.PsiElement

interface KaCompilerPluginDiagnostic0 : KaFirDiagnostic<PsiElement> {
    override val diagnosticClass
        get() = KaCompilerPluginDiagnostic0::class
}

typealias KtCompilerPluginDiagnostic0 = KaCompilerPluginDiagnostic0

interface KaCompilerPluginDiagnostic1 : KaFirDiagnostic<PsiElement> {
    val parameter1: Any?

    override val diagnosticClass
        get() = KaCompilerPluginDiagnostic1::class
}

typealias KtCompilerPluginDiagnostic1 = KaCompilerPluginDiagnostic1

interface KaCompilerPluginDiagnostic2 : KaFirDiagnostic<PsiElement> {
    val parameter1: Any?
    val parameter2: Any?

    override val diagnosticClass
        get() = KaCompilerPluginDiagnostic2::class
}

typealias KtCompilerPluginDiagnostic2 = KaCompilerPluginDiagnostic2

interface KaCompilerPluginDiagnostic3 : KaFirDiagnostic<PsiElement> {
    val parameter1: Any?
    val parameter2: Any?
    val parameter3: Any?

    override val diagnosticClass
        get() = KaCompilerPluginDiagnostic3::class
}

typealias KtCompilerPluginDiagnostic3 = KaCompilerPluginDiagnostic3

interface KaCompilerPluginDiagnostic4 : KaFirDiagnostic<PsiElement> {
    val parameter1: Any?
    val parameter2: Any?
    val parameter3: Any?
    val parameter4: Any?

    override val diagnosticClass
        get() = KaCompilerPluginDiagnostic4::class
}

typealias KtCompilerPluginDiagnostic4 = KaCompilerPluginDiagnostic4