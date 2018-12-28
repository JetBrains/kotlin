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

package org.jetbrains.kotlin.codegen.range.comparison

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.range.getRangeOrProgressionElementType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

interface ComparisonGenerator {
    val comparedType: Type

    fun jumpIfGreaterOrEqual(v: InstructionAdapter, label: Label)
    fun jumpIfLessOrEqual(v: InstructionAdapter, label: Label)
    fun jumpIfGreater(v: InstructionAdapter, label: Label)
    fun jumpIfLess(v: InstructionAdapter, label: Label)
}

interface SignedIntegerComparisonGenerator : ComparisonGenerator {
    fun jumpIfLessThanZero(v: InstructionAdapter, label: Label)
}

fun getComparisonGeneratorForKotlinType(kotlinType: KotlinType): ComparisonGenerator =
    when {
        KotlinBuiltIns.isChar(kotlinType) ->
            CharComparisonGenerator
        KotlinBuiltIns.isByte(kotlinType) || KotlinBuiltIns.isShort(kotlinType) || KotlinBuiltIns.isInt(kotlinType) ->
            IntComparisonGenerator
        KotlinBuiltIns.isLong(kotlinType) ->
            LongComparisonGenerator
        KotlinBuiltIns.isFloat(kotlinType) ->
            FloatComparisonGenerator
        KotlinBuiltIns.isDouble(kotlinType) ->
            DoubleComparisonGenerator
        KotlinBuiltIns.isUInt(kotlinType) ->
            UIntComparisonGenerator
        KotlinBuiltIns.isULong(kotlinType) ->
            ULongComparisonGenerator
        else ->
            throw UnsupportedOperationException("Unexpected element type: $kotlinType")
    }

fun getComparisonGeneratorForRangeContainsCall(
    codegen: ExpressionCodegen,
    call: ResolvedCall<out CallableDescriptor>
): ComparisonGenerator? {
    val descriptor = call.resultingDescriptor

    val receiverType = descriptor.extensionReceiverParameter?.type ?: descriptor.dispatchReceiverParameter?.type ?: return null

    val elementType = getRangeOrProgressionElementType(receiverType) ?: return null

    val valueParameterType = descriptor.valueParameters.singleOrNull()?.type ?: return null

    val asmElementType = codegen.asmType(elementType)
    val asmValueParameterType = codegen.asmType(valueParameterType)

    return when {
        asmElementType == asmValueParameterType ->
            getComparisonGeneratorForKotlinType(elementType)

        asmElementType.isPrimitiveIntOrCoercible() && asmValueParameterType.isPrimitiveIntOrCoercible() ->
            IntComparisonGenerator

        asmElementType.isPrimitiveIntOrCoercible() && asmValueParameterType == Type.LONG_TYPE ||
                asmValueParameterType.isPrimitiveIntOrCoercible() && asmElementType == Type.LONG_TYPE ->
            LongComparisonGenerator

        asmElementType == Type.FLOAT_TYPE && asmValueParameterType == Type.DOUBLE_TYPE ||
                asmElementType == Type.DOUBLE_TYPE && asmValueParameterType == Type.FLOAT_TYPE ->
            DoubleComparisonGenerator

        else -> null
    }
}

private fun Type.isPrimitiveIntOrCoercible() =
    this == Type.INT_TYPE || this == Type.SHORT_TYPE || this == Type.BYTE_TYPE