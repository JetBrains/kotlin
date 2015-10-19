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

package org.jetbrains.kotlin.jvm

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.jvm.bindingContextSlices.RUNTIME_ASSERTION_INFO
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.context.CallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KtType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.upperIfFlexible

public class RuntimeAssertionInfo(public val needNotNullAssertion: Boolean, public val message: String) {
    public interface DataFlowExtras {
        class OnlyMessage(message: String) : DataFlowExtras {
            override val canBeNull: Boolean get() = true
            override val possibleTypes: Set<KtType> get() = setOf()
            override val presentableText: String = message
        }

        val canBeNull: Boolean
        val possibleTypes: Set<KtType>
        val presentableText: String
    }

    companion object {
        @JvmStatic
        public fun create(
                expectedType: KtType,
                expressionType: KtType,
                dataFlowExtras: DataFlowExtras
        ): RuntimeAssertionInfo? {
            fun assertNotNull(): Boolean {
                if (expectedType.isError() || expressionType.isError()) return false

                // T : Any, T! = T..T?
                // Let T$ will be copy of T! with enhanced nullability.
                // Cases when nullability assertion needed: T! -> T, T$ -> T

                // Expected type either T?, T! or T$
                if (TypeUtils.isNullableType(expectedType) || expectedType.hasEnhancedNullability()) return false

                // Expression type is not nullable and not enhanced (neither T?, T! or T$)
                val isExpressionTypeNullable = TypeUtils.isNullableType(expressionType)
                if (!isExpressionTypeNullable && !expressionType.hasEnhancedNullability()) return false

                // Smart-cast T! or T?
                if (!dataFlowExtras.canBeNull && isExpressionTypeNullable) return false

                return true
            }

            return if (assertNotNull())
                RuntimeAssertionInfo(needNotNullAssertion = true, message = dataFlowExtras.presentableText)
            else
                null
        }

        private fun KtType.hasEnhancedNullability()
                = getAnnotations().findAnnotation(JvmAnnotationNames.ENHANCED_NULLABILITY_ANNOTATION) != null
    }
}

public object RuntimeAssertionsTypeChecker : AdditionalTypeChecker {
    override fun checkType(expression: KtExpression, expressionType: KtType, expressionTypeWithSmartCast: KtType, c: ResolutionContext<*>) {
        if (TypeUtils.noExpectedType(c.expectedType)) return

        val assertionInfo = RuntimeAssertionInfo.create(
                c.expectedType,
                expressionType,
                object : RuntimeAssertionInfo.DataFlowExtras {
                    override val canBeNull: Boolean
                        get() = c.dataFlowInfo.getNullability(dataFlowValue).canBeNull()
                    override val possibleTypes: Set<KtType>
                        get() = c.dataFlowInfo.getPossibleTypes(dataFlowValue)
                    override val presentableText: String
                        get() = StringUtil.trimMiddle(expression.getText(), 50)

                    private val dataFlowValue = DataFlowValueFactory.createDataFlowValue(expression, expressionType, c)
                }
        )

        if (assertionInfo != null) {
            c.trace.record(RUNTIME_ASSERTION_INFO, expression, assertionInfo)
        }
    }

    override fun checkReceiver(
            receiverParameter: ReceiverParameterDescriptor,
            receiverArgument: ReceiverValue,
            safeAccess: Boolean,
            c: CallResolutionContext<*>
    ) { }
}
