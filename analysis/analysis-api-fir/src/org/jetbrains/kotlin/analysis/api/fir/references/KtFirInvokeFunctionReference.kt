/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.calls.KaSimpleFunctionCall
import org.jetbrains.kotlin.analysis.api.calls.calls
import org.jetbrains.kotlin.analysis.api.calls.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtImportAlias

class KaFirInvokeFunctionReference(expression: KtCallExpression) : KtInvokeFunctionReference(expression), KaFirReference {
    override fun KaSession.resolveToSymbols(): Collection<KaSymbol> {
        return expression.resolveCallOld()?.calls.orEmpty().mapNotNull { call ->
            (call as? KaSimpleFunctionCall)
                ?.takeIf { it.isImplicitInvoke }
                ?.partiallyAppliedSymbol
                ?.symbol
                ?.takeUnless { it is KaFunctionSymbol && it.isBuiltinFunctionInvoke }
        }
    }

    override fun isReferenceToImportAlias(alias: KtImportAlias): Boolean {
        return super<KaFirReference>.isReferenceToImportAlias(alias)
    }
}