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

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.AsmUtil.comparisonOperandType
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import java.lang.UnsupportedOperationException

class CompareTo : IntrinsicMethod() {
    private fun genInvoke(type: Type?, v: InstructionAdapter) {
        when (type) {
            Type.CHAR_TYPE, Type.BYTE_TYPE, Type.SHORT_TYPE, Type.INT_TYPE ->
                v.invokestatic(
                    IntrinsicMethods.INTRINSICS_CLASS_NAME,
                    "compare",
                    "(II)I",
                    false
                )
            Type.LONG_TYPE -> v.invokestatic(IntrinsicMethods.INTRINSICS_CLASS_NAME, "compare", "(JJ)I", false)
            Type.FLOAT_TYPE -> v.invokestatic("java/lang/Float", "compare", "(FF)I", false)
            Type.DOUBLE_TYPE -> v.invokestatic("java/lang/Double", "compare", "(DD)I", false)
            else -> throw UnsupportedOperationException()
        }
    }

    override fun toCallable(
        expression: IrMemberAccessExpression,
        signature: JvmMethodSignature,
        context: JvmBackendContext
    ): IrIntrinsicFunction {
        val parameterType = comparisonOperandType(
            expressionType(expression.dispatchReceiver ?: expression.extensionReceiver!!, context),
            signature.valueParameters.single().asmType
        )
        return IrIntrinsicFunction.create(expression, signature, context, listOf(parameterType, parameterType)) {
            genInvoke(parameterType, it)
        }
    }
}


class PrimitiveComparison(
    private val primitiveNumberType: KotlinType,
    private val operatorToken: KtSingleValueToken
) : IntrinsicMethod(), ComparisonIntrinsic {

    override fun genStackValue(expression: IrMemberAccessExpression, context: JvmBackendContext): StackValue {
        val parameterType = context.state.typeMapper.mapType(primitiveNumberType)

        return StackValue.cmp(
            operatorToken,
            parameterType,
            StackValue.onStack(parameterType, primitiveNumberType),
            StackValue.onStack(parameterType, primitiveNumberType)
        )
    }

    override fun toCallable(
        expression: IrMemberAccessExpression,
        signature: JvmMethodSignature,
        context: JvmBackendContext
    ): IrIntrinsicFunction {
        val parameterType = context.state.typeMapper.mapType(primitiveNumberType)

        return object : IrIntrinsicFunction(expression, signature, context, listOf(parameterType, parameterType)) {
            override fun genInvokeInstruction(v: InstructionAdapter) {
                genStackValue(expression, context).put(Type.BOOLEAN_TYPE, v)
            }
        }
    }

}
