/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.diagnostics.*

internal class KtCompilerPluginDiagnostic0Impl(
    override val firDiagnostic: KtPsiSimpleDiagnostic,
    override val token: KtLifetimeToken
) : KtCompilerPluginDiagnostic0()

internal class KtCompilerPluginDiagnostic1Impl(
    override val firDiagnostic: KtPsiDiagnosticWithParameters1<*>,
    override val token: KtLifetimeToken,
    override val parameter1: Any?
) : KtCompilerPluginDiagnostic1()

internal class KtCompilerPluginDiagnostic2Impl(
    override val firDiagnostic: KtPsiDiagnosticWithParameters2<*, *>,
    override val token: KtLifetimeToken,
    override val parameter1: Any?,
    override val parameter2: Any?
) : KtCompilerPluginDiagnostic2()

internal class KtCompilerPluginDiagnostic3Impl(
    override val firDiagnostic: KtPsiDiagnosticWithParameters3<*, *, *>,
    override val token: KtLifetimeToken,
    override val parameter1: Any?,
    override val parameter2: Any?,
    override val parameter3: Any?
) : KtCompilerPluginDiagnostic3()

internal class KtCompilerPluginDiagnostic4Impl(
    override val firDiagnostic: KtPsiDiagnosticWithParameters4<*, *, *, *>,
    override val token: KtLifetimeToken,
    override val parameter1: Any?,
    override val parameter2: Any?,
    override val parameter3: Any?,
    override val parameter4: Any?
) : KtCompilerPluginDiagnostic4()
