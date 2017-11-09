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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.org.objectweb.asm.Type

enum class ValueKind {
    GENERAL,
    GENERAL_VARARG,
    DEFAULT_PARAMETER,
    DEFAULT_MASK,
    METHOD_HANDLE_IN_DEFAULT,
    CAPTURED,
    DEFAULT_LAMBDA_CAPTURED_PARAMETER
}

interface CallGenerator {

    class DefaultCallGenerator(private val codegen: ExpressionCodegen) : CallGenerator {

        override fun genCallInner(
                callableMethod: Callable,
                resolvedCall: ResolvedCall<*>?,
                callDefault: Boolean,
                codegen: ExpressionCodegen) {
            if (!callDefault) {
                callableMethod.genInvokeInstruction(codegen.v)
            }
            else {
                (callableMethod as CallableMethod).genInvokeDefaultInstruction(codegen.v)
            }
        }

        override fun processAndPutHiddenParameters(justProcess: Boolean) {

        }

        override fun putHiddenParamsIntoLocals() {

        }

        override fun genValueAndPut(
                valueParameterDescriptor: ValueParameterDescriptor,
                argumentExpression: KtExpression,
                parameterType: Type,
                parameterIndex: Int) {
            val value = codegen.gen(argumentExpression)
            value.put(parameterType, codegen.v)
        }

        override fun putCapturedValueOnStack(
                stackValue: StackValue, valueType: Type, paramIndex: Int) {
            stackValue.put(stackValue.type, codegen.v)
        }

        override fun putValueIfNeeded(parameterType: Type, value: StackValue, kind: ValueKind, parameterIndex: Int) {
            value.put(value.type, codegen.v)
        }

        override fun reorderArgumentsIfNeeded(actualArgsWithDeclIndex: List<ArgumentAndDeclIndex>, valueParameterTypes: List<Type>) {
            val mark = codegen.myFrameMap.mark()
            val reordered = actualArgsWithDeclIndex.withIndex().dropWhile {
                it.value.declIndex == it.index
            }

            reordered.reversed().map {
                val argumentAndDeclIndex = it.value
                val type = valueParameterTypes.get(argumentAndDeclIndex.declIndex)
                val stackValue = StackValue.local(codegen.frameMap.enterTemp(type), type)
                stackValue.store(StackValue.onStack(type), codegen.v)
                Pair(argumentAndDeclIndex.declIndex, stackValue)
            }.sortedBy {
                it.first
            }.forEach {
                it.second.put(valueParameterTypes.get(it.first), codegen.v)
            }
            mark.dropTo()
        }
    }

    fun genCall(callableMethod: Callable, resolvedCall: ResolvedCall<*>?, callDefault: Boolean, codegen: ExpressionCodegen) {
        if (resolvedCall != null) {
            val calleeExpression = resolvedCall.call.calleeExpression
            if (calleeExpression != null) {
                codegen.markStartLineNumber(calleeExpression)
            }
        }

        genCallInner(callableMethod, resolvedCall, callDefault, codegen)
    }

    fun genCallInner(callableMethod: Callable, resolvedCall: ResolvedCall<*>?, callDefault: Boolean, codegen: ExpressionCodegen)

    fun genValueAndPut(
            valueParameterDescriptor: ValueParameterDescriptor,
            argumentExpression: KtExpression,
            parameterType: Type,
            parameterIndex: Int)

    fun putValueIfNeeded(
            parameterType: Type,
            value: StackValue) {
        putValueIfNeeded(parameterType, value, ValueKind.GENERAL)
    }

    fun putValueIfNeeded(
            parameterType: Type,
            value: StackValue,
            kind: ValueKind = ValueKind.GENERAL,
            parameterIndex: Int = -1)

    fun putCapturedValueOnStack(
            stackValue: StackValue,
            valueType: Type, paramIndex: Int)

    fun processAndPutHiddenParameters(justProcess: Boolean)

    /*should be called if justProcess = true in processAndPutHiddenParameters*/
    fun putHiddenParamsIntoLocals()

    fun reorderArgumentsIfNeeded(actualArgsWithDeclIndex: List<ArgumentAndDeclIndex>, valueParameterTypes: List<Type>)
}
