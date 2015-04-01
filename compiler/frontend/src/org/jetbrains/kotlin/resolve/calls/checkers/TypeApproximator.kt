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

package org.jetbrains.kotlin.resolve.calls.checkers

import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext

import org.jetbrains.kotlin.types.TypeUtils.noExpectedType
import org.jetbrains.kotlin.types.getApproximationTo
import org.jetbrains.kotlin.types.Approximation
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.calls.context.CallResolutionContext

public class TypeApproximator : AdditionalTypeChecker {
    override fun checkType(expression: JetExpression, expressionType: JetType, c: ResolutionContext<*>) {
        if (noExpectedType(c.expectedType)) return

        val approximationInfo = expressionType.getApproximationTo(
                c.expectedType,
                object : Approximation.DataFlowExtras {
                    override val canBeNull: Boolean
                        get() = c.dataFlowInfo.getNullability(dataFlowValue).canBeNull()
                    override val possibleTypes: Set<JetType>
                        get() = c.dataFlowInfo.getPossibleTypes(dataFlowValue)
                    override val presentableText: String
                        get() = StringUtil.trimMiddle(expression.getText(), 50)

                    private val dataFlowValue = DataFlowValueFactory.createDataFlowValue(expression, expressionType, c)
                }
        )
        if (approximationInfo != null) {
            c.trace.record(BindingContext.EXPRESSION_RESULT_APPROXIMATION, expression, approximationInfo)
        }
    }

    override fun checkReceiver(
            receiverParameter: ReceiverParameterDescriptor,
            receiverArgument: ReceiverValue,
            safeAccess: Boolean,
            c: CallResolutionContext<*>
    ) { }
}