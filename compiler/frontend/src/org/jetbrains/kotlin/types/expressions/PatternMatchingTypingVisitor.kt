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

import com.google.common.collect.Maps
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.isBoolean
import org.jetbrains.kotlin.cfg.WhenChecker
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.TypeResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency.INDEPENDENT
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.newWritableScopeImpl
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.createTypeInfo
import org.jetbrains.kotlin.utils.addIfNotNull

class PatternMatchingTypingVisitor internal constructor(facade: ExpressionTypingInternals) : ExpressionTypingVisitor(facade) {

    override fun visitIsExpression(expression: KtIsExpression, contextWithExpectedType: ExpressionTypingContext): KotlinTypeInfo {
        val context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT)
        val leftHandSide = expression.leftHandSide
        val typeInfo = facade.safeGetTypeInfo(leftHandSide, context.replaceScope(context.scope))
        val knownType = typeInfo.type
        if (expression.typeReference != null && knownType != null) {
            val dataFlowValue = DataFlowValueFactory.createDataFlowValue(leftHandSide, knownType, context)
            val conditionInfo = checkTypeForIs(context, knownType, expression.typeReference, dataFlowValue).thenInfo
            val newDataFlowInfo = conditionInfo.and(typeInfo.dataFlowInfo)
            context.trace.record(BindingContext.DATAFLOW_INFO_AFTER_CONDITION, expression, newDataFlowInfo)
        }
        return components.dataFlowAnalyzer.checkType(typeInfo.replaceType(components.builtIns.booleanType), expression, contextWithExpectedType)
    }

    override fun visitWhenExpression(expression: KtWhenExpression, context: ExpressionTypingContext) =
            visitWhenExpression(expression, context, false)

    fun visitWhenExpression(expression: KtWhenExpression, contextWithExpectedType: ExpressionTypingContext, isStatement: Boolean): KotlinTypeInfo {
        WhenChecker.checkDeprecatedWhenSyntax(contextWithExpectedType.trace, expression)
        WhenChecker.checkReservedPrefix(contextWithExpectedType.trace, expression)

        components.dataFlowAnalyzer.recordExpectedType(contextWithExpectedType.trace, expression, contextWithExpectedType.expectedType)

        val contextBeforeSubject = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT)
        // TODO :change scope according to the bound value in the when header
        val subjectExpression = expression.subjectExpression

        val contextAfterSubject: ExpressionTypingContext
        val subjectType: KotlinType
        val subjectDataFlowValue: DataFlowValue
        val jumpOutPossible: Boolean

        if (subjectExpression == null) {
            subjectType = ErrorUtils.createErrorType("Unknown type")
            subjectDataFlowValue = DataFlowValue.nullValue(components.builtIns)
            contextAfterSubject = contextBeforeSubject
            jumpOutPossible = false
        }
        else {
            val subjectTypeInfo = facade.safeGetTypeInfo(subjectExpression, contextBeforeSubject)

            contextAfterSubject = contextBeforeSubject.replaceDataFlowInfo(subjectTypeInfo.dataFlowInfo)
            subjectType = subjectTypeInfo.type!!
            subjectDataFlowValue = DataFlowValueFactory.createDataFlowValue(subjectExpression, subjectType, contextAfterSubject)
            jumpOutPossible = subjectTypeInfo.jumpOutPossible

            if (TypeUtils.isNullableType(subjectType) && !WhenChecker.containsNullCase(expression, contextBeforeSubject.trace.bindingContext)) {
                val trace = TemporaryBindingTrace.create(contextBeforeSubject.trace, "Temporary trace for when subject nullability")
                val subjectContext = contextBeforeSubject.replaceExpectedType(TypeUtils.makeNotNullable(subjectType)).replaceBindingTrace(trace)
                val castResult = DataFlowAnalyzer.checkPossibleCast(
                        subjectType, KtPsiUtil.safeDeparenthesize(subjectExpression), subjectContext)
                if (castResult != null && castResult.isCorrect) {
                    trace.commit()
                }
            }
        }

        val dataFlowInfoBeforeEntryBodies = collectDataFlowInfoBeforeEntryBodies(expression, contextAfterSubject, subjectType, subjectDataFlowValue)
        val isExhaustive = WhenChecker.isWhenExhaustive(expression, contextAfterSubject.trace)

        return getTypeInfoForIncompleteWhen(expression, isStatement, isExhaustive, contextWithExpectedType,
                                            contextAfterSubject, jumpOutPossible, dataFlowInfoBeforeEntryBodies)
    }

    private fun collectDataFlowInfoBeforeEntryBodies(
            expression: KtWhenExpression,
            contextAfterSubject: ExpressionTypingContext,
            subjectType: KotlinType,
            subjectDataFlowValue: DataFlowValue
    ): Map<KtWhenEntry, DataFlowInfo> {
        val subjectExpression = expression.subjectExpression
        val thenInfo = Maps.newHashMapWithExpectedSize<KtWhenEntry, DataFlowInfo>(expression.entries.size)

        var inputDataFlowInfo = contextAfterSubject.dataFlowInfo
        for (whenEntry in expression.entries) {
            val conditionsInfo = analyzeWhenEntryConditions(whenEntry,
                                                            contextAfterSubject.replaceDataFlowInfo(inputDataFlowInfo),
                                                            subjectExpression, subjectType, subjectDataFlowValue)
            thenInfo[whenEntry] = conditionsInfo.thenInfo
            inputDataFlowInfo = inputDataFlowInfo.and(conditionsInfo.elseInfo)
        }

        return thenInfo
    }

    private fun getTypeInfoForIncompleteWhen(
            expression: KtWhenExpression,
            isStatement: Boolean,
            isExhaustive: Boolean,
            contextWithExpectedType: ExpressionTypingContext,
            contextAfterSubject: ExpressionTypingContext,
            jumpOutPossibleInSubject: Boolean,
            dataFlowBeforeEntryBody: Map<KtWhenEntry, DataFlowInfo>
    ): KotlinTypeInfo {
        val coercionStrategy = if (isStatement) CoercionStrategy.COERCION_TO_UNIT else CoercionStrategy.NO_COERCION

        val expressionTypes = hashSetOf<KotlinType>()
        var commonDataFlowInfo: DataFlowInfo? = null
        var jumpOutPossible = jumpOutPossibleInSubject
        val whenValue = DataFlowValueFactory.createDataFlowValue(expression, components.builtIns.nullableAnyType, contextAfterSubject)

        for (whenEntry in expression.entries) {
            val ifTrueInfo = dataFlowBeforeEntryBody[whenEntry]!!

            val bodyExpression = whenEntry.expression
            if (bodyExpression != null) {
                val scopeToExtend = newWritableScopeImpl(contextAfterSubject, LexicalScopeKind.WHEN)
                val contextForEntry = contextWithExpectedType.replaceScope(scopeToExtend).replaceDataFlowInfo(ifTrueInfo).replaceContextDependency(INDEPENDENT)
                val entryTypeInfo = components.expressionTypingServices.getBlockReturnedTypeWithWritableScope(
                        scopeToExtend, listOf(bodyExpression), coercionStrategy, contextForEntry)

                jumpOutPossible = jumpOutPossible or entryTypeInfo.jumpOutPossible

                val entryType = entryTypeInfo.type

                expressionTypes.addIfNotNull(entryType)

                val entryDataFlowInfo = if (entryType != null) {
                    val entryValue = DataFlowValueFactory.createDataFlowValue(bodyExpression, entryType, contextAfterSubject)
                    entryTypeInfo.dataFlowInfo.assign(whenValue, entryValue)
                }
                else entryTypeInfo.dataFlowInfo

                commonDataFlowInfo = commonDataFlowInfo?.or(entryDataFlowInfo) ?: entryDataFlowInfo
            }
        }

        if (commonDataFlowInfo == null) {
            commonDataFlowInfo = contextAfterSubject.dataFlowInfo
        }
        else if (expression.elseExpression == null && !isExhaustive) {
            // Without else expression in non-exhaustive when, we *must* take initial data flow info into account,
            // because data flow can bypass all when branches in this case
            commonDataFlowInfo = commonDataFlowInfo.or(contextAfterSubject.dataFlowInfo)
        }

        var resultType: KotlinType? = if (expressionTypes.isNotEmpty()) {
            val commonSupertype = CommonSupertypes.commonSupertype(expressionTypes)
            val resultValue = DataFlowValueFactory.createDataFlowValue(expression, commonSupertype, contextAfterSubject)
            commonDataFlowInfo = commonDataFlowInfo.assign(resultValue, whenValue)
            if (isExhaustive && expression.elseExpression == null && KotlinBuiltIns.isNothing(commonSupertype)) {
                contextAfterSubject.trace.record(BindingContext.IMPLICIT_EXHAUSTIVE_WHEN, expression)
            }
            components.dataFlowAnalyzer.checkType(commonSupertype, expression, contextWithExpectedType)
        }
        else null

        return createTypeInfo(resultType, commonDataFlowInfo, jumpOutPossible, contextWithExpectedType.dataFlowInfo)
    }

    private fun analyzeWhenEntryConditions(
            whenEntry: KtWhenEntry,
            context: ExpressionTypingContext,
            subjectExpression: KtExpression?,
            subjectType: KotlinType,
            subjectDataFlowValue: DataFlowValue
    ): ConditionalDataFlowInfo {
        if (whenEntry.isElse) {
            return ConditionalDataFlowInfo(context.dataFlowInfo)
        }

        var entryInfo: ConditionalDataFlowInfo? = null
        var contextForCondition = context
        for (condition in whenEntry.conditions) {
            val conditionInfo = checkWhenCondition(subjectExpression, subjectType, condition,
                                                   contextForCondition, subjectDataFlowValue)
            entryInfo = entryInfo?.let {
                ConditionalDataFlowInfo(it.thenInfo.or(conditionInfo.thenInfo), it.elseInfo.and(conditionInfo.elseInfo))
            } ?: conditionInfo

            contextForCondition = contextForCondition.replaceDataFlowInfo(conditionInfo.elseInfo)
        }

        return entryInfo ?: ConditionalDataFlowInfo(context.dataFlowInfo)
    }

    private fun checkWhenCondition(
            subjectExpression: KtExpression?,
            subjectType: KotlinType,
            condition: KtWhenCondition,
            context: ExpressionTypingContext,
            subjectDataFlowValue: DataFlowValue
    ): ConditionalDataFlowInfo {
        var newDataFlowInfo = noChange(context)
        condition.accept(object : KtVisitorVoid() {
            override fun visitWhenConditionInRange(condition: KtWhenConditionInRange) {
                val rangeExpression = condition.rangeExpression ?: return
                if (subjectExpression == null) {
                    context.trace.report(EXPECTED_CONDITION.on(condition))
                    val dataFlowInfo = facade.getTypeInfo(rangeExpression, context).dataFlowInfo
                    newDataFlowInfo = ConditionalDataFlowInfo(dataFlowInfo, dataFlowInfo)
                    return
                }
                val argumentForSubject = CallMaker.makeExternalValueArgument(subjectExpression)
                val typeInfo = facade.checkInExpression(condition, condition.operationReference,
                                                        argumentForSubject, rangeExpression, context)
                val dataFlowInfo = typeInfo.dataFlowInfo
                newDataFlowInfo = ConditionalDataFlowInfo(dataFlowInfo, dataFlowInfo)
                val type = typeInfo.type
                if (type == null || !isBoolean(type)) {
                    context.trace.report(TYPE_MISMATCH_IN_RANGE.on(condition))
                }
            }

            override fun visitWhenConditionIsPattern(condition: KtWhenConditionIsPattern) {
                if (subjectExpression == null) {
                    context.trace.report(EXPECTED_CONDITION.on(condition))
                }
                if (condition.typeReference != null) {
                    val result = checkTypeForIs(context, subjectType, condition.typeReference, subjectDataFlowValue)
                    if (condition.isNegated) {
                        newDataFlowInfo = ConditionalDataFlowInfo(result.elseInfo, result.thenInfo)
                    }
                    else {
                        newDataFlowInfo = result
                    }
                }
            }

            override fun visitWhenConditionWithExpression(condition: KtWhenConditionWithExpression) {
                val expression = condition.expression
                if (expression != null) {
                    newDataFlowInfo = checkTypeForExpressionCondition(context, expression, subjectType, subjectExpression == null,
                                                                      subjectDataFlowValue)
                }
            }

            override fun visitKtElement(element: KtElement) {
                context.trace.report(UNSUPPORTED.on(element, javaClass.canonicalName))
            }
        })
        return newDataFlowInfo
    }

    private class ConditionalDataFlowInfo(val thenInfo: DataFlowInfo, val elseInfo: DataFlowInfo = thenInfo)

    private fun checkTypeForExpressionCondition(
            context: ExpressionTypingContext,
            expression: KtExpression?,
            subjectType: KotlinType,
            conditionExpected: Boolean,
            subjectDataFlowValue: DataFlowValue
    ): ConditionalDataFlowInfo {
        var newContext = context
        if (expression == null) {
            return noChange(newContext)
        }
        val typeInfo = facade.getTypeInfo(expression, newContext)
        val type = typeInfo.type ?: return noChange(newContext)
        newContext = newContext.replaceDataFlowInfo(typeInfo.dataFlowInfo)
        if (conditionExpected) {
            val booleanType = components.builtIns.booleanType
            if (!KotlinTypeChecker.DEFAULT.equalTypes(booleanType, type)) {
                newContext.trace.report(TYPE_MISMATCH_IN_CONDITION.on(expression, type))
            }
            else {
                val ifInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(expression, true, newContext)
                val elseInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(expression, false, newContext)
                return ConditionalDataFlowInfo(ifInfo, elseInfo)
            }
            return noChange(newContext)
        }
        checkTypeCompatibility(newContext, type, subjectType, expression)
        val expressionDataFlowValue = DataFlowValueFactory.createDataFlowValue(expression, type, newContext)
        var result = noChange(newContext)
        result = ConditionalDataFlowInfo(
                result.thenInfo.equate(subjectDataFlowValue, expressionDataFlowValue),
                result.elseInfo.disequate(subjectDataFlowValue, expressionDataFlowValue))
        return result
    }

    private fun checkTypeForIs(
            context: ExpressionTypingContext,
            subjectType: KotlinType,
            typeReferenceAfterIs: KtTypeReference?,
            subjectDataFlowValue: DataFlowValue
    ): ConditionalDataFlowInfo {
        if (typeReferenceAfterIs == null) {
            return noChange(context)
        }
        val typeResolutionContext = TypeResolutionContext(context.scope, context.trace, true, /*allowBareTypes=*/ true)
        val possiblyBareTarget = components.typeResolver.resolvePossiblyBareType(typeResolutionContext, typeReferenceAfterIs)
        val targetType = TypeReconstructionUtil.reconstructBareType(typeReferenceAfterIs, possiblyBareTarget, subjectType, context.trace, components.builtIns)

        if (targetType.isDynamic()) {
            context.trace.report(DYNAMIC_NOT_ALLOWED.on(typeReferenceAfterIs))
        }
        val targetDescriptor = TypeUtils.getClassDescriptor(targetType)
        if (targetDescriptor != null && DescriptorUtils.isEnumEntry(targetDescriptor)) {
            context.trace.report(IS_ENUM_ENTRY.on(typeReferenceAfterIs))
        }

        if (!subjectType.isMarkedNullable && targetType.isMarkedNullable) {
            val element = typeReferenceAfterIs.typeElement
            assert(element is KtNullableType) { "element must be instance of " + KtNullableType::class.java.name }
            context.trace.report(Errors.USELESS_NULLABLE_CHECK.on(element as KtNullableType))
        }
        checkTypeCompatibility(context, targetType, subjectType, typeReferenceAfterIs)
        if (CastDiagnosticsUtil.isCastErased(subjectType, targetType, KotlinTypeChecker.DEFAULT)) {
            context.trace.report(Errors.CANNOT_CHECK_FOR_ERASED.on(typeReferenceAfterIs, targetType))
        }
        return ConditionalDataFlowInfo(context.dataFlowInfo.establishSubtyping(subjectDataFlowValue, targetType), context.dataFlowInfo)
    }

    private fun noChange(context: ExpressionTypingContext) = ConditionalDataFlowInfo(context.dataFlowInfo, context.dataFlowInfo)

    /*
     * (a: SubjectType) is Type
     */
    private fun checkTypeCompatibility(
            context: ExpressionTypingContext,
            type: KotlinType?,
            subjectType: KotlinType,
            reportErrorOn: KtElement
    ) {
        // TODO : Take smart casts into account?
        if (type == null) {
            return
        }
        if (TypeIntersector.isIntersectionEmpty(type, subjectType)) {
            context.trace.report(INCOMPATIBLE_TYPES.on(reportErrorOn, type, subjectType))
            return
        }

        // check if the pattern is essentially a 'null' expression
        if (KotlinBuiltIns.isNullableNothing(type) && !TypeUtils.isNullableType(subjectType)) {
            context.trace.report(SENSELESS_NULL_IN_WHEN.on(reportErrorOn))
        }
    }
}
