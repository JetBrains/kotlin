/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.FunctionInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.ParameterInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions

object CreateGetFunctionActionFactory : CreateGetSetFunctionActionFactory(isGet = true) {
    override fun createCallableInfo(element: KtArrayAccessExpression, diagnostic: Diagnostic): CallableInfo? {
        val arrayExpr = element.arrayExpression ?: return null

        val arrayType = TypeInfo(arrayExpr, Variance.IN_VARIANCE)
        val parameters = element.indexExpressions.map { ParameterInfo(TypeInfo(it, Variance.IN_VARIANCE)) }
        val returnType = TypeInfo(element, Variance.OUT_VARIANCE)
        return FunctionInfo(
            OperatorNameConventions.GET.asString(),
            arrayType,
            returnType,
            emptyList(),
            parameters,
            modifierList = KtPsiFactory(element).createModifierList(KtTokens.OPERATOR_KEYWORD)
        )
    }
}
