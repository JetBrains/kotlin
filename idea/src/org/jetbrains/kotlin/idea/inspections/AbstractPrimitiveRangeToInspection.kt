/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.getCallableDescriptor
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.expressionVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

abstract class AbstractPrimitiveRangeToInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return expressionVisitor { expression ->
            if (expression !is KtBinaryExpression && expression !is KtDotQualifiedExpression) return@expressionVisitor

            val fqName = expression.getCallableDescriptor()?.fqNameUnsafe?.asString() ?: return@expressionVisitor
            if (!fqName.matches(REGEX_RANGE_TO)) return@expressionVisitor

            visitRangeToExpression(expression, holder)
        }
    }

    abstract fun visitRangeToExpression(expression: KtExpression, holder: ProblemsHolder)

    companion object {
        private val REGEX_RANGE_TO = """kotlin.(Char|Byte|Short|Int|Long).rangeTo""".toRegex()

        fun KtExpression.constantValueOrNull(context: BindingContext? = null): ConstantValue<Any?>? {
            val c = context ?: this.analyze(BodyResolveMode.PARTIAL)

            val constant = ConstantExpressionEvaluator.getConstant(this, c) ?: return null

            return constant.toConstantValue(getType(c) ?: return null)
        }
    }
}