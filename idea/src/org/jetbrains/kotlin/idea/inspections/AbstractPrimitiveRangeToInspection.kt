/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.getCallableDescriptor
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

abstract class AbstractPrimitiveRangeToInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitExpression(expression: KtExpression) {
                super.visitExpression(expression)

                if (expression !is KtBinaryExpression && expression !is KtDotQualifiedExpression) return

                val fqName = expression.getCallableDescriptor()?.fqNameUnsafe?.asString() ?: return
                if (!fqName.matches(REGEX_RANGE_TO)) return

                visitRangeToExpression(expression, holder)
            }
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