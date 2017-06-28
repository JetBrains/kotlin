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
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.org.objectweb.asm.Type

fun ExpressionCodegen.createRangeValueForExpression(rangeExpression: KtExpression): RangeValue {
    getResolvedCallForRangeExpression(bindingContext, rangeExpression)?.let {
        createIntrinsifiedRangeValueOrNull(it)?.let {
            return it
        }
    }

    val rangeType = bindingContext.getType(rangeExpression)!!
    val asmRangeType = asmType(rangeType)

    return when {
        asmRangeType.sort == Type.ARRAY ->
            ArrayRangeValue()
        isRange(rangeType) ->
            PrimitiveRangeRangeValue()
        isProgression(rangeType) ->
            PrimitiveProgressionRangeValue()
        isSubtypeOfCharSequence(rangeType, state.module.builtIns) ->
            CharSequenceRangeValue()
        else ->
            IterableRangeValue()
    }
}

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

private fun createIntrinsifiedRangeValueOrNull(rangeCall: ResolvedCall<out CallableDescriptor>): RangeValue? {
    val rangeCallee = rangeCall.resultingDescriptor

    return when {
        isPrimitiveNumberRangeTo(rangeCallee) ->
            PrimitiveNumberRangeToRangeValue(rangeCall)
        isPrimitiveNumberDownTo(rangeCallee) ->
            DownToProgressionRangeValue(rangeCall)
        isPrimitiveNumberUntil(rangeCallee) ->
            PrimitiveNumberUntilRangeValue(rangeCall)
        isArrayOrPrimitiveArrayIndices(rangeCallee) ->
            ArrayIndicesRangeValue(rangeCall)
        isCollectionIndices(rangeCallee) ->
            CollectionIndicesRangeValue(rangeCall)
        isCharSequenceIndices(rangeCallee) ->
            CharSequenceIndicesRangeValue(rangeCall)
        else ->
            null
    }
}