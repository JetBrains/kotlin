/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers.diagnostics.factories

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext

interface DebugInfoDiagnosticFactory {
    val withExplicitDefinitionOnly: Boolean

    fun createDiagnostic(
        expression: KtExpression,
        bindingContext: BindingContext
    ): Diagnostic
}
