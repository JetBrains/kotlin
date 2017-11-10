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
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
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
import java.util.*

class SmartCastManager {

    fun getSmartCastVariants(
        receiverToCast: ReceiverValue,
        bindingContext: BindingContext,
        containingDeclarationOrModule: DeclarationDescriptor,
        dataFlowInfo: DataFlowInfo,
        languageVersionSettings: LanguageVersionSettings
    ): List<KotlinType> {
        val variants = getSmartCastVariantsExcludingReceiver(
            bindingContext, containingDeclarationOrModule, dataFlowInfo, receiverToCast, languageVersionSettings
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
            context.languageVersionSettings
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
        languageVersionSettings: LanguageVersionSettings
    ): Collection<KotlinType> {
        val dataFlowValue = DataFlowValueFactory.createDataFlowValue(receiverToCast, bindingContext, containingDeclarationOrModule)
        return dataFlowInfo.getCollectedTypes(dataFlowValue, languageVersionSettings)
    }

    // Checks if `receiverArgument` can be cast to `receiverParameterType` with smartcasts taken into consideration
    fun getSmartCastReceiverResult(
        receiverArgument: ReceiverValue,
        receiverParameterType: KotlinType,
        context: ResolutionContext<*>
    ): ReceiverSmartCastResult? {
        // Ok, let's just check if some cast is known with exact types
        getSmartCastReceiverResultWithGivenNullability(receiverArgument, receiverParameterType, context)?.let {
            // We have some cast with exact type
            // Noe that it also returns for plain subtypes
            return it
        }

        // No luck here, but maybe we can find suitable cast for nullable reciever?
        val nullableParameterType = TypeUtils.makeNullable(receiverParameterType)

        return when {
        // Still no luck, return 'null'
            getSmartCastReceiverResultWithGivenNullability(receiverArgument, nullableParameterType, context) == null -> null

        // Found cast, but note that we've expanded nullability, so no matters what we've found, it's still "NOT_NULL_EXPECTED"
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
        // Fun fact -- it is used only in SmartCastManager
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
                // Smartcast is possible, lets try to do this!

                val oldSmartCasts = trace[SMARTCAST, expression]
                val newSmartCast = SingleSmartCast(call, type)
                if (oldSmartCasts != null) {
                    // There already was some smartcast on that expression

                    // Let's check if this SMARTCAST slice (for the same expression!) already contains cast for this particular 'call'
                    val oldType = oldSmartCasts.type(call)
                    if (oldType != null && oldType != type) {
                        // Oops, this slice already contains smartcast for this particular 'call'
                        throw AssertionError("Rewriting key $call for smart cast on ${expression.text}")
                    }
                }
                // Ok, either there was no SMARTCAST slice at all for this 'expression', or it didn't contain a cast for this particular call
                trace.record(SMARTCAST, expression, oldSmartCasts?.let { it + newSmartCast } ?: newSmartCast)
                if (recordExpressionType) {
                    //TODO
                    //Why the expression type is rewritten for receivers and is not rewritten for arguments? Is it necessary?
                    trace.recordType(expression, type)
                }
            } else {
                // Unstable data
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

        /*
        Semantics of this method:
        1. Take 'dataFlowValue'
        2. Pull 'dataFlowInfo' from 'c' and get all types for that 'dataFlowValue'
        3. If some of that types matches 'expectedType' and 'additionalPredicate' (if any), then record smartcast/error (i.e. diagnostic) with stability taken into consideration
           (can also involve some additional hoops for implicit receiver)

        IF NO TYPES WERE MATCHED ON PREVIOUS STEP, THEN:
        4.
         */
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
            for (possibleType in c.dataFlowInfo.getCollectedTypes(dataFlowValue, c.languageVersionSettings)) {
                // Check another type from smartcast
                if (ArgumentTypeResolver.isSubtypeOfForArgumentType(possibleType, expectedType) &&
                    (additionalPredicate == null || additionalPredicate(possibleType))
                ) {
                    // This type suits us!
                    if (expression != null) {
                        // records cast if DFV is stable (with rewrite checks) OR records SMARTCAST_IMPOSSIBLE if DFV is unstable
                        recordCastOrError(expression, possibleType, c.trace, dataFlowValue, call, recordExpressionType)
                    } else if (calleeExpression != null && dataFlowValue.isStable) {
                        // This is the case when we have implicit receiver (e.g. in cases like 'with(a) { if (this is String) ... }'
                        // or in the body of extension function)
                        //
                        // Here we have to invent additional diagnostic on call itself
                        val receiver = (dataFlowValue.identifierInfo as? IdentifierInfo.Receiver)?.value
                        if (receiver is ImplicitReceiver) {
                            // This is the case #2

                            // dat logic duplication tho (see above)
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

                    // ...but who the fuck is going to report this smartcast? :(
                    return SmartCastResult(possibleType, dataFlowValue.isStable)
                }
            }

            if (!c.dataFlowInfo.getCollectedNullability(dataFlowValue).canBeNull() && !expectedType.isMarkedNullable) {
                // Ok, this value can't be null, and we expect not-null

                // Handling cases like:
                // fun bar(x: Any) {}
                // fun <T : Any?> foo(x: T) {
                //      if (x != null) {
                //          bar(x) // Should be allowed with smart cast
                //      }
                // }
                //
                // It doesn't handled by upper code with getCollectedTypes because smart cast of T after `x != null` is still has same type T.
                // But at the same time we're sure that `x` can't be null and just check for such cases manually

                // E.g. in case x!! when x has type of T where T is type parameter with nullable upper bounds
                // x!! is immanently not null (see DataFlowValueFactory.createDataFlowValue for expression)


                // Segment below is much easier to understand if considered without 'immanentlyNotNull' flag

                // Essentially, this is fast path for cases when 'dataFlowValue.type' can be casted to 'expected type'
                // with the help of smartcasts.
                val immanentlyNotNull = !dataFlowValue.immanentNullability.canBeNull()
                val nullableExpectedType = TypeUtils.makeNullable(expectedType)

                // Let's check if types are suitable modulo nullability smartcast
                if (ArgumentTypeResolver.isSubtypeOfForArgumentType(dataFlowValue.type, nullableExpectedType) &&
                    (additionalPredicate == null || additionalPredicate(dataFlowValue.type))
                ) {
                    // Ok, dataFlowValue.type <: expectedType?
                    // AND
                    // we know what dataFlowValue can be casted to not-null, and we expect not-null
                    // =>
                    // let's record smartcast!
                    if (!immanentlyNotNull && expression != null) {
                        // Let's record smartcast!
                        recordCastOrError(expression, dataFlowValue.type, c.trace, dataFlowValue, call, recordExpressionType)
                    }

                    return SmartCastResult(dataFlowValue.type, immanentlyNotNull || dataFlowValue.isStable)
                }
                // Now, why do we even have that 'immanentlyNotNull' flag?
                // Because it indicates that we're doing '!!'-assertion on generic type with nullable upper bound, which should be
                // handled separately if it is in call chain (i.e. in cases like 'x!!.foo) because we have no other place to
                // handle this case (compare that with cases like 'x!!; x.length' - here first statement provides DFI that x != null,
                // and we already see it in the second statement)


                // Ok, so we can't transform 'dataFlowValue' to 'expectedType' via nullability smartcast
                // We say here: let's try again, but we will try to find cast to the nullable expected type
                return checkAndRecordPossibleCast(dataFlowValue, nullableExpectedType, expression, c, call, recordExpressionType)
            }

            return null
        }
    }
}
