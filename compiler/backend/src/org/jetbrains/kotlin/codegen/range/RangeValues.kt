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

package org.jetbrains.kotlin.codegen.range

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.org.objectweb.asm.Type

fun ExpressionCodegen.createRangeValueForExpression(rangeExpression: KtExpression): RangeValue {
    // NB when you implement a new intrinsic RangeValue,
    // also look into org.jetbrains.kotlin.generators.tests and update related testData generators
    // (e.g., GenerateInRangeExpressionTestData).

    val resolvedCall = getResolvedCallForRangeExpression(bindingContext, rangeExpression)
    if (resolvedCall != null) {
        val intrinsicRangeValue = createIntrinsifiedRangeValueOrNull(resolvedCall)
        if (intrinsicRangeValue != null) {
            return intrinsicRangeValue
        }
    }

    val rangeType = bindingContext.getType(rangeExpression)!!
    val asmRangeType = asmType(rangeType)

    val loopRangeIteratorResolvedCall = bindingContext[BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL, rangeExpression]

    val builtIns = state.module.builtIns
    return when {
        asmRangeType.sort == Type.ARRAY -> {
            val properForInArraySemantics =
                state.languageVersionSettings.supportsFeature(LanguageFeature.ProperForInArrayLoopRangeVariableAssignmentSemantic)
            ArrayRangeValue(
                properForInArraySemantics || !isLocalVarReference(rangeExpression, bindingContext),
                properForInArraySemantics
            )
        }

        isPrimitiveRange(rangeType) || isUnsignedRange(rangeType) ->
            PrimitiveRangeRangeValue(rangeExpression)
        isPrimitiveProgression(rangeType) || isUnsignedProgression(rangeType) ->
            PrimitiveProgressionRangeValue(rangeExpression)
        isSubtypeOfString(rangeType, builtIns) && isCharSequenceIteratorCall(loopRangeIteratorResolvedCall) ->
            CharSequenceRangeValue(true, AsmTypes.JAVA_STRING_TYPE)
        isSubtypeOfCharSequence(rangeType, builtIns) && isCharSequenceIteratorCall(loopRangeIteratorResolvedCall) ->
            CharSequenceRangeValue(false, null)
        else ->
            IterableRangeValue()
    }
}

@Suppress("DEPRECATION")
fun isLocalVarReference(rangeExpression: KtExpression, bindingContext: BindingContext): Boolean {
    if (rangeExpression !is KtSimpleNameExpression) return false
    val resultingDescriptor = rangeExpression.getResolvedCall(bindingContext)?.resultingDescriptor ?: return false
    return resultingDescriptor is LocalVariableDescriptor &&
            resultingDescriptor !is SyntheticFieldDescriptor &&
            !resultingDescriptor.isDelegated &&
            resultingDescriptor.isVar
}

private fun isSubtypeOfString(type: KotlinType, builtIns: KotlinBuiltIns) =
    KotlinTypeChecker.DEFAULT.isSubtypeOf(type, builtIns.stringType)

private fun isSubtypeOfCharSequence(type: KotlinType, builtIns: KotlinBuiltIns) =
    KotlinTypeChecker.DEFAULT.isSubtypeOf(type, builtIns.getBuiltInClassByName(Name.identifier("CharSequence")).defaultType)

private fun getResolvedCallForRangeExpression(
    bindingContext: BindingContext,
    rangeExpression: KtExpression
): ResolvedCall<out CallableDescriptor>? {
    val expression = KtPsiUtil.deparenthesize(rangeExpression) ?: return null

    return when (expression) {
        is KtQualifiedExpression ->
            expression.selectorExpression.let { selector ->
                if (selector is KtCallExpression || selector is KtSimpleNameExpression)
                    selector.getResolvedCall(bindingContext)
                else
                    null
            }

        is KtSimpleNameExpression, is KtCallExpression ->
            expression.getResolvedCall(bindingContext)
        is KtBinaryExpression ->
            expression.operationReference.getResolvedCall(bindingContext)
        else ->
            null
    }
}

private fun ExpressionCodegen.createIntrinsifiedRangeValueOrNull(rangeCall: ResolvedCall<out CallableDescriptor>): RangeValue? {
    val rangeCallee = rangeCall.resultingDescriptor

    return when {
        isPrimitiveNumberRangeTo(rangeCallee) || isUnsignedIntegerRangeTo(rangeCallee) ->
            PrimitiveNumberRangeLiteralRangeValue(rangeCall)
        isPrimitiveNumberDownTo(rangeCallee) || isUnsignedIntegerDownTo(rangeCallee) ->
            DownToProgressionRangeValue(rangeCall)
        isPrimitiveNumberUntil(rangeCallee) || isUnsignedIntegerUntil(rangeCallee) ->
            PrimitiveNumberUntilRangeValue(rangeCall)
        isArrayOrPrimitiveArrayIndices(rangeCallee) ->
            ArrayIndicesRangeValue(rangeCall)
        isCollectionIndices(rangeCallee) ->
            CollectionIndicesRangeValue(rangeCall)
        isCharSequenceIndices(rangeCallee) ->
            CharSequenceIndicesRangeValue(rangeCall)
        isArrayOrPrimitiveArrayWithIndex(rangeCallee) ->
            ArrayWithIndexRangeValue(rangeCall)
        isIterableWithIndex(rangeCallee) ->
            IterableWithIndexRangeValue(rangeCall)
        isSequenceWithIndex(rangeCallee) ->
            SequenceWithIndexRangeValue(rangeCall)
        isCharSequenceWithIndex(rangeCallee) ->
            CharSequenceWithIndexRangeValue(rangeCall)
        isComparableRangeTo(rangeCallee) ->
            ComparableRangeLiteralRangeValue(this, rangeCall)
        isPrimitiveProgressionReverse(rangeCallee) ->
            createReversedRangeValueOrNull(rangeCall)
        else ->
            null
    }
}

private fun ExpressionCodegen.createReversedRangeValueOrNull(rangeCall: ResolvedCall<out CallableDescriptor>): RangeValue? {
    val receiver = rangeCall.extensionReceiver as? ExpressionReceiver ?: return null
    val receiverRangeValue = createRangeValueForExpression(receiver.expression) as? ReversableRangeValue ?: return null
    return ReversedRangeValue(receiverRangeValue)
}

private fun isCharSequenceIteratorCall(iteratorCall: ResolvedCall<*>?) =
    iteratorCall?.resultingDescriptor?.let { isCharSequenceIterator(it) } ?: false