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

package org.jetbrains.kotlin.codegen.intrinsics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.ExtendedCallable
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

import org.jetbrains.org.objectweb.asm.Type.*

public class RangeTo : IntrinsicMethod() {
    override fun generateImpl(codegen: ExpressionCodegen, v: InstructionAdapter, returnType: Type, element: PsiElement?, arguments: List<JetExpression>, receiver: StackValue): Type {
        v.anew(returnType)
        v.dup()

        val type: Type
        if (arguments.size() == 1) {
            assert(receiver is StackValue.CallReceiver) { "Receiver in an intrinsic qualified expression should be CallReceiver: " + receiver + " on " + element!!.getText() }
            type = parameterType(receiver.type, codegen.expressionType(arguments.get(0)))
            receiver.put(type, v)
            codegen.gen(arguments.get(0), type)
        }
        else {
            val expression = element as JetBinaryExpression
            type = parameterType(codegen.expressionType(expression.getLeft()), codegen.expressionType(expression.getRight()))
            codegen.gen(expression.getLeft(), type)
            codegen.gen(expression.getRight(), type)
        }

        v.invokespecial(returnType.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, type, type), false)

        return returnType
    }

    private fun nameToPrimitive(name: String) : Type {
        return when (name) {
            "Double" -> DOUBLE_TYPE;
            "Float" -> FLOAT_TYPE
            "Long" -> LONG_TYPE
            "Int" -> INT_TYPE
            "Short" -> SHORT_TYPE
            "Char" -> CHAR_TYPE
            "Byte" -> BYTE_TYPE
            else -> throw IllegalStateException("RangeTo intrinsic can only work for primitive types: " + name)
        }
    }

    private fun parameterType(leftType: Type, rightType: Type): Type {
        val left = leftType.getSort()
        val right = rightType.getSort()
        if (left == DOUBLE || right == DOUBLE) {
            return DOUBLE_TYPE
        }
        else if (left == FLOAT || right == FLOAT) {
            return FLOAT_TYPE
        }
        else if (left == LONG || right == LONG) {
            return LONG_TYPE
        }
        else if (left == INT || right == INT) {
            return INT_TYPE
        }
        else if (left == SHORT || right == SHORT) {
            return SHORT_TYPE
        }
        else if (left == CHAR || right == CHAR) {
            return CHAR_TYPE
        }
        else if (left == BYTE || right == BYTE) {
            return BYTE_TYPE
        }
        else {
            throw IllegalStateException("RangeTo intrinsic can only work for primitive types: " + leftType + ", " + rightType)
        }
    }

    override fun supportCallable(): Boolean {
        return true
    }

    override fun toCallable(fd: FunctionDescriptor, isSuper: Boolean, resolvedCall: ResolvedCall<*>, codegen: ExpressionCodegen): ExtendedCallable {
        val method = codegen.getState().getTypeMapper().mapToCallableMethod(fd, false, codegen.getContext())
        val argType = nameToPrimitive(method.getReturnType().getInternalName().substringAfter("kotlin/").substringBefore("Range"))
        return object : IntrinsicCallable(method.getReturnType(), method.getValueParameterTypes().map { argType }, nullOr(method.getThisType(), argType), nullOr(method.getReceiverClass(), argType)) {
            override fun beforeParameterGeneration(v: InstructionAdapter, value: StackValue?) {
                v.anew(getReturnType())
                v.dup()
                value?.moveToTopOfStack(value!!.type, v, 2)
            }

            override fun invokeIntrinsic(v: InstructionAdapter) {
                v.invokespecial(getReturnType().getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, argType, argType), false)
            }
        }
    }
}