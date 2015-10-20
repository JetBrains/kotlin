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

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.cfg.WhenChecker
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.lazy.types.isMarkedNotNull
import org.jetbrains.kotlin.load.java.lazy.types.isMarkedNullable
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.context.CallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.Nullability
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KtType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.SenselessComparisonChecker
import org.jetbrains.kotlin.types.flexibility
import org.jetbrains.kotlin.types.isFlexible

public class JavaNullabilityWarningsChecker : AdditionalTypeChecker {
    private fun KtType.mayBeNull(): ErrorsJvm.NullabilityInformationSource? {
        if (!isError() && !isFlexible() && TypeUtils.isNullableType(this)) return ErrorsJvm.NullabilityInformationSource.KOTLIN

        if (isFlexible() && TypeUtils.isNullableType(flexibility().lowerBound)) return ErrorsJvm.NullabilityInformationSource.KOTLIN

        if (getAnnotations().isMarkedNullable()) return ErrorsJvm.NullabilityInformationSource.JAVA
        return null
    }

    private fun KtType.mustNotBeNull(): ErrorsJvm.NullabilityInformationSource? {
        if (!isError() && !isFlexible() && !TypeUtils.isNullableType(this)) return ErrorsJvm.NullabilityInformationSource.KOTLIN

        if (isFlexible() && !TypeUtils.isNullableType(flexibility().upperBound)) return ErrorsJvm.NullabilityInformationSource.KOTLIN

        if (!isMarkedNullable() && getAnnotations().isMarkedNotNull()) return ErrorsJvm.NullabilityInformationSource.JAVA
        return null
    }

    private fun doCheckType(
            expressionType: KtType,
            expectedType: KtType,
            dataFlowValue: DataFlowValue,
            dataFlowInfo: DataFlowInfo,
            reportWarning: (expectedMustNotBeNull: ErrorsJvm.NullabilityInformationSource, actualMayBeNull: ErrorsJvm.NullabilityInformationSource) -> Unit
    ) {
        if (TypeUtils.noExpectedType(expectedType)) return

        val expectedMustNotBeNull = expectedType.mustNotBeNull()
        if (dataFlowInfo.getNullability(dataFlowValue) == Nullability.NOT_NULL) return

        val actualMayBeNull = expressionType.mayBeNull()

        if (expectedMustNotBeNull == ErrorsJvm.NullabilityInformationSource.KOTLIN && actualMayBeNull == ErrorsJvm.NullabilityInformationSource.KOTLIN) {
            // a type mismatch error will be reported elsewhere
            return;
        }

        if (expectedMustNotBeNull != null && actualMayBeNull != null) {
            reportWarning(expectedMustNotBeNull, actualMayBeNull)
        }
    }

    override fun checkType(expression: KtExpression, expressionType: KtType, expressionTypeWithSmartCast: KtType, c: ResolutionContext<*>) {
        doCheckType(
                expressionType,
                c.expectedType,
                DataFlowValueFactory.createDataFlowValue(expression, expressionType, c),
                c.dataFlowInfo
        ) {
            expectedMustNotBeNull,
            actualMayBeNull ->
            c.trace.report(ErrorsJvm.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS.on(expression, expectedMustNotBeNull, actualMayBeNull))
        }

        when (expression) {
            is KtWhenExpression ->
                    if (expression.getElseExpression() == null) {
                        // Check for conditionally-exhaustive when on platform enums, see KT-6399
                        val type = expression.getSubjectExpression()?.let { c.trace.getType(it) } ?: return
                        if (type.isFlexible() && TypeUtils.isNullableType(type.flexibility().upperBound) && !type.getAnnotations().isMarkedNotNull()) {
                            val enumClassDescriptor = WhenChecker.getClassDescriptorOfTypeIfEnum(type) ?: return

                            if (WhenChecker.isWhenOnEnumExhaustive(expression, c.trace, enumClassDescriptor)
                                && !WhenChecker.containsNullCase(expression, c.trace)) {

                                c.trace.report(ErrorsJvm.WHEN_ENUM_CAN_BE_NULL_IN_JAVA.on(expression.getSubjectExpression()))
                            }
                        }
                    }
            is KtPostfixExpression ->
                    if (expression.getOperationToken() == KtTokens.EXCLEXCL) {
                        val baseExpression = expression.getBaseExpression() ?: return
                        val baseExpressionType = c.trace.getType(baseExpression) ?: return
                        doIfNotNull(
                                DataFlowValueFactory.createDataFlowValue(baseExpression, baseExpressionType, c),
                                c
                        ) {
                            c.trace.report(Errors.UNNECESSARY_NOT_NULL_ASSERTION.on(expression.getOperationReference(), baseExpressionType))
                        }
                    }
            is KtBinaryExpression ->
                when (expression.getOperationToken()) {
                    KtTokens.ELVIS -> {
                        val baseExpression = expression.getLeft()
                        val baseExpressionType = baseExpression?.let{ c.trace.getType(it) } ?: return
                        doIfNotNull(
                                DataFlowValueFactory.createDataFlowValue(baseExpression!!, baseExpressionType, c),
                                c
                        ) {
                            c.trace.report(Errors.USELESS_ELVIS.on(expression, baseExpressionType))
                        }
                    }
                    KtTokens.EQEQ,
                    KtTokens.EXCLEQ,
                    KtTokens.EQEQEQ,
                    KtTokens.EXCLEQEQEQ -> {
                        if (expression.getLeft() != null && expression.getRight() != null) {
                            SenselessComparisonChecker.checkSenselessComparisonWithNull(
                                    expression, expression.getLeft()!!, expression.getRight()!!, c,
                                    { c.trace.getType(it) },
                                    {
                                        value ->
                                        doIfNotNull(value, c) { Nullability.NOT_NULL } ?: Nullability.UNKNOWN
                                    }
                            )
                        }
                    }
                }
        }
    }

    private fun <T: Any> doIfNotNull(dataFlowValue: DataFlowValue, c: ResolutionContext<*>, body: () -> T): T? {
        if (c.dataFlowInfo.getNullability(dataFlowValue).canBeNull()
            && dataFlowValue.type.mustNotBeNull() == ErrorsJvm.NullabilityInformationSource.JAVA) {
            return body()
        }
        return null
    }

    override fun checkReceiver(
            receiverParameter: ReceiverParameterDescriptor,
            receiverArgument: ReceiverValue,
            safeAccess: Boolean,
            c: CallResolutionContext<*>
    ) {
        val dataFlowValue = DataFlowValueFactory.createDataFlowValue(receiverArgument, c)
        if (!safeAccess) {
            doCheckType(
                    receiverArgument.getType(),
                    receiverParameter.getType(),
                    dataFlowValue,
                    c.dataFlowInfo
            ) {
                expectedMustNotBeNull,
                actualMayBeNull ->
                val reportOn =
                        if (receiverArgument is ExpressionReceiver)
                            receiverArgument.getExpression()
                        else
                            c.call.getCalleeExpression() ?: c.call.getCallElement()

                c.trace.report(ErrorsJvm.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS.on(
                        reportOn, expectedMustNotBeNull, actualMayBeNull
                ))

            }
        }
        else {
            doIfNotNull(dataFlowValue, c) {
                c.trace.report(Errors.UNNECESSARY_SAFE_CALL.on(c.call.getCallOperationNode()!!.getPsi(), receiverArgument.getType()))
            }
        }
    }
}