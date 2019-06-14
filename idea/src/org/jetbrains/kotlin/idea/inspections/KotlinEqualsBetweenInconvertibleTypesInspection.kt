/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.callExpressionVisitor
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isEnum
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.util.OperatorNameConventions

class KotlinEqualsBetweenInconvertibleTypesInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = callExpressionVisitor(fun(call) {
        val callee = call.calleeExpression as? KtSimpleNameExpression ?: return
        val identifier = callee.getReferencedNameAsName()
        if (identifier != OperatorNameConventions.EQUALS) return
        val receiver = call.getQualifiedExpressionForSelector()?.receiverExpression ?: return
        val argument = call.valueArguments.singleOrNull()?.getArgumentExpression() ?: return
        if (call.analyze(BodyResolveMode.PARTIAL).isInconvertibleTypes(receiver, argument)) {
            holder.registerProblem(callee, "'equals()' between objects of inconvertible types")
        }
    })

    companion object {
        fun BindingContext.isInconvertibleTypes(expr1: KtExpression?, expr2: KtExpression?): Boolean {
            val type1 = expr1?.getTargetType(this) ?: return false
            val type2 = expr2?.getTargetType(this) ?: return false
            return type1 != type2
        }

        private fun KtExpression.getTargetType(context: BindingContext): KotlinType? {
            return context.getType(this)?.takeIf {
                KotlinBuiltIns.isPrimitiveTypeOrNullablePrimitiveType(it) || KotlinBuiltIns.isStringOrNullableString(it) || it.isEnum()
            }?.makeNotNullable()
        }
    }
}
