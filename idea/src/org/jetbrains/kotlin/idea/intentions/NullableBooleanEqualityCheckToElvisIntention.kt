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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isBooleanOrNullableBoolean

class NullableBooleanEqualityCheckToElvisIntention : SelfTargetingIntention<KtBinaryExpression>(
        KtBinaryExpression::class.java, "Convert Boolean? == const to elvis"
) {
    override fun isApplicableTo(element: KtBinaryExpression, caretOffset: Int): Boolean {
        if (element.operationToken != KtTokens.EQEQ && element.operationToken != KtTokens.EXCLEQ) return false
        val lhs = element.left ?: return false
        val rhs = element.right ?: return false
        return isApplicable(lhs, rhs) || isApplicable(rhs, lhs)
    }

    private fun isApplicable(lhs: KtExpression, rhs: KtExpression): Boolean {
        if (!KtPsiUtil.isBooleanConstant(rhs)) return false

        val lhsType = lhs.analyze(BodyResolveMode.PARTIAL).getType(lhs) ?: return false
        return TypeUtils.isNullableType(lhsType) && lhsType.isBooleanOrNullableBoolean()
    }

    override fun applyTo(element: KtBinaryExpression, editor: Editor?) {
        val equality = element.operationToken == KtTokens.EQEQ
        val constPart = element.left as? KtConstantExpression ?:
                        element.right as? KtConstantExpression ?: return
        val exprPart = (if (element.right == constPart) element.left else element.right) ?: return
        val constValue = when {
            KtPsiUtil.isTrueConstant(constPart) -> true
            KtPsiUtil.isFalseConstant(constPart) -> false
            else -> return
        }

        val factory = KtPsiFactory(constPart)
        val elvis = factory.createExpressionByPattern("$0 ?: ${!constValue}", exprPart)
        element.replaced(if (constValue == equality) elvis else factory.createExpressionByPattern("!($0)", elvis))
    }
}