/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.calls.KtSimpleFunctionCall
import org.jetbrains.kotlin.analysis.api.calls.calls
import org.jetbrains.kotlin.analysis.api.calls.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression

class KtFirInvokeFunctionReference(expression: KtCallExpression) : KtInvokeFunctionReference(expression), KtFirReference {
    override fun KtAnalysisSession.resolveToSymbols(): Collection<KtSymbol> {
        return expression.resolveCall()?.calls.orEmpty().mapNotNull { call ->
            (call as? KtSimpleFunctionCall)
                ?.takeIf { it.isImplicitInvoke }
                ?.partiallyAppliedSymbol
                ?.symbol
                ?.takeUnless { it is KtFunctionSymbol && it.isBuiltinFunctionInvoke }
        }
    }
}