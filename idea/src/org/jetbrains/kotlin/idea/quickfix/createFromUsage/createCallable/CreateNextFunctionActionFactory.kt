/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.FunctionInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions

object CreateNextFunctionActionFactory : CreateCallableMemberFromUsageFactory<KtForExpression>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtForExpression? {
        return QuickFixUtil.getParentElementOfType(diagnostic, KtForExpression::class.java)
    }

    override fun createCallableInfo(element: KtForExpression, diagnostic: Diagnostic): CallableInfo? {
        val diagnosticWithParameters = DiagnosticFactory.cast(diagnostic, Errors.NEXT_MISSING, Errors.NEXT_NONE_APPLICABLE)
        val ownerType = TypeInfo(diagnosticWithParameters.a, Variance.IN_VARIANCE)

        val variableExpr = element.loopParameter ?: element.destructuringDeclaration ?: return null
        val returnType = TypeInfo(variableExpr as KtExpression, Variance.OUT_VARIANCE)
        return FunctionInfo(
            OperatorNameConventions.NEXT.asString(),
            ownerType,
            returnType,
            modifierList = KtPsiFactory(element).createModifierList(KtTokens.OPERATOR_KEYWORD)
        )
    }
}
