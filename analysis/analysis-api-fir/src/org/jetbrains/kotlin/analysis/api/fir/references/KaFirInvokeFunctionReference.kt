/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.references

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.resolution.KaSimpleFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.calls
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.idea.references.KtInvokeFunctionReference
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtImportAlias

internal class KaFirInvokeFunctionReference(expression: KtCallExpression) : KtInvokeFunctionReference(expression), KaFirReference {
    override fun KaFirSession.computeSymbols(): Collection<KaSymbol> {
        return expression.resolveToCall()?.calls.orEmpty().mapNotNull { call ->
            (call as? KaSimpleFunctionCall)
                ?.takeIf { it.isImplicitInvoke }
                ?.partiallyAppliedSymbol
                ?.symbol
                ?.takeUnless { it is KaNamedFunctionSymbol && it.isBuiltinFunctionInvoke }
        }
    }

    override fun isReferenceToImportAlias(alias: KtImportAlias): Boolean {
        return super<KaFirReference>.isReferenceToImportAlias(alias)
    }
}