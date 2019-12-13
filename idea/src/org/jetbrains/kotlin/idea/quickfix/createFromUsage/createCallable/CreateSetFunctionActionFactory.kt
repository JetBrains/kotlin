/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.inspections.isReadOnlyCollectionOrMap
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.FunctionInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.ParameterInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.*

object CreateSetFunctionActionFactory : CreateGetSetFunctionActionFactory(isGet = false) {
    override fun createCallableInfo(element: KtArrayAccessExpression, diagnostic: Diagnostic): CallableInfo? {
        val arrayExpr = element.arrayExpression ?: return null
        val arrayType = TypeInfo(arrayExpr, Variance.IN_VARIANCE)

        val builtIns = element.builtIns

        val parameters = element.indexExpressions.mapTo(ArrayList<ParameterInfo>()) {
            ParameterInfo(TypeInfo(it, Variance.IN_VARIANCE))
        }
        val assignmentExpr = QuickFixUtil.getParentElementOfType(diagnostic, KtOperationExpression::class.java) ?: return null
        if (arrayExpr.getType(arrayExpr.analyze(BodyResolveMode.PARTIAL))?.isReadOnlyCollectionOrMap(builtIns) == true) return null
        val valType = when (assignmentExpr) {
            is KtBinaryExpression -> {
                TypeInfo(assignmentExpr.right ?: return null, Variance.IN_VARIANCE)
            }
            is KtUnaryExpression -> {
                if (assignmentExpr.operationToken !in OperatorConventions.INCREMENT_OPERATIONS) return null

                val rhsType = assignmentExpr.resolveToCall()?.resultingDescriptor?.returnType
                TypeInfo(if (rhsType == null || ErrorUtils.containsErrorType(rhsType)) builtIns.anyType else rhsType, Variance.IN_VARIANCE)
            }
            else -> return null
        }
        parameters.add(ParameterInfo(valType, "value"))

        val returnType = TypeInfo(builtIns.unitType, Variance.OUT_VARIANCE)
        return FunctionInfo(
            OperatorNameConventions.SET.asString(),
            arrayType,
            returnType,
            Collections.emptyList(),
            parameters,
            modifierList = KtPsiFactory(element).createModifierList(KtTokens.OPERATOR_KEYWORD)
        )
    }
}
