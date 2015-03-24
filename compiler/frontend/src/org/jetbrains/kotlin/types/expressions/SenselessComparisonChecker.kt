/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types.expressions

import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.resolve.calls.smartcasts.Nullability
import org.jetbrains.kotlin.diagnostics.Errors
import kotlin.platform.platformStatic
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext

object SenselessComparisonChecker {
    platformStatic fun checkSenselessComparisonWithNull(
            expression: JetBinaryExpression,
            left: JetExpression,
            right: JetExpression,
            context: ResolutionContext<*>,
            getType: (JetExpression) -> JetType?,
            getNullability: (DataFlowValue) -> Nullability
    ) {
        val expr =
                if (JetPsiUtil.isNullConstant(left)) right
                else if (JetPsiUtil.isNullConstant(right)) left
                else return

        val type = getType(expr)
        if (type == null || type.isError()) return

        val operationSign = expression.getOperationReference()
        val value = DataFlowValueFactory.createDataFlowValue(expr, type, context)

        val equality = operationSign.getReferencedNameElementType() == JetTokens.EQEQ || operationSign.getReferencedNameElementType() == JetTokens.EQEQEQ
        val nullability = getNullability(value)

        val expressionIsAlways =
                if (nullability == Nullability.NULL) equality
                else if (nullability == Nullability.NOT_NULL) !equality
                else if (nullability == Nullability.IMPOSSIBLE) false
                else return

        context.trace.report(Errors.SENSELESS_COMPARISON.on(expression, expression, expressionIsAlways))
    }
}