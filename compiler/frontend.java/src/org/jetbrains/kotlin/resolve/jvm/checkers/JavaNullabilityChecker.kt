/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.UpperBoundChecker
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.Nullability
import org.jetbrains.kotlin.resolve.calls.tower.NewResolvedCallImpl
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.*
import org.jetbrains.kotlin.types.expressions.SenselessComparisonChecker
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

class JavaNullabilityChecker(val upperBoundChecker: UpperBoundChecker) : AdditionalTypeChecker {
    override fun checkType(
        expression: KtExpression,
        expressionType: KotlinType,
        expressionTypeWithSmartCast: KotlinType,
        c: ResolutionContext<*>
    ) {
        checkTypeParameterBounds(expression, expressionType, c)

        val dataFlowValue by lazy(LazyThreadSafetyMode.NONE) {
            c.dataFlowValueFactory.createDataFlowValue(expression, expressionType, c)
        }

        if (isWrongTypeParameterNullabilityForSubtyping(expressionType, c) { dataFlowValue }) {
            c.trace.report(ErrorsJvm.NULLABLE_TYPE_PARAMETER_AGAINST_NOT_NULL_TYPE_PARAMETER.on(expression, expressionType))
        }
        doCheckType(
            expressionType,
            c.expectedType,
            { dataFlowValue },
            c.dataFlowInfo
        ) { expectedType, actualType ->
            c.trace.report(ErrorsJvm.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS.on(expression, expectedType, actualType))
        }

        when (expression) {
            is KtWhenExpression ->
                if (expression.elseExpression == null) {
                    // Check for conditionally-exhaustive when on platform enums, see KT-6399
                    val subjectExpression = expression.subjectExpression ?: return
                    val type = c.trace.getType(subjectExpression) ?: return
                    if (type.isFlexible() && TypeUtils.isNullableType(type.asFlexibleType().upperBound)) {
                        val enumClassDescriptor = WhenChecker.getClassDescriptorOfTypeIfEnum(type) ?: return
                        val context = c.trace.bindingContext
                        if (WhenChecker.getEnumMissingCases(expression, context, enumClassDescriptor).isEmpty()
                            && !WhenChecker.containsNullCase(expression, context)
                        ) {
                            val subjectDataFlowValue = c.dataFlowValueFactory.createDataFlowValue(subjectExpression, type, c)
                            val dataFlowInfo = c.trace[BindingContext.EXPRESSION_TYPE_INFO, subjectExpression]?.dataFlowInfo
                            if (dataFlowInfo != null && !dataFlowInfo.getStableNullability(subjectDataFlowValue).canBeNull()) {
                                return
                            }

                            c.trace.report(ErrorsJvm.WHEN_ENUM_CAN_BE_NULL_IN_JAVA.on(expression.subjectExpression!!))
                        }
                    }
                }
            is KtPostfixExpression ->
                if (expression.operationToken == KtTokens.EXCLEXCL) {
                    val baseExpression = expression.baseExpression ?: return
                    val baseExpressionType = c.trace.getType(baseExpression) ?: return
                    doIfNotNull(
                        baseExpressionType,
                        { c.dataFlowValueFactory.createDataFlowValue(baseExpression, baseExpressionType, c) },
                        c
                    ) {
                        c.trace.report(Errors.UNNECESSARY_NOT_NULL_ASSERTION.on(expression.operationReference, baseExpressionType))
                    }
                }
            is KtBinaryExpression ->
                when (expression.operationToken) {
                    KtTokens.EQEQ,
                    KtTokens.EXCLEQ,
                    KtTokens.EQEQEQ,
                    KtTokens.EXCLEQEQEQ -> {
                        if (expression.left != null && expression.right != null) {
                            SenselessComparisonChecker.checkSenselessComparisonWithNull(
                                expression, expression.left!!, expression.right!!, c,
                                { c.trace.getType(it) },
                                { value ->
                                    doIfNotNull(value.type, { value }, c) { Nullability.NOT_NULL } ?: Nullability.UNKNOWN
                                }
                            )
                        }
                    }
                }
        }
    }

    private fun checkTypeParameterBounds(
        expression: KtExpression,
        expressionType: KotlinType,
        c: ResolutionContext<*>
    ) {
        if (expressionType is AbbreviatedType) {
            upperBoundChecker.checkBoundsOfExpandedTypeAlias(expressionType.expandedType, expression, c.trace)
        }

        if (upperBoundChecker !is WarningAwareUpperBoundChecker) return

        val call = (c as? BasicCallResolutionContext)?.call
            ?: c.trace.bindingContext[BindingContext.CALL, (expression as? KtCallExpression)?.calleeExpression]
            ?: return
        val resolvedCall = c.trace.bindingContext[BindingContext.RESOLVED_CALL, call] ?: return

        val typeArguments = if (resolvedCall is NewResolvedCallImpl<*>) {
            resolvedCall.resolvedCallAtom.typeArgumentMappingByOriginal
        } else {
            resolvedCall.typeArguments.entries
        }

        for ((typeParameter, typeArgument) in typeArguments) {
            // continue if we don't have explicit type arguments
            val typeReference = call.typeArguments.getOrNull(typeParameter.index)?.typeReference ?: continue

            if (typeArgument == null) continue

            upperBoundChecker.checkBounds(
                typeReference, typeArgument, typeParameter, TypeSubstitutor.create(typeArgument), c.trace, withOnlyCheckForWarning = true
            )
        }
    }

    private fun isWrongTypeParameterNullabilityForSubtyping(
        expressionType: KotlinType,
        c: ResolutionContext<*>,
        dataFlowValueForWholeExpression: () -> DataFlowValue
    ): Boolean {
        if (c.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated)) return false
        if (TypeUtils.noExpectedType(c.expectedType)) return false

        var metWrongNullabilityInsideArguments = false

        @OptIn(ClassicTypeCheckerStateInternals::class)
        val typeState: TypeCheckerState = object : ClassicTypeCheckerState(isErrorTypeEqualsToAnything = true) {
            private var expectsTypeArgument = false
            override fun customIsSubtypeOf(subType: KotlinTypeMarker, superType: KotlinTypeMarker): Boolean {

                if (isNullableTypeAgainstNotNullTypeParameter(subType as KotlinType, superType as KotlinType)) {
                    // data flow value is only checked for top-level types
                    if (expectsTypeArgument || c.dataFlowInfo.getStableNullability(dataFlowValueForWholeExpression()) != Nullability.NOT_NULL) {
                        metWrongNullabilityInsideArguments = true
                        return false
                    }
                }

                if (!expectsTypeArgument) {
                    expectsTypeArgument = true
                }
                return true
            }
        }

        AbstractTypeChecker.isSubtypeOf(typeState, expressionType, c.expectedType)

        return metWrongNullabilityInsideArguments
    }

    private fun isNullableTypeAgainstNotNullTypeParameter(
        subType: KotlinType,
        superType: KotlinType
    ): Boolean {
        if (superType !is NotNullTypeVariable) return false
        return !AbstractNullabilityChecker.isSubtypeOfAny(
            createClassicTypeCheckerState(isErrorTypeEqualsToAnything = true),
            subType
        )
    }

    override fun checkReceiver(
        receiverParameter: ReceiverParameterDescriptor,
        receiverArgument: ReceiverValue,
        safeAccess: Boolean,
        c: CallResolutionContext<*>
    ) {
        val dataFlowValue by lazy(LazyThreadSafetyMode.NONE) {
            c.dataFlowValueFactory.createDataFlowValue(receiverArgument, c)
        }

        if (safeAccess) {
            val safeAccessElement = c.call.callOperationNode?.psi ?: return
            doIfNotNull(receiverArgument.type, { dataFlowValue }, c) {
                c.trace.report(Errors.UNNECESSARY_SAFE_CALL.on(safeAccessElement, receiverArgument.type))
            }

            return
        }

        doCheckType(
            receiverArgument.type,
            receiverParameter.type,
            { dataFlowValue },
            c.dataFlowInfo
        ) { expectedType, actualType ->
            val receiverExpression = (receiverArgument as? ExpressionReceiver)?.expression
            if (receiverExpression != null) {
                c.trace.report(ErrorsJvm.RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS.on(receiverExpression, actualType))
            } else {
                val reportOn = c.call.calleeExpression ?: c.call.callElement
                c.trace.report(ErrorsJvm.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS.on(reportOn, expectedType, actualType))
            }
        }
    }

    private fun doCheckType(
        expressionType: KotlinType,
        expectedType: KotlinType,
        expressionTypeDataFlowValue: () -> DataFlowValue,
        dataFlowInfo: DataFlowInfo,
        reportWarning: (expectedType: KotlinType, actualType: KotlinType) -> Unit
    ) {
        if (TypeUtils.noExpectedType(expectedType)) return

        @Suppress("NAME_SHADOWING")
        val expressionType = exactedExpressionTypeByDataFlowNullability(expressionType, expressionTypeDataFlowValue, dataFlowInfo)

        val isEnhancedExpectedTypeSubtypeOfExpressionType = typeCheckerForEnhancedTypes.isSubtypeOf(expressionType, expectedType)

        if (isEnhancedExpectedTypeSubtypeOfExpressionType) return

        val isExpectedTypeSubtypeOfExpressionType = typeCheckerForBaseTypes.isSubtypeOf(expressionType, expectedType)

        if (!isEnhancedExpectedTypeSubtypeOfExpressionType && isExpectedTypeSubtypeOfExpressionType) {
            reportWarning(expectedType.unwrapEnhancementDeeply(), expressionType.unwrapEnhancementDeeply())
        }
    }

    private fun exactedExpressionTypeByDataFlowNullability(
        expressionType: KotlinType,
        expressionTypeDataFlowValue: () -> DataFlowValue,
        dataFlowInfo: DataFlowInfo,
    ): KotlinType {
        val isNotNullByDataFlowInfo = dataFlowInfo.getStableNullability(expressionTypeDataFlowValue()) == Nullability.NOT_NULL
        return if (expressionType.isNullable() && isNotNullByDataFlowInfo) expressionType.makeNotNullable() else expressionType
    }

    private fun <T : Any> doIfNotNull(
        type: KotlinType,
        dataFlowValue: () -> DataFlowValue,
        c: ResolutionContext<*>,
        body: () -> T
    ) = if (type.mustNotBeNull()?.isFromJava == true && c.dataFlowInfo.getStableNullability(dataFlowValue()).canBeNull()) {
        body()
    } else {
        null
    }

    companion object {
        val typeCheckerForEnhancedTypes = NewKotlinTypeCheckerImpl(
            kotlinTypeRefiner = KotlinTypeRefiner.Default,
            kotlinTypePreparator = object : KotlinTypePreparator() {
                override fun prepareType(type: KotlinTypeMarker): UnwrappedType =
                    super.prepareType(type).let { it.getEnhancementDeeply() ?: it }.unwrap()
            }
        )
        val typeCheckerForBaseTypes = NewKotlinTypeCheckerImpl(KotlinTypeRefiner.Default)
    }
}

class EnhancedNullabilityInfo(val enhancedType: KotlinType, val isFromJava: Boolean) {
    val isFromKotlin get() = !isFromJava
}

private fun KotlinType.enhancementFromKotlin() = EnhancedNullabilityInfo(this, isFromJava = false)
private fun TypeWithEnhancement.enhancementFromJava() = EnhancedNullabilityInfo(enhancement, isFromJava = true)

fun KotlinType.mustNotBeNull(): EnhancedNullabilityInfo? = when {
    !isError && !isFlexible() && !TypeUtils.acceptsNullable(this) -> enhancementFromKotlin()
    isFlexible() && !TypeUtils.acceptsNullable(asFlexibleType().upperBound) -> enhancementFromKotlin()
    this is TypeWithEnhancement && enhancement.mustNotBeNull() != null -> enhancementFromJava()
    else -> null
}
