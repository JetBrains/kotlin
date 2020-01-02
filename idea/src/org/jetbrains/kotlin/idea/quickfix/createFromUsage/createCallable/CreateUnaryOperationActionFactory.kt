/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.FunctionInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.expressions.OperatorConventions

object CreateUnaryOperationActionFactory : CreateCallableMemberFromUsageFactory<KtUnaryExpression>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtUnaryExpression? {
        return diagnostic.psiElement.parent as? KtUnaryExpression
    }

    override fun createCallableInfo(element: KtUnaryExpression, diagnostic: Diagnostic): CallableInfo? {
        val token = element.operationToken as KtToken
        val operationName = OperatorConventions.getNameForOperationSymbol(token, true, false) ?: return null
        val incDec = token in OperatorConventions.INCREMENT_OPERATIONS

        val receiverExpr = element.baseExpression ?: return null

        val receiverType = TypeInfo(receiverExpr, Variance.IN_VARIANCE)
        val returnType = if (incDec) TypeInfo.ByReceiverType(Variance.OUT_VARIANCE) else TypeInfo(element, Variance.OUT_VARIANCE)
        return FunctionInfo(
            operationName.asString(),
            receiverType,
            returnType,
            modifierList = KtPsiFactory(element).createModifierList(KtTokens.OPERATOR_KEYWORD)
        )
    }
}
