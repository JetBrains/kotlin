/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.diagnostics.*

internal class KaCompilerPluginDiagnostic0Impl(
    firDiagnostic: KtPsiSimpleDiagnostic,
    token: KaLifetimeToken
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaCompilerPluginDiagnostic0

internal class KaCompilerPluginDiagnostic1Impl(
    firDiagnostic: KtPsiDiagnosticWithParameters1<*>,
    token: KaLifetimeToken,
    override val parameter1: Any?
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaCompilerPluginDiagnostic1

internal class KaCompilerPluginDiagnostic2Impl(
    firDiagnostic: KtPsiDiagnosticWithParameters2<*, *>,
    token: KaLifetimeToken,
    override val parameter1: Any?,
    override val parameter2: Any?
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaCompilerPluginDiagnostic2

internal class KaCompilerPluginDiagnostic3Impl(
    firDiagnostic: KtPsiDiagnosticWithParameters3<*, *, *>,
    token: KaLifetimeToken,
    override val parameter1: Any?,
    override val parameter2: Any?,
    override val parameter3: Any?
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaCompilerPluginDiagnostic3

internal class KaCompilerPluginDiagnostic4Impl(
    firDiagnostic: KtPsiDiagnosticWithParameters4<*, *, *, *>,
    token: KaLifetimeToken,
    override val parameter1: Any?,
    override val parameter2: Any?,
    override val parameter3: Any?,
    override val parameter4: Any?
) : KaAbstractFirDiagnostic<PsiElement>(firDiagnostic, token), KaCompilerPluginDiagnostic4
