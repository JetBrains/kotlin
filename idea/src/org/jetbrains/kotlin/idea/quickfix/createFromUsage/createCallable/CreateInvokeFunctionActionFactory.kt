/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.FunctionInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.ParameterInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.util.OperatorNameConventions

object CreateInvokeFunctionActionFactory : CreateCallableMemberFromUsageFactory<KtCallExpression>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtCallExpression? {
        return diagnostic.psiElement.parent as? KtCallExpression
    }

    override fun createCallableInfo(element: KtCallExpression, diagnostic: Diagnostic): CallableInfo? {
        val expectedType = Errors.FUNCTION_EXPECTED.cast(diagnostic).b
        if (expectedType.isError) return null

        val receiverType = TypeInfo(expectedType, Variance.IN_VARIANCE)

        val anyType = element.builtIns.nullableAnyType
        val parameters = element.valueArguments.map {
            ParameterInfo(
                it.getArgumentExpression()?.let { expression -> TypeInfo(expression, Variance.IN_VARIANCE) } ?: TypeInfo(
                    anyType,
                    Variance.IN_VARIANCE
                ),
                it.getArgumentName()?.referenceExpression?.getReferencedName()
            )
        }

        val returnType = TypeInfo(element, Variance.OUT_VARIANCE)
        return FunctionInfo(
            OperatorNameConventions.INVOKE.asString(),
            receiverType,
            returnType,
            parameterInfos = parameters,
            modifierList = KtPsiFactory(element).createModifierList(KtTokens.OPERATOR_KEYWORD)
        )
    }
}
