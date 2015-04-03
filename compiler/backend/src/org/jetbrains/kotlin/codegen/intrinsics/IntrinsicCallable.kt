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

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.context.CodegenContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

public abstract class IntrinsicCallable(val returnType1: Type,
                                        val valueParametersTypes: List<Type>,
                                        val thisType1: Type?,
                                        val receiverType1: Type?) : ExtendedCallable {

    companion object {
        fun create(descriptor: FunctionDescriptor, context: CodegenContext<*>, state: GenerationState, lambda: IntrinsicCallable.(i: InstructionAdapter) -> Unit): IntrinsicCallable {
            return create(state.getTypeMapper().mapToCallableMethod(descriptor, false, context), lambda)
        }

        fun create(callableMethod: CallableMethod, lambda: IntrinsicCallable.(i: InstructionAdapter) -> Unit): IntrinsicCallable {
            return object : IntrinsicCallable(callableMethod.getReturnType(), callableMethod.getValueParameterTypes(), callableMethod.getThisType(), callableMethod.getReceiverClass()) {
                override fun invokeIntrinsic(v: InstructionAdapter) {
                    lambda(v)
                }
            }
        }

        fun binaryIntrinsic(returnType: Type, valueParameterType: Type, thisType: Type? = null, receiverType: Type? = null, lambda: IntrinsicCallable.(i: InstructionAdapter) -> Unit): IntrinsicCallable {
            assert(AsmUtil.isPrimitive(returnType)) { "Return type of BinaryOp intrinsic should be of primitive type : " + returnType }

            return object : IntrinsicCallable(returnType, listOf(valueParameterType), thisType, receiverType) {
                override fun invokeIntrinsic(v: InstructionAdapter) {
                    lambda(v)
                }
            }
        }

        fun create(descriptor: FunctionDescriptor, context: CodegenContext<*>, state: GenerationState, receiverTransformer: Type.() -> Type, lambda: IntrinsicCallable.(i: InstructionAdapter) -> Unit): IntrinsicCallable {
            val callableMethod = state.getTypeMapper().mapToCallableMethod(descriptor, false, context)

            return object : IntrinsicCallable(callableMethod.getReturnType(), callableMethod.getValueParameterTypes(), callableMethod.getThisType()?.receiverTransformer(), callableMethod.getReceiverClass()?.receiverTransformer()) {
                override fun invokeIntrinsic(v: InstructionAdapter) {
                    lambda(v)
                }
            }
        }
    }

    override fun getValueParameterTypes(): List<Type> {
        return valueParametersTypes
    }


    override fun invokeWithoutAssertions(v: InstructionAdapter) {
        invokeIntrinsic(v)
    }

    override fun invokeWithNotNullAssertion(v: InstructionAdapter, state: GenerationState, resolvedCall: ResolvedCall<out CallableDescriptor>) {
        invokeWithoutAssertions(v)
    }

    public abstract fun invokeIntrinsic(v: InstructionAdapter);

    override fun getArgumentTypes(): Array<out Type>? {
        throw UnsupportedOperationException()
    }

    override fun isStaticCall(): Boolean {
        return false
    }

    override fun getGenerateCalleeType(): Type? {
        return null
    }

    override fun getReturnType(): Type {
        return returnType1
    }

    override fun getThisType(): Type? {
        return thisType1
    }

    override fun getReceiverClass(): Type? {
        return receiverType1
    }

    override fun getOwner(): Type {
        throw UnsupportedOperationException()
    }

    public fun calcReceiverType(): Type? {
        return getReceiverClass() ?: getThisType()
    }

    override fun beforeParameterGeneration(v: InstructionAdapter, value: StackValue?) {

    }

    override fun invokeMethodWithArguments(resolvedCall: ResolvedCall<*>, receiver: StackValue, returnType: Type, codegen: ExpressionCodegen): StackValue {
        return StackValue.functionCall(returnType) {
            codegen.invokeMethodWithArguments(this, resolvedCall, receiver, returnType)
        }
    }
}


public class UnaryIntrinsic(val callable: CallableMethod, val newReturnType: Type? = null, needPrimitiveCheck: Boolean = false, val newThisType: Type? = null, val invoke: UnaryIntrinsic.(v: InstructionAdapter) -> Unit) :
        IntrinsicCallable(newReturnType ?: callable.getReturnType(), callable.getValueParameterTypes(), newThisType ?: callable.getThisType(), callable.getReceiverClass()) {

    {
        if (needPrimitiveCheck) {
            assert(AsmUtil.isPrimitive(getReturnType())) { "Return type of UnaryPlus intrinsic should be of primitive type : " + getReturnType() }
        }
        assert(getValueParameterTypes().size == 0, "Unary operation should not have any parameters")
    }

    override fun invokeIntrinsic(v: InstructionAdapter) {
        invoke(v)
    }

}

public open class MappedCallable(val callable: CallableMethod, val invoke: MappedCallable.(v: InstructionAdapter) -> Unit = {}) :
        IntrinsicCallable(callable.getReturnType(), callable.getValueParameterTypes(), callable.getThisType(), callable.getReceiverClass()) {


    override fun invokeIntrinsic(v: InstructionAdapter) {
        invoke(v)
    }
}