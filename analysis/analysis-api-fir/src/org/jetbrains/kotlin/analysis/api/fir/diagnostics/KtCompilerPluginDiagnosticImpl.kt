/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.diagnostics.*

internal class KtCompilerPluginDiagnostic0Impl(
    firDiagnostic: KtPsiSimpleDiagnostic,
    token: KtLifetimeToken
) : KtAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KtCompilerPluginDiagnostic0

internal class KtCompilerPluginDiagnostic1Impl(
    firDiagnostic: KtPsiDiagnosticWithParameters1<*>,
    token: KtLifetimeToken,
    override val parameter1: Any?
) : KtAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KtCompilerPluginDiagnostic1

internal class KtCompilerPluginDiagnostic2Impl(
    firDiagnostic: KtPsiDiagnosticWithParameters2<*, *>,
    token: KtLifetimeToken,
    override val parameter1: Any?,
    override val parameter2: Any?
) : KtAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KtCompilerPluginDiagnostic2

internal class KtCompilerPluginDiagnostic3Impl(
    firDiagnostic: KtPsiDiagnosticWithParameters3<*, *, *>,
    token: KtLifetimeToken,
    override val parameter1: Any?,
    override val parameter2: Any?,
    override val parameter3: Any?
) : KtAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KtCompilerPluginDiagnostic3

internal class KtCompilerPluginDiagnostic4Impl(
    firDiagnostic: KtPsiDiagnosticWithParameters4<*, *, *, *>,
    token: KtLifetimeToken,
    override val parameter1: Any?,
    override val parameter2: Any?,
    override val parameter3: Any?,
    override val parameter4: Any?
) : KtAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KtCompilerPluginDiagnostic4
