/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.VariableAsFunctionLikeCallInfo
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression

class KtFirInvokeFunctionReference(expression: KtCallExpression) : KtInvokeFunctionReference(expression), KtFirReference {
    override fun doRenameImplicitConventionalCall(newName: String?): KtExpression {
        TODO("Not yet implemented")
    }

    override fun KtAnalysisSession.resolveToSymbols(): Collection<KtSymbol> {
        val call = expression.resolveCall() ?: return emptyList()
        if (call is VariableAsFunctionLikeCallInfo) {
            return listOf(call.invokeFunction)
        }
        return emptyList()
    }
}