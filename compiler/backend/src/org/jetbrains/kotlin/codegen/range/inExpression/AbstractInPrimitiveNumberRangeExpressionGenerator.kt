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

package org.jetbrains.kotlin.codegen.range.inExpression

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.getClosedFloatingPointRangeElementType
import org.jetbrains.kotlin.codegen.getPrimitiveRangeElementType
import org.jetbrains.kotlin.codegen.range.comparison.*
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

abstract class AbstractInPrimitiveNumberRangeExpressionGenerator(
        codegen: ExpressionCodegen,
        operatorReference: KtSimpleNameExpression,
        rangeCall: ResolvedCall<*>,
        isInclusiveHighBound: Boolean
) : AbstractInRangeWithKnownBoundsExpressionGenerator(
        codegen, operatorReference, isInclusiveHighBound,
        getAsmRangeElementTypeForPrimitiveRange(rangeCall.resultingDescriptor)
) {
    override val comparisonGenerator: ComparisonGenerator =
            when (asmElementType) {
                Type.INT_TYPE, Type.SHORT_TYPE, Type.BYTE_TYPE, Type.CHAR_TYPE -> IntComparisonGenerator
                Type.LONG_TYPE -> LongComparisonGenerator
                Type.FLOAT_TYPE -> FloatComparisonGenerator
                Type.DOUBLE_TYPE -> DoubleComparisonGenerator
                else -> throw UnsupportedOperationException("Unexpected type: " + asmElementType)
            }
}


internal fun getAsmRangeElementTypeForPrimitiveRange(rangeCallee: CallableDescriptor): Type {
    val rangeType = rangeCallee.returnType!!

    getPrimitiveRangeElementType(rangeType)?.let {
        return AsmTypes.valueTypeForPrimitive(it)
    }

    val floatingPointElementType = getClosedFloatingPointRangeElementType(rangeType) ?:
                                   throw AssertionError("Unexpected range type: $rangeType")

    return when {
        KotlinBuiltIns.isDouble(floatingPointElementType) -> Type.DOUBLE_TYPE
        KotlinBuiltIns.isFloat(floatingPointElementType) -> Type.FLOAT_TYPE
        else -> throw AssertionError("Unexpected ClosedFloatingPointRange element type: $floatingPointElementType")
    }
}
