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

package org.jetbrains.kotlin.resolve.calls.smartcasts

import com.google.common.collect.Lists
import com.google.common.collect.Sets
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors.SMARTCAST_IMPOSSIBLE
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.IMPLICIT_RECEIVER_SMARTCAST
import org.jetbrains.kotlin.resolve.BindingContext.SMARTCAST
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeIntersector
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker

// We do not want to make methods static to keep SmartCastManager as a component
@SuppressWarnings("MethodMayBeStatic")
class SmartCastManager {

    fun getSmartCastVariants(
            receiverToCast: ReceiverValue,
            context: ResolutionContext<*>): List<KotlinType> {
        return getSmartCastVariants(receiverToCast, context.trace.bindingContext, context.scope.ownerDescriptor, context.dataFlowInfo)
    }

    fun getSmartCastVariants(
            receiverToCast: ReceiverValue,
            bindingContext: BindingContext,
            containingDeclarationOrModule: DeclarationDescriptor,
            dataFlowInfo: DataFlowInfo): List<KotlinType> {
        val variants = Lists.newArrayList<KotlinType>()
        variants.add(receiverToCast.type)
        variants.addAll(getSmartCastVariantsExcludingReceiver(bindingContext, containingDeclarationOrModule, dataFlowInfo, receiverToCast))
        return variants
    }

    fun getSmartCastVariantsWithLessSpecificExcluded(
            receiverToCast: ReceiverValue,
            bindingContext: BindingContext,
            containingDeclarationOrModule: DeclarationDescriptor,
            dataFlowInfo: DataFlowInfo): List<KotlinType> {
        val variants = getSmartCastVariants(receiverToCast, bindingContext, containingDeclarationOrModule, dataFlowInfo).distinct()
        return variants.filter { type -> variants.none { another -> another !== type && KotlinTypeChecker.DEFAULT.isSubtypeOf(another, type) } }
    }

    /**
     * @return variants @param receiverToCast may be cast to according to context dataFlowInfo, receiverToCast itself is NOT included
     */
    fun getSmartCastVariantsExcludingReceiver(
            context: ResolutionContext<*>,
            receiverToCast: ReceiverValue): Collection<KotlinType> {
        return getSmartCastVariantsExcludingReceiver(context.trace.bindingContext,
                                                     context.scope.ownerDescriptor,
                                                     context.dataFlowInfo,
                                                     receiverToCast)
    }

    /**
     * @return variants @param receiverToCast may be cast to according to @param dataFlowInfo, @param receiverToCast itself is NOT included
     */
    fun getSmartCastVariantsExcludingReceiver(
            bindingContext: BindingContext,
            containingDeclarationOrModule: DeclarationDescriptor,
            dataFlowInfo: DataFlowInfo,
            receiverToCast: ReceiverValue): Collection<KotlinType> {
        val dataFlowValue = DataFlowValueFactory.createDataFlowValue(
                receiverToCast, bindingContext, containingDeclarationOrModule)

        return dataFlowInfo.getCollectedTypes(dataFlowValue)
    }

    fun isSubTypeBySmartCastIgnoringNullability(
            receiverArgument: ReceiverValue,
            receiverParameterType: KotlinType,
            context: ResolutionContext<*>): Boolean {
        val smartCastTypes = getSmartCastVariants(receiverArgument, context)
        return getSmartCastSubType(TypeUtils.makeNullable(receiverParameterType), smartCastTypes) != null
    }

    private fun getSmartCastSubType(
            receiverParameterType: KotlinType,
            smartCastTypes: Collection<KotlinType>): KotlinType? {
        val subTypes = Sets.newHashSet<KotlinType>()
        for (smartCastType in smartCastTypes) {
            if (ArgumentTypeResolver.isSubtypeOfForArgumentType(smartCastType, receiverParameterType)) {
                subTypes.add(smartCastType)
            }
        }
        if (subTypes.isEmpty()) return null

        val intersection = TypeIntersector.intersectTypes(KotlinTypeChecker.DEFAULT, subTypes)
        if (intersection == null || !intersection.constructor.isDenotable) {
            return receiverParameterType
        }
        return intersection
    }

    companion object {

        private fun recordCastOrError(
                expression: KtExpression,
                type: KotlinType,
                trace: BindingTrace,
                dataFlowValue: DataFlowValue,
                recordExpressionType: Boolean) {
            if (KotlinBuiltIns.isNullableNothing(type)) return
            if (dataFlowValue.isPredictable) {
                trace.record(SMARTCAST, expression, type)
                if (recordExpressionType) {
                    //TODO
                    //Why the expression type is rewritten for receivers and is not rewritten for arguments? Is it necessary?
                    trace.recordType(expression, type)
                }
            }
            else {
                trace.report(SMARTCAST_IMPOSSIBLE.on(expression, type, expression.text, dataFlowValue.kind.description))
            }
        }

        fun checkAndRecordPossibleCast(
                dataFlowValue: DataFlowValue,
                expectedType: KotlinType,
                expression: KtExpression?,
                c: ResolutionContext<*>,
                calleeExpression: KtExpression?,
                recordExpressionType: Boolean): SmartCastResult? {
            return checkAndRecordPossibleCast(
                    dataFlowValue, expectedType, null, expression, c, calleeExpression, recordExpressionType)
        }

        fun checkAndRecordPossibleCast(
                dataFlowValue: DataFlowValue,
                expectedType: KotlinType,
                additionalPredicate: Function1<KotlinType, Boolean>?,
                expression: KtExpression?,
                c: ResolutionContext<*>,
                calleeExpression: KtExpression?,
                recordExpressionType: Boolean): SmartCastResult? {
            for (possibleType in c.dataFlowInfo.getCollectedTypes(dataFlowValue)) {
                if (ArgumentTypeResolver.isSubtypeOfForArgumentType(possibleType, expectedType) && (additionalPredicate == null || additionalPredicate.invoke(possibleType))) {
                    if (expression != null) {
                        recordCastOrError(expression, possibleType, c.trace, dataFlowValue, recordExpressionType)
                    }
                    else if (calleeExpression != null && dataFlowValue.isPredictable) {
                        c.trace.record(IMPLICIT_RECEIVER_SMARTCAST, calleeExpression, possibleType)
                    }
                    return SmartCastResult(possibleType, dataFlowValue.isPredictable)
                }
            }

            if (!c.dataFlowInfo.getCollectedNullability(dataFlowValue).canBeNull() && !expectedType.isMarkedNullable) {
                // Handling cases like:
                // fun bar(x: Any) {}
                // fun <T : Any?> foo(x: T) {
                //      if (x != null) {
                //          bar(x) // Should be allowed with smart cast
                //      }
                // }
                //
                // It doesn't handled by lower code with getPossibleTypes because smart cast of T after `x != null` is still has same type T.
                // But at the same time we're sure that `x` can't be null and just check for such cases manually

                // E.g. in case x!! when x has type of T where T is type parameter with nullable upper bounds
                // x!! is immanently not null (see DataFlowValueFactory.createDataFlowValue for expression)
                val immanentlyNotNull = !dataFlowValue.immanentNullability.canBeNull()
                val nullableExpectedType = TypeUtils.makeNullable(expectedType)

                if (ArgumentTypeResolver.isSubtypeOfForArgumentType(dataFlowValue.type, nullableExpectedType) && (additionalPredicate == null || additionalPredicate.invoke(dataFlowValue.type))) {
                    if (!immanentlyNotNull) {
                        if (expression != null) {
                            recordCastOrError(expression, dataFlowValue.type, c.trace, dataFlowValue, recordExpressionType)
                        }
                    }

                    return SmartCastResult(dataFlowValue.type, immanentlyNotNull || dataFlowValue.isPredictable)
                }
                return checkAndRecordPossibleCast(dataFlowValue, nullableExpectedType, expression, c, calleeExpression, recordExpressionType)
            }

            return null
        }
    }
}
