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

import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.range.comparison.getComparisonGeneratorForKotlinType
import org.jetbrains.kotlin.codegen.range.comparison.getComparisonGeneratorForRangeContainsCall
import org.jetbrains.kotlin.codegen.range.forLoop.ForInDefinitelySafeSimpleProgressionLoopGenerator
import org.jetbrains.kotlin.codegen.range.forLoop.ForLoopGenerator
import org.jetbrains.kotlin.codegen.range.inExpression.CallBasedInExpressionGenerator
import org.jetbrains.kotlin.codegen.range.inExpression.InExpressionGenerator
import org.jetbrains.kotlin.codegen.range.inExpression.InFloatingPointRangeLiteralExpressionGenerator
import org.jetbrains.kotlin.codegen.range.inExpression.InIntegralContinuousRangeExpressionGenerator
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.Type

abstract class PrimitiveNumberRangeIntrinsicRangeValue(
    rangeCall: ResolvedCall<out CallableDescriptor>
) : CallIntrinsicRangeValue(rangeCall) {

    protected val elementKotlinType =
        rangeCall.resultingDescriptor.returnType?.let { getRangeOrProgressionElementType(it) }
            ?: throw AssertionError("Unexpected range ")

    override fun isIntrinsicInCall(resolvedCallForIn: ResolvedCall<out CallableDescriptor>) =
        resolvedCallForIn.resultingDescriptor.let {
            isPrimitiveRangeContains(it) ||
                    isClosedFloatingPointRangeContains(it) ||
                    isPrimitiveNumberRangeExtensionContainsPrimitiveNumber(it)
        }

    override fun createIntrinsicInExpressionGenerator(
        codegen: ExpressionCodegen,
        operatorReference: KtSimpleNameExpression,
        resolvedCall: ResolvedCall<out CallableDescriptor>
    ): InExpressionGenerator {
        val comparisonGenerator = getComparisonGeneratorForRangeContainsCall(codegen, resolvedCall)
        val comparedType = comparisonGenerator?.comparedType

        return when {
            comparisonGenerator == null -> CallBasedInExpressionGenerator(codegen, operatorReference)

            comparedType == Type.DOUBLE_TYPE || comparedType == Type.FLOAT_TYPE -> {
                val rangeLiteral = getBoundedValue(codegen) as? SimpleBoundedValue
                    ?: throw AssertionError("Floating point intrinsic range value should be a range literal")
                InFloatingPointRangeLiteralExpressionGenerator(operatorReference, rangeLiteral, comparisonGenerator, codegen.frameMap)
            }

            else ->
                InIntegralContinuousRangeExpressionGenerator(
                    operatorReference, getBoundedValue(codegen), comparisonGenerator, codegen.frameMap
                )
        }
    }

    protected abstract fun getBoundedValue(codegen: ExpressionCodegen): BoundedValue

    protected fun createConstBoundedForLoopGeneratorOrNull(
        codegen: ExpressionCodegen,
        forExpression: KtForExpression,
        startValue: StackValue,
        endExpression: KtExpression,
        step: Int,
        isStartInclusive: Boolean = true
    ): ForLoopGenerator? {
        val endConstValue = codegen.getCompileTimeConstant(endExpression).safeAs<IntegerValueConstant<*>>() ?: return null

        return when (endConstValue) {
            is ByteValue -> {
                val endIntValue = endConstValue.value.toInt()
                if (isProhibitedIntConstEndValue(step, endIntValue))
                    null
                else
                    createConstBoundedIntForLoopGenerator(codegen, forExpression, startValue, endIntValue, step, isStartInclusive)
            }

            is ShortValue -> {
                val endIntValue = endConstValue.value.toInt()
                if (isProhibitedIntConstEndValue(step, endIntValue))
                    null
                else
                    createConstBoundedIntForLoopGenerator(codegen, forExpression, startValue, endIntValue, step, isStartInclusive)
            }

            is IntValue -> {
                val endIntValue = endConstValue.value
                if (isProhibitedIntConstEndValue(step, endIntValue))
                    null
                else
                    createConstBoundedIntForLoopGenerator(codegen, forExpression, startValue, endIntValue, step, isStartInclusive)
            }

            is CharValue -> {
                val endCharValue = endConstValue.value
                if (isProhibitedCharConstEndValue(step, endCharValue))
                    null
                else
                    createConstBoundedIntForLoopGenerator(codegen, forExpression, startValue, endCharValue.toInt(), step, isStartInclusive)
            }

            is LongValue -> {
                val endLongValue = endConstValue.value
                if (isProhibitedLongConstEndValue(step, endLongValue))
                    null
                else
                    createConstBoundedLongForLoopGenerator(codegen, forExpression, startValue, endLongValue, step, isStartInclusive)
            }

            else -> null
        }
    }

    private fun createConstBoundedIntForLoopGenerator(
        codegen: ExpressionCodegen,
        forExpression: KtForExpression,
        startValue: StackValue,
        endIntValue: Int,
        step: Int,
        isStartInclusive: Boolean
    ): ForLoopGenerator? =
        ForInDefinitelySafeSimpleProgressionLoopGenerator(
            codegen, forExpression,
            startValue = startValue,
            isStartInclusive = isStartInclusive,
            endValue = StackValue.integerConstant(endIntValue, codegen.asmType(elementKotlinType)),
            isEndInclusive = true,
            comparisonGenerator = getComparisonGeneratorForKotlinType(elementKotlinType),
            step = step
        )

    private fun createConstBoundedLongForLoopGenerator(
        codegen: ExpressionCodegen,
        forExpression: KtForExpression,
        startValue: StackValue,
        endLongValue: Long,
        step: Int,
        isStartInclusive: Boolean
    ): ForLoopGenerator? =
        ForInDefinitelySafeSimpleProgressionLoopGenerator(
            codegen, forExpression,
            startValue = startValue,
            isStartInclusive = isStartInclusive,
            endValue = StackValue.constant(endLongValue, codegen.asmType(elementKotlinType), elementKotlinType),
            isEndInclusive = true,
            comparisonGenerator = getComparisonGeneratorForKotlinType(elementKotlinType),
            step = step
        )

    private fun isProhibitedCharConstEndValue(step: Int, endValue: Char) =
        endValue == if (step == 1) java.lang.Character.MAX_VALUE else java.lang.Character.MIN_VALUE

    private fun isProhibitedIntConstEndValue(step: Int, endValue: Int) =
        endValue == if (step == 1) Int.MAX_VALUE else Int.MIN_VALUE

    private fun isProhibitedLongConstEndValue(step: Int, endValue: Long) =
        endValue == if (step == 1) Long.MAX_VALUE else Long.MIN_VALUE

}