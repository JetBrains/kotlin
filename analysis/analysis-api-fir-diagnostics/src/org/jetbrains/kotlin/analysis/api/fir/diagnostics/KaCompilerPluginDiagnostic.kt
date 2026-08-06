/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import kotlin.reflect.KClass

@KaUnstableDiagnosticApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaCompilerPluginDiagnostic0 : KaFirDiagnostic<PsiElement> {
    override val diagnosticClass: KClass<KaCompilerPluginDiagnostic0>
        get() = KaCompilerPluginDiagnostic0::class
}

@KaUnstableDiagnosticApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaCompilerPluginDiagnostic1 : KaFirDiagnostic<PsiElement> {
    public val parameter1: Any?

    override val diagnosticClass: KClass<KaCompilerPluginDiagnostic1>
        get() = KaCompilerPluginDiagnostic1::class
}

@KaUnstableDiagnosticApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaCompilerPluginDiagnostic2 : KaFirDiagnostic<PsiElement> {
    public val parameter1: Any?
    public val parameter2: Any?

    override val diagnosticClass: KClass<KaCompilerPluginDiagnostic2>
        get() = KaCompilerPluginDiagnostic2::class
}

@KaUnstableDiagnosticApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaCompilerPluginDiagnostic3 : KaFirDiagnostic<PsiElement> {
    public val parameter1: Any?
    public val parameter2: Any?
    public val parameter3: Any?

    override val diagnosticClass: KClass<KaCompilerPluginDiagnostic3>
        get() = KaCompilerPluginDiagnostic3::class
}

@KaUnstableDiagnosticApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaCompilerPluginDiagnostic4 : KaFirDiagnostic<PsiElement> {
    public val parameter1: Any?
    public val parameter2: Any?
    public val parameter3: Any?
    public val parameter4: Any?

    override val diagnosticClass: KClass<KaCompilerPluginDiagnostic4>
        get() = KaCompilerPluginDiagnostic4::class
}
