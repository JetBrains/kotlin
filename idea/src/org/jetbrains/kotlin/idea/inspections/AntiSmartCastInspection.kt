/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.quickfix.AddExclExclCallFix
import org.jetbrains.kotlin.idea.quickfix.CastExpressionFix
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.referenceExpressionVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.smartcasts.MultipleSmartCasts
import org.jetbrains.kotlin.types.checker.StrictEqualityTypeChecker
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

class AntiSmartCastInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return referenceExpressionVisitor(fun(refExpr: KtReferenceExpression) {
            val context = refExpr.analyze()
            val smartCast = context.get(BindingContext.SMARTCAST, refExpr) ?: return
            val expressionType = refExpr.getResolvedCall(context)?.resultingDescriptor?.returnType
            val defaultType = smartCast.defaultType
            if (defaultType != null) {
                val fix = when {
                    expressionType != null && expressionType!!.isNullable() && !defaultType!!.isNullable() &&
                            StrictEqualityTypeChecker.strictEqualTypes(expressionType!!.makeNotNullable().unwrap(), defaultType!!.unwrap()) -> {
                        IntentionWrapper(AddExclExclCallFix(refExpr), refExpr.containingKtFile)
                    }
                    else -> {
                        IntentionWrapper(CastExpressionFix(refExpr, defaultType!!), refExpr.containingKtFile)
                    }

                }
                holder.registerProblem(refExpr, "This is a hated smart-cast", fix)
            } else if (smartCast is MultipleSmartCasts) {
                // ???
            }
        })
    }
}