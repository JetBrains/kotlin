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

package org.jetbrains.kotlin.resolve.calls.smartcasts

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Errors.SMARTCAST_IMPOSSIBLE
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.IMPLICIT_RECEIVER_SMARTCAST
import org.jetbrains.kotlin.resolve.BindingContext.SMARTCAST
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.expandIntersectionTypeIfNecessary
import java.util.*

class SmartCastManager {

    fun getSmartCastVariants(
        receiverToCast: ReceiverValue,
        bindingContext: BindingContext,
        containingDeclarationOrModule: DeclarationDescriptor,
        dataFlowInfo: DataFlowInfo,
        languageVersionSettings: LanguageVersionSettings,
        dataFlowValueFactory: DataFlowValueFactory
    ): List<KotlinType> {
        val variants = getSmartCastVariantsExcludingReceiver(
            bindingContext, containingDeclarationOrModule, dataFlowInfo, receiverToCast, languageVersionSettings, dataFlowValueFactory
        )
        val result = ArrayList<KotlinType>(variants.size + 1)
        result.add(receiverToCast.type)
        result.addAll(variants)
        return result
    }

    /**
     * @return variants @param receiverToCast may be cast to according to context dataFlowInfo, receiverToCast itself is NOT included
     */
    fun getSmartCastVariantsExcludingReceiver(
        context: ResolutionContext<*>,
        receiverToCast: ReceiverValue
    ): Collection<KotlinType> {
        return getSmartCastVariantsExcludingReceiver(
            context.trace.bindingContext,
            context.scope.ownerDescriptor,
            context.dataFlowInfo,
            receiverToCast,
            context.languageVersionSettings,
            context.dataFlowValueFactory
        )
    }

    /**
     * @return variants @param receiverToCast may be cast to according to @param dataFlowInfo, @param receiverToCast itself is NOT included
     */
    private fun getSmartCastVariantsExcludingReceiver(
        bindingContext: BindingContext,
        containingDeclarationOrModule: DeclarationDescriptor,
        dataFlowInfo: DataFlowInfo,
        receiverToCast: ReceiverValue,
        languageVersionSettings: LanguageVersionSettings,
        dataFlowValueFactory: DataFlowValueFactory
    ): Collection<KotlinType> {
        val dataFlowValue = dataFlowValueFactory.createDataFlowValue(receiverToCast, bindingContext, containingDeclarationOrModule)
        return dataFlowInfo.getCollectedTypes(dataFlowValue, languageVersionSettings)
    }

    fun getSmartCastReceiverResult(
        receiverArgument: ReceiverValue,
        receiverParameterType: KotlinType,
        context: ResolutionContext<*>
    ): ReceiverSmartCastResult? {
        getSmartCastReceiverResultWithGivenNullability(receiverArgument, receiverParameterType, context)?.let {
            return it
        }

        val nullableParameterType = TypeUtils.makeNullable(receiverParameterType)
        return when {
            getSmartCastReceiverResultWithGivenNullability(receiverArgument, nullableParameterType, context) == null -> null
            else -> ReceiverSmartCastResult.SMARTCAST_NEEDED_OR_NOT_NULL_EXPECTED
        }
    }

    private fun getSmartCastReceiverResultWithGivenNullability(
        receiverArgument: ReceiverValue,
        receiverParameterType: KotlinType,
        context: ResolutionContext<*>
    ): ReceiverSmartCastResult? =
        when {
            ArgumentTypeResolver.isSubtypeOfForArgumentType(receiverArgument.type, receiverParameterType) ->
                ReceiverSmartCastResult.OK
            getSmartCastVariantsExcludingReceiver(context, receiverArgument).any {
                ArgumentTypeResolver.isSubtypeOfForArgumentType(it, receiverParameterType)
            } ->
                ReceiverSmartCastResult.SMARTCAST_NEEDED_OR_NOT_NULL_EXPECTED
            else -> null
        }

    enum class ReceiverSmartCastResult {
        OK,
        SMARTCAST_NEEDED_OR_NOT_NULL_EXPECTED
    }

    companion object {

        private fun recordCastOrError(
            expression: KtExpression,
            type: KotlinType,
            trace: BindingTrace,
            dataFlowValue: DataFlowValue,
            call: Call?,
            recordExpressionType: Boolean
        ) {
            if (KotlinBuiltIns.isNullableNothing(type)) return
            if (dataFlowValue.isStable) {
                if (dataFlowValue.kind == DataFlowValue.Kind.LEGACY_STABLE_LOCAL_DELEGATED_PROPERTY) {
                    trace.report(Errors.DEPRECATED_SMARTCAST.on(expression, type, expression.text, dataFlowValue.kind.description))
                }

                val oldSmartCasts = trace[SMARTCAST, expression]
                val newSmartCast = SingleSmartCast(call, type)
                if (oldSmartCasts != null) {
                    val oldType = oldSmartCasts.type(call)
                    if (oldType != null && oldType != type) {
                        throw AssertionError("Rewriting key $call for smart cast on ${expression.text}")
                    }
                }
                trace.record(SMARTCAST, expression, oldSmartCasts?.let { it + newSmartCast } ?: newSmartCast)
                if (recordExpressionType) {
                    //TODO
                    //Why the expression type is rewritten for receivers and is not rewritten for arguments? Is it necessary?
                    trace.recordType(expression, type)
                }
            } else {
                trace.report(SMARTCAST_IMPOSSIBLE.on(expression, type, expression.text, dataFlowValue.kind.description))
            }
        }

        fun checkAndRecordPossibleCast(
            dataFlowValue: DataFlowValue,
            expectedType: KotlinType,
            expression: KtExpression?,
            c: ResolutionContext<*>,
            call: Call?,
            recordExpressionType: Boolean
        ): SmartCastResult? {
            return checkAndRecordPossibleCast(dataFlowValue, expectedType, null, expression, c, call, recordExpressionType)
        }

        fun checkAndRecordPossibleCast(
            dataFlowValue: DataFlowValue,
            expectedType: KotlinType,
            additionalPredicate: ((KotlinType) -> Boolean)?,
            expression: KtExpression?,
            c: ResolutionContext<*>,
            call: Call?,
            recordExpressionType: Boolean
        ): SmartCastResult? {
            val calleeExpression = call?.calleeExpression
            val expectedTypes = if (c.languageVersionSettings.supportsFeature(LanguageFeature.NewInference))
                expectedType.expandIntersectionTypeIfNecessary()
            else
                listOf(expectedType)

            for (possibleType in c.dataFlowInfo.getCollectedTypes(dataFlowValue, c.languageVersionSettings)) {
                if (expectedTypes.any { ArgumentTypeResolver.isSubtypeOfForArgumentType(possibleType, it) } &&
                    (additionalPredicate == null || additionalPredicate(possibleType))
                ) {
                    if (expression != null) {
                        recordCastOrError(expression, possibleType, c.trace, dataFlowValue, call, recordExpressionType)
                    } else if (calleeExpression != null && dataFlowValue.isStable) {
                        val receiver = (dataFlowValue.identifierInfo as? IdentifierInfo.Receiver)?.value
                        if (receiver is ImplicitReceiver) {
                            val oldSmartCasts = c.trace[IMPLICIT_RECEIVER_SMARTCAST, calleeExpression]
                            val newSmartCasts = ImplicitSmartCasts(receiver, possibleType)
                            if (oldSmartCasts != null) {
                                val oldType = oldSmartCasts.receiverTypes[receiver]
                                if (oldType != null && oldType != possibleType) {
                                    throw AssertionError(
                                        "Rewriting key $receiver for implicit smart cast on ${calleeExpression.text}: " +
                                                "was $oldType, now $possibleType"
                                    )
                                }
                            }
                            c.trace.record(IMPLICIT_RECEIVER_SMARTCAST, calleeExpression,
                                           oldSmartCasts?.let { it + newSmartCasts } ?: newSmartCasts)

                        }
                    }
                    return SmartCastResult(possibleType, dataFlowValue.isStable)
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

                if (ArgumentTypeResolver.isSubtypeOfForArgumentType(dataFlowValue.type, nullableExpectedType) &&
                    (additionalPredicate == null || additionalPredicate(dataFlowValue.type))
                ) {
                    if (!immanentlyNotNull && expression != null) {
                        recordCastOrError(expression, dataFlowValue.type, c.trace, dataFlowValue, call, recordExpressionType)
                    }

                    return SmartCastResult(dataFlowValue.type, immanentlyNotNull || dataFlowValue.isStable)
                }
                return checkAndRecordPossibleCast(dataFlowValue, nullableExpectedType, expression, c, call, recordExpressionType)
            }

            return null
        }
    }
}
